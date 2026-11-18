package com.gestion.alquileres.seguridad;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class SeguridadService {

    private static final Logger log = LoggerFactory.getLogger(SeguridadService.class);
    private static final String CLAVE_APP_PASSWORD = "APP_PASSWORD";

    /**
     * TTL corto (10s) para la lectura del hash en BD y TTL de 60s para el par validado
     * (pinRecibido_sha256, hashEnBD) para responder en <1ms en ráfagas de peticiones paralelas.
     */
    private static final Duration TTL_CACHE_BD = Duration.ofSeconds(10);
    private static final Duration TTL_CACHE_VALIDACION = Duration.ofSeconds(60);

    private record EntradaValidacionRapida(String pinSha256, String hashEnBd, Instant expiraEn) {
        boolean esValida(String sha256, String valorBdActual, Instant ahora) {
            return ahora.isBefore(expiraEn)
                    && this.pinSha256.equals(sha256)
                    && this.hashEnBd.equals(valorBdActual);
        }
    }

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Object lockValidacion = new Object();

    @Value("${app.security.pin:1234}")
    private String fallbackPin;

    private volatile String cachedDbValor;
    private volatile Instant cachedDbExpiraEn = Instant.MIN;
    private volatile EntradaValidacionRapida cacheValidacionRapida;

    public SeguridadService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void inicializarSeguridad() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS configuracion_seguridad (
                        clave          VARCHAR(50)  PRIMARY KEY,
                        valor          VARCHAR(255) NOT NULL,
                        actualizado_en TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.update("""
                    INSERT INTO configuracion_seguridad (clave, valor)
                    VALUES (?, ?)
                    ON CONFLICT (clave) DO NOTHING
                    """, CLAVE_APP_PASSWORD, fallbackPin);

            refrescarValorDesdeBd();
        } catch (Exception ex) {
            log.warn("No se pudo inicializar tabla configuracion_seguridad al arranque: {}", ex.getMessage());
        }
    }

    /**
     * Valida la contraseña/PIN enviada en X-App-Pin:
     * 1. Fast-path (<1ms): verifica en memoria el par (pinRecibido_sha256, hashEnBD) con TTL 60s
     *    para evitar penalización de CPU de BCrypt en las 6 peticiones paralelas de Angular.
     * 2. Invalidación automática si cambia en SQL: si falla contra la caché en memoria,
     *    relee la fila 'APP_PASSWORD' directamente de PostgreSQL antes de darla por inválida.
     * 3. Auto-BCrypt híbrido: si en PostgreSQL hay texto plano, lo verifica y lo cifra a BCrypt.
     */
    @Transactional
    public boolean validarPin(String rawPin) {
        if (rawPin == null || rawPin.isBlank()) {
            return false;
        }
        String pinLimpio = rawPin.trim();
        String pinSha256 = calcularSha256Hex(pinLimpio);
        Instant ahora = Instant.now();

        String valorBdActual = obtenerValorBdConTtl(ahora);
        EntradaValidacionRapida rapida = this.cacheValidacionRapida;
        if (rapida != null && valorBdActual != null && rapida.esValida(pinSha256, valorBdActual, ahora)) {
            return true;
        }

        synchronized (lockValidacion) {
            ahora = Instant.now();
            valorBdActual = obtenerValorBdConTtl(ahora);
            rapida = this.cacheValidacionRapida;
            if (rapida != null && valorBdActual != null && rapida.esValida(pinSha256, valorBdActual, ahora)) {
                return true;
            }

            if (evaluarContraValorBd(pinLimpio, pinSha256, valorBdActual)) {
                return true;
            }

            // Si falla contra el valor en caché, releer inmediatamente de PostgreSQL
            // por si el administrador actualizó 'APP_PASSWORD' directamente en SQL en caliente.
            String valorBdFresco = refrescarValorDesdeBd();
            if (valorBdFresco != null && !valorBdFresco.equals(valorBdActual)) {
                log.info("Detectado cambio de APP_PASSWORD en PostgreSQL: recargando caché de seguridad.");
                if (evaluarContraValorBd(pinLimpio, pinSha256, valorBdFresco)) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean evaluarContraValorBd(String pinLimpio, String pinSha256, String valorBd) {
        if (valorBd == null || valorBd.isBlank()) {
            valorBd = fallbackPin;
        }

        if (!esHashBcrypt(valorBd)) {
            if (valorBd.equals(pinLimpio)) {
                String nuevoHash = passwordEncoder.encode(pinLimpio);
                guardarHashEnBd(nuevoHash);
                actualizarCachesEnMemoria(pinSha256, nuevoHash);
                log.info("Clave en texto plano detectada en configuracion_seguridad: auto-hasheada a BCrypt correctamente.");
                return true;
            }
            return false;
        }

        if (passwordEncoder.matches(pinLimpio, valorBd)) {
            actualizarCachesEnMemoria(pinSha256, valorBd);
            return true;
        }
        return false;
    }

    @Transactional
    public void cambiarPassword(String passwordActual, String passwordNueva) {
        if (passwordActual == null || !validarPin(passwordActual)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseña actual no es correcta");
        }
        if (passwordNueva == null || passwordNueva.trim().length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe tener al menos 4 caracteres");
        }
        String pinNuevoLimpio = passwordNueva.trim();
        synchronized (lockValidacion) {
            String nuevoHash = passwordEncoder.encode(pinNuevoLimpio);
            guardarHashEnBd(nuevoHash);
            actualizarCachesEnMemoria(calcularSha256Hex(pinNuevoLimpio), nuevoHash);
        }
        log.info("Contraseña maestra actualizada correctamente en configuracion_seguridad (BCrypt).");
    }

    private String obtenerValorBdConTtl(Instant ahora) {
        String valor = this.cachedDbValor;
        if (valor != null && ahora.isBefore(this.cachedDbExpiraEn)) {
            return valor;
        }
        return refrescarValorDesdeBd();
    }

    private String refrescarValorDesdeBd() {
        String valor = leerValorEnBd();
        if (valor == null || valor.isBlank()) {
            valor = fallbackPin;
        }
        this.cachedDbValor = valor;
        this.cachedDbExpiraEn = Instant.now().plus(TTL_CACHE_BD);
        return valor;
    }

    private void actualizarCachesEnMemoria(String pinSha256, String hashEnBd) {
        Instant ahora = Instant.now();
        this.cachedDbValor = hashEnBd;
        this.cachedDbExpiraEn = ahora.plus(TTL_CACHE_BD);
        this.cacheValidacionRapida = new EntradaValidacionRapida(
                pinSha256,
                hashEnBd,
                ahora.plus(TTL_CACHE_VALIDACION)
        );
    }

    private static String calcularSha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private boolean esHashBcrypt(String valor) {
        return valor != null && (valor.startsWith("$2a$") || valor.startsWith("$2b$") || valor.startsWith("$2y$"));
    }

    private String leerValorEnBd() {
        List<String> filas = jdbcTemplate.query(
                "SELECT valor FROM configuracion_seguridad WHERE clave = ?",
                (rs, rowNum) -> rs.getString("valor"),
                CLAVE_APP_PASSWORD
        );
        return filas.isEmpty() ? null : filas.get(0);
    }

    private void guardarHashEnBd(String hashBcrypt) {
        int actualizadas = jdbcTemplate.update("""
                UPDATE configuracion_seguridad
                SET valor = ?, actualizado_en = CURRENT_TIMESTAMP
                WHERE clave = ?
                """, hashBcrypt, CLAVE_APP_PASSWORD);
        if (actualizadas == 0) {
            jdbcTemplate.update("""
                    INSERT INTO configuracion_seguridad (clave, valor, actualizado_en)
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                    """, CLAVE_APP_PASSWORD, hashBcrypt);
        }
    }
}
