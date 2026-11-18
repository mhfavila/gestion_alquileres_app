package com.gestion.alquileres.seguridad;

import com.gestion.alquileres.comun.Contexto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ContextFilter.class);

    static final String PREFIJO = "/api";
    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private static final class EstadoIntentosIp {
        int fallos = 0;
        Instant bloqueadoHasta = null;
    }

    private final ConcurrentHashMap<String, EstadoIntentosIp> intentosPorIp = new ConcurrentHashMap<>();
    private final SeguridadService seguridadService;

    public ContextFilter(SeguridadService seguridadService) {
        this.seguridadService = seguridadService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = pathSinContextPath(request);
        return !(path.equals(PREFIJO) || path.startsWith(PREFIJO + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = obtenerIpCliente(request);
        if (estaIpBloqueada(clientIp)) {
            log.warn("Petición bloqueada por fuerza bruta (429): IP {} en {} {}", clientIp, request.getMethod(), request.getRequestURI());
            rechazarDemasiadosIntentos(response);
            return;
        }

        String pinHeader = request.getHeader(ContextoHolder.PIN_HEADER);
        if (!seguridadService.validarPin(pinHeader)) {
            boolean bloqueadaAhora = registrarFalloIp(clientIp);
            if (bloqueadaAhora) {
                log.warn("IP {} ha alcanzado {} intentos fallidos seguidos: bloqueada 15 minutos", clientIp, MAX_INTENTOS_FALLIDOS);
                rechazarDemasiadosIntentos(response);
            } else {
                log.warn("Petición rechazada (401): PIN inválido o ausente desde IP {} en {} {}", clientIp, request.getMethod(), request.getRequestURI());
                rechazarNoAutorizado(response);
            }
            return;
        }

        resetearIntentosIp(clientIp);

        String path = pathSinContextPath(request);
        if (path.startsWith("/api/seguridad")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(ContextoHolder.HEADER);
        log.info("HTTP Request: {} {} | X-Context-Id: {}", request.getMethod(), request.getRequestURI(), header);
        if (header == null || header.isBlank()) {
            log.warn("Petición rechazada: falta el header {} en {} {}", ContextoHolder.HEADER, request.getMethod(), request.getRequestURI());
            rechazar(response, "El header X-Context-Id es obligatorio");
            return;
        }

        final Contexto contexto;
        try {
            contexto = Contexto.valueOf(header.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Petición rechazada: X-Context-Id inválido '{}' en {} {}", header, request.getMethod(), request.getRequestURI());
            rechazar(response, "X-Context-Id no es un contexto válido");
            return;
        }

        log.info("Contexto resuelto: {} para {} {}", contexto, request.getMethod(), request.getRequestURI());
        request.setAttribute(ContextoHolder.ATRIBUTO, contexto);
        ContextoHolder.set(contexto);
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("Excepción no controlada procesando {} {} con contexto {}", request.getMethod(), request.getRequestURI(), contexto, ex);
            throw ex;
        } finally {
            ContextoHolder.clear();
        }
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String primeraIp = forwarded.split(",")[0].trim();
            if (!primeraIp.isEmpty()) {
                return primeraIp;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private boolean estaIpBloqueada(String ip) {
        EstadoIntentosIp estado = intentosPorIp.get(ip);
        if (estado == null) {
            return false;
        }
        synchronized (estado) {
            if (estado.bloqueadoHasta == null) {
                return false;
            }
            if (Instant.now().isBefore(estado.bloqueadoHasta)) {
                return true;
            }
            estado.fallos = 0;
            estado.bloqueadoHasta = null;
            return false;
        }
    }

    private boolean registrarFalloIp(String ip) {
        EstadoIntentosIp estado = intentosPorIp.computeIfAbsent(ip, k -> new EstadoIntentosIp());
        synchronized (estado) {
            estado.fallos++;
            if (estado.fallos >= MAX_INTENTOS_FALLIDOS) {
                estado.bloqueadoHasta = Instant.now().plus(DURACION_BLOQUEO);
                return true;
            }
            return false;
        }
    }

    private void resetearIntentosIp(String ip) {
        intentosPorIp.remove(ip);
    }

    private static String pathSinContextPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private static void rechazar(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"error\":\"" + mensaje + "\",\"permitidos\":[\"REAL_ESTATE\",\"HOME\",\"PERSONAL\"]}");
    }

    private static void rechazarNoAutorizado(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"PIN de acceso inválido o ausente\"}");
    }

    private static void rechazarDemasiadosIntentos(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Demasiados intentos fallidos. IP bloqueada temporalmente durante 15 minutos.\"}");
    }
}
