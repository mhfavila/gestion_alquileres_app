package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.comun.Contexto;
import com.gestion.alquileres.comun.TipoTransaccion;
import com.gestion.alquileres.inmobiliaria.Contrato;
import com.gestion.alquileres.inmobiliaria.ContratoRepository;
import com.gestion.alquileres.inmobiliaria.Inmueble;
import com.gestion.alquileres.inmobiliaria.InmuebleRepository;
import com.gestion.alquileres.seguridad.ContextoHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class TransaccionService {

    private static final Logger log = LoggerFactory.getLogger(TransaccionService.class);

    private final TransaccionRepository repository;
    private final WalletRepository walletRepository;
    private final CategoriaRepository categoriaRepository;
    private final InmuebleRepository inmuebleRepository;
    private final ContratoRepository contratoRepository;

    public TransaccionService(TransaccionRepository repository, WalletRepository walletRepository,
                               CategoriaRepository categoriaRepository, InmuebleRepository inmuebleRepository,
                               ContratoRepository contratoRepository) {
        this.repository = repository;
        this.walletRepository = walletRepository;
        this.categoriaRepository = categoriaRepository;
        this.inmuebleRepository = inmuebleRepository;
        this.contratoRepository = contratoRepository;
    }

    public List<Transaccion> obtenerPorContexto() {
        return obtenerPorContexto(null, null);
    }

    public List<Transaccion> obtenerPorContexto(String mes, Integer anio) {
        Contexto contexto = ContextoHolder.get();
        if (mes != null && !mes.isBlank()) {
            try {
                YearMonth ym = YearMonth.parse(mes.trim());
                LocalDate inicio = ym.atDay(1);
                LocalDate fin = ym.atEndOfMonth();
                log.info("Listando transacciones del contexto {} filtradas por mes {} ({} a {})", contexto, ym, inicio, fin);
                return repository.findByContextoAndFechaBetweenOrderByFechaDesc(contexto, inicio, fin);
            } catch (DateTimeParseException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Formato de mes inválido (esperado YYYY-MM): " + mes);
            }
        }
        if (anio != null) {
            LocalDate inicio = LocalDate.of(anio, 1, 1);
            LocalDate fin = LocalDate.of(anio, 12, 31);
            log.info("Listando transacciones del contexto {} filtradas por año {} ({} a {})", contexto, anio, inicio, fin);
            return repository.findByContextoAndFechaBetweenOrderByFechaDesc(contexto, inicio, fin);
        }
        log.info("Listando todo el histórico de transacciones del contexto {}", contexto);
        return repository.findByContextoOrderByFechaDesc(contexto);
    }

    public Transaccion guardar(Transaccion transaccion) {
        Contexto contextoActual = ContextoHolder.get();
        log.info("Guardando transacción entrante: importe={}, tipo={}, fecha={}, walletId={}, categoriaId={}, inmuebleId={}, contratoId={}, contexto={}",
                transaccion.getImporte(), transaccion.getTipo(), transaccion.getFecha(),
                transaccion.getWallet() != null ? transaccion.getWallet().getId() : null,
                transaccion.getCategoria() != null ? transaccion.getCategoria().getId() : null,
                transaccion.getInmueble() != null ? transaccion.getInmueble().getId() : null,
                transaccion.getContrato() != null ? transaccion.getContrato().getId() : null,
                contextoActual);

        resolverRelaciones(transaccion, contextoActual);
        transaccion.setContexto(contextoActual);

        try {
            Transaccion guardada = repository.save(transaccion);
            log.info("Transacción guardada correctamente con ID: {} (wallet={}, categoria={})",
                    guardada.getId(), guardada.getWallet().getId(), guardada.getCategoria().getId());
            return guardada;
        } catch (RuntimeException ex) {
            log.error("Error al guardar transacción: importe={}, tipo={}, walletId={}",
                    transaccion.getImporte(), transaccion.getTipo(),
                    transaccion.getWallet() != null ? transaccion.getWallet().getId() : null, ex);
            throw ex;
        }
    }

    @Transactional
    public Transaccion actualizar(Long id, Transaccion datos) {
        Contexto contextoActual = ContextoHolder.get();
        log.info("Actualizando transacción ID {}: importe={}, tipo={}, fecha={}, walletId={}, categoriaId={}, inmuebleId={}, contratoId={}",
                id, datos.getImporte(), datos.getTipo(), datos.getFecha(),
                datos.getWallet() != null ? datos.getWallet().getId() : null,
                datos.getCategoria() != null ? datos.getCategoria().getId() : null,
                datos.getInmueble() != null ? datos.getInmueble().getId() : null,
                datos.getContrato() != null ? datos.getContrato().getId() : null);

        Transaccion existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaccion no encontrada con ID: " + id));

        if (!existente.getContexto().equals(contextoActual)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Transaccion pertenece a otro contexto");
        }

        if (existente.getTraspasoParId() != null) {
            return actualizarTraspasoEnEspejo(existente, datos);
        }

        resolverRelaciones(datos, contextoActual);

        existente.setImporte(datos.getImporte());
        existente.setTipo(datos.getTipo());
        existente.setFecha(datos.getFecha());
        existente.setDescripcion(datos.getDescripcion());
        existente.setWallet(datos.getWallet());
        existente.setCategoria(datos.getCategoria());
        existente.setInmueble(datos.getInmueble());
        existente.setContrato(datos.getContrato());
        existente.setContexto(contextoActual);

        try {
            Transaccion actualizada = repository.save(existente);
            log.info("Transacción actualizada correctamente con ID: {}", actualizada.getId());
            return actualizada;
        } catch (RuntimeException ex) {
            log.error("Error al actualizar transacción ID {}", id, ex);
            throw ex;
        }
    }

    /**
     * Edición en espejo de un traspaso interno: solo se sincronizan importe, fecha
     * y descripción entre la transacción y su gemela del otro contexto.
     * El tipo, la categoría y las relaciones (inmueble/contrato) quedan bloqueados
     * para que el traspaso nunca quede descuadrado contablemente.
     */
    private Transaccion actualizarTraspasoEnEspejo(Transaccion existente, Transaccion datos) {
        if (datos.getImporte() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Importe es requerido");
        }
        if (datos.getFecha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha es requerida");
        }

        existente.setImporte(datos.getImporte());
        existente.setFecha(datos.getFecha());
        existente.setDescripcion(datos.getDescripcion());

        Transaccion gemela = repository.findById(existente.getTraspasoParId()).orElse(null);
        if (gemela != null) {
            gemela.setImporte(datos.getImporte());
            gemela.setFecha(datos.getFecha());
            if (datos.getDescripcion() != null && !datos.getDescripcion().isBlank()) {
                String prefijo = "";
                String descGemela = gemela.getDescripcion();
                if (descGemela != null && descGemela.contains(":")) {
                    prefijo = descGemela.substring(0, descGemela.indexOf(':') + 1) + " ";
                }
                gemela.setDescripcion(prefijo + datos.getDescripcion());
            }
            repository.save(gemela);
            log.info("Traspaso espejo actualizado: ID {} -> gemela ID {}", existente.getId(), gemela.getId());
        } else {
            log.warn("Traspaso ID {} sin transacción gemela (par ID {}), se actualiza solo", existente.getId(), existente.getTraspasoParId());
        }

        try {
            Transaccion actualizada = repository.save(existente);
            log.info("Traspaso actualizado correctamente con ID: {} (espejo)", actualizada.getId());
            return actualizada;
        } catch (RuntimeException ex) {
            log.error("Error al actualizar traspaso ID {}", existente.getId(), ex);
            throw ex;
        }
    }

    private void resolverRelaciones(Transaccion transaccion, Contexto contextoActual) {
        if (transaccion.getWallet() == null || transaccion.getWallet().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet es requerido");
        }
        if (transaccion.getCategoria() == null || transaccion.getCategoria().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria es requerida");
        }

        Wallet wallet = walletRepository.findById(transaccion.getWallet().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Wallet no encontrado con ID: " + transaccion.getWallet().getId()));

        Categoria categoria = categoriaRepository.findById(transaccion.getCategoria().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Categoria no encontrada con ID: " + transaccion.getCategoria().getId()));

        if (!wallet.getContexto().equals(contextoActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Wallet pertenece a otro contexto: " + wallet.getContexto() + ", contexto actual: " + contextoActual);
        }

        if (!categoria.getContexto().equals(contextoActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Categoria pertenece a otro contexto: " + categoria.getContexto() + ", contexto actual: " + contextoActual);
        }

        transaccion.setWallet(wallet);
        transaccion.setCategoria(categoria);

        if (transaccion.getInmueble() != null && transaccion.getInmueble().getId() != null) {
            Inmueble inmueble = inmuebleRepository.findById(transaccion.getInmueble().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Inmueble no encontrado con ID: " + transaccion.getInmueble().getId()));

            if (!inmueble.getContexto().equals(contextoActual)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Inmueble pertenece a otro contexto: " + inmueble.getContexto() + ", contexto actual: " + contextoActual);
            }

            transaccion.setInmueble(inmueble);
        } else {
            transaccion.setInmueble(null);
        }

        if (transaccion.getContrato() != null && transaccion.getContrato().getId() != null) {
            Contrato contrato = contratoRepository.findById(transaccion.getContrato().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Contrato no encontrado con ID: " + transaccion.getContrato().getId()));

            if (!contrato.getContexto().equals(contextoActual)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Contrato pertenece a otro contexto: " + contrato.getContexto() + ", contexto actual: " + contextoActual);
            }

            transaccion.setContrato(contrato);
        } else {
            transaccion.setContrato(null);
        }
    }

    @Transactional
    public void realizarTraspaso(TraspasoRequest req) {
        Contexto contextoOrigen = ContextoHolder.get();
        log.info("Iniciando traspaso: walletOrigenId={}, destino={}, importe={}, fecha={} (origen={})",
                req.walletOrigenId(), req.contextoDestino(), req.importe(), req.fecha(), contextoOrigen);

        if (req.contextoDestino() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contextoDestino es obligatorio");
        }
        if (req.contextoDestino().equals(contextoOrigen)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El contexto destino debe ser diferente al origen");
        }

        Wallet walletOrigen = walletRepository.findById(req.walletOrigenId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Wallet origen no encontrada con ID: " + req.walletOrigenId()));

        if (!walletOrigen.getContexto().equals(contextoOrigen)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Wallet origen pertenece a otro contexto: " + walletOrigen.getContexto());
        }

        List<Wallet> walletsDestino = walletRepository.findByContextoOrderByNombreAsc(req.contextoDestino());
        if (walletsDestino.isEmpty()) {
            log.warn("Traspaso abortado: no hay wallets en el contexto destino {}", req.contextoDestino());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No existe ninguna wallet en el contexto destino: " + req.contextoDestino());
        }
        Wallet walletDestino = walletsDestino.get(0);
        log.info("Traspaso: wallet origen ID {} -> wallet destino ID {} ('{}')", walletOrigen.getId(), walletDestino.getId(), walletDestino.getNombre());

        Categoria catOrigen = categoriaRepository.findByContextoAndNombreIgnoreCase(contextoOrigen, "Traspaso Interno");
        if (catOrigen == null) {
            catOrigen = new Categoria("Traspaso Interno", contextoOrigen);
            catOrigen = categoriaRepository.save(catOrigen);
        }

        Categoria catDestino = categoriaRepository.findByContextoAndNombreIgnoreCase(req.contextoDestino(), "Traspaso Interno");
        if (catDestino == null) {
            catDestino = new Categoria("Traspaso Interno", req.contextoDestino());
            catDestino = categoriaRepository.save(catDestino);
        }

        String descSalida = "Traspaso a " + req.contextoDestino();
        if (req.descripcion() != null && !req.descripcion().isBlank()) {
            descSalida += ": " + req.descripcion();
        }

        String descEntrada = "Traspaso desde " + contextoOrigen;
        if (req.descripcion() != null && !req.descripcion().isBlank()) {
            descEntrada += ": " + req.descripcion();
        }

        Transaccion txSalida = new Transaccion(req.importe(), TipoTransaccion.GASTO, req.fecha(), descSalida, walletOrigen, catOrigen);
        txSalida.setContexto(contextoOrigen);
        txSalida = repository.save(txSalida);

        Transaccion txEntrada = new Transaccion(req.importe(), TipoTransaccion.INGRESO, req.fecha(), descEntrada, walletDestino, catDestino);
        txEntrada.setContexto(req.contextoDestino());
        txEntrada = repository.save(txEntrada);

        txSalida.setTraspasoParId(txEntrada.getId());
        txEntrada.setTraspasoParId(txSalida.getId());

        repository.save(txSalida);
        repository.save(txEntrada);
        log.info("Traspaso completado: transacción salida ID {} <-> entrada ID {}", txSalida.getId(), txEntrada.getId());
    }

    @Transactional
    public void eliminar(Long id) {
        Contexto contextoActual = ContextoHolder.get();
        log.info("Eliminando transacción con ID: {} (contexto: {})", id, contextoActual);

        Transaccion tx = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Transaccion no encontrada con ID: " + id));

        if (!tx.getContexto().equals(contextoActual)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Transaccion pertenece a otro contexto");
        }

        if (tx.getTraspasoParId() != null) {
            Transaccion txPar = repository.findById(tx.getTraspasoParId()).orElse(null);
            if (txPar != null) {
                repository.delete(txPar);
            }
        }

        repository.delete(tx);
        repository.flush();
        log.info("Transacción eliminada correctamente: ID {}", id);
    }
}
