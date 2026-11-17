package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.stereotype.Service;

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
}
