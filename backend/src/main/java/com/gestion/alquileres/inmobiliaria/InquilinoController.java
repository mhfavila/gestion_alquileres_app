package com.gestion.alquileres.inmobiliaria;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inmobiliaria/inquilinos")
public class InquilinoController {

    private final InquilinoService service;

    public InquilinoController(InquilinoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Inquilino> listar() {
        return service.obtenerPorContexto();
    }

    @PostMapping
    public Inquilino crear(@RequestBody Inquilino inquilino) {
        return service.guardar(inquilino);
    }
}
