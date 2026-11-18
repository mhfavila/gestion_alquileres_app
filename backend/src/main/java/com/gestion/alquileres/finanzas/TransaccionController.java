package com.gestion.alquileres.finanzas;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finanzas/transacciones")
public class TransaccionController {

    private final TransaccionService service;

    public TransaccionController(TransaccionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Transaccion> listar(
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) Integer anio) {
        return service.obtenerPorContexto(mes, anio);
    }

    @PostMapping
    public Transaccion crear(@RequestBody Transaccion transaccion) {
        return service.guardar(transaccion);
    }

    @PutMapping("/{id}")
    public Transaccion actualizar(@PathVariable Long id, @RequestBody Transaccion transaccion) {
        return service.actualizar(id, transaccion);
    }

    @PostMapping("/traspaso")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void realizarTraspaso(@RequestBody TraspasoRequest request) {
        service.realizarTraspaso(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
