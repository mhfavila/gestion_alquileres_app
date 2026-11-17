package com.gestion.alquileres.inmobiliaria;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inmobiliaria/contratos")
public class ContratoController {

    private final ContratoService service;

    public ContratoController(ContratoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Contrato> listar() {
        return service.obtenerPorContexto();
    }

    @PostMapping
    public Contrato crear(@RequestBody Contrato contrato) {
        return service.guardar(contrato);
    }
}
