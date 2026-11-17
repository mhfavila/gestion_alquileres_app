package com.gestion.alquileres.finanzas;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finanzas/wallets")
public class WalletController {

    private final WalletService service;

    public WalletController(WalletService service) {
        this.service = service;
    }

    @GetMapping
    public List<Wallet> listar() {
        return service.obtenerPorContexto();
    }

    @PostMapping
    public Wallet crear(@RequestBody Wallet wallet) {
        return service.guardar(wallet);
    }
}
