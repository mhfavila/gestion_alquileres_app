package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository repository;

    public WalletService(WalletRepository repository) {
        this.repository = repository;
    }

    public List<Wallet> obtenerPorContexto() {
        log.info("Listando wallets del contexto {}", ContextoHolder.get());
        return repository.findByContextoOrderByNombreAsc(ContextoHolder.get());
    }

    public Wallet guardar(Wallet wallet) {
        wallet.setContexto(ContextoHolder.get());
        log.info("Guardando wallet entrante: nombre='{}', contexto={}", wallet.getNombre(), wallet.getContexto());
        try {
            Wallet guardada = repository.save(wallet);
            log.info("Wallet guardada correctamente con ID: {}", guardada.getId());
            return guardada;
        } catch (RuntimeException ex) {
            log.error("Error al guardar wallet: nombre='{}'", wallet.getNombre(), ex);
            throw ex;
        }
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando wallet con ID: {} (contexto: {})", id, ContextoHolder.get());
        Wallet wallet = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Wallet no encontrada con ID: " + id));

        if (!wallet.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Wallet pertenece a otro contexto");
        }

        try {
            repository.delete(wallet);
            repository.flush();
            log.info("Wallet eliminada correctamente: ID {} ('{}')", id, wallet.getNombre());
        } catch (DataIntegrityViolationException e) {
            log.warn("No se puede eliminar la wallet ID {}: tiene transacciones asociadas", id, e);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar la wallet porque tiene transacciones asociadas");
        } catch (RuntimeException ex) {
            log.error("Error inesperado eliminando wallet ID {}", id, ex);
            throw ex;
        }
    }
}
