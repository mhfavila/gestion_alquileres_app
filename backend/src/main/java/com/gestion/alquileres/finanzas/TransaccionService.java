package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.comun.Contexto;
import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TransaccionService {

    private final TransaccionRepository repository;
    private final WalletRepository walletRepository;
    private final CategoriaRepository categoriaRepository;

    public TransaccionService(TransaccionRepository repository, WalletRepository walletRepository,
                               CategoriaRepository categoriaRepository) {
        this.repository = repository;
        this.walletRepository = walletRepository;
        this.categoriaRepository = categoriaRepository;
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
        transaccion.setContexto(contextoActual);

        return repository.save(transaccion);
    }
}
