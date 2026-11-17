package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.comun.Contexto;
import com.gestion.alquileres.comun.TipoTransaccion;
import com.gestion.alquileres.inmobiliaria.Contrato;
import com.gestion.alquileres.inmobiliaria.ContratoRepository;
import com.gestion.alquileres.inmobiliaria.Inmueble;
import com.gestion.alquileres.inmobiliaria.InmuebleRepository;
import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TransaccionService {

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
        return repository.findByContextoOrderByFechaDesc(ContextoHolder.get());
    }

    public Transaccion guardar(Transaccion transaccion) {
        Contexto contextoActual = ContextoHolder.get();

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

        transaccion.setContexto(contextoActual);

        return repository.save(transaccion);
    }

    @Transactional
    public void realizarTraspaso(TraspasoRequest req) {
        Contexto contextoOrigen = ContextoHolder.get();

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No existe ninguna wallet en el contexto destino: " + req.contextoDestino());
        }
        Wallet walletDestino = walletsDestino.get(0);

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
    }

    @Transactional
    public void eliminar(Long id) {
        Contexto contextoActual = ContextoHolder.get();

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
    }
}
