package com.gestion.alquileres.inmobiliaria;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inmobiliaria/inmuebles")
public class InmuebleController {

    private final InmuebleService service;

    public InmuebleController(InmuebleService service) {
        this.service = service;
    }

    @GetMapping
    public List<Inmueble> listar() {
        return service.obtenerPorContexto();
    }

    @PostMapping
    public Inmueble crear(@RequestBody Inmueble inmueble) {
        return service.guardar(inmueble);
    }
}
