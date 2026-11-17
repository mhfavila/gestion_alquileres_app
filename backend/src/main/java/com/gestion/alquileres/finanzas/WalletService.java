package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WalletService {

    private final WalletRepository repository;

    public WalletService(WalletRepository repository) {
        this.repository = repository;
    }

    public List<Wallet> obtenerPorContexto() {
        return repository.findByContextoOrderByNombreAsc(ContextoHolder.get());
    }

    public Wallet guardar(Wallet wallet) {
        wallet.setContexto(ContextoHolder.get());
        return repository.save(wallet);
    }

    @Transactional
    public void eliminar(Long id) {
        Wallet wallet = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Wallet no encontrada con ID: " + id));

        if (!wallet.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Wallet pertenece a otro contexto");
        }

        try {
            repository.delete(wallet);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar la wallet porque tiene transacciones asociadas");
        }
    }
}
