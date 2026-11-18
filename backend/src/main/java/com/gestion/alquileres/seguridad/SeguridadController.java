package com.gestion.alquileres.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seguridad")
public class SeguridadController {

    public record CambioPasswordRequest(String passwordActual, String passwordNueva) {
    }

    private final SeguridadService seguridadService;

    public SeguridadController(SeguridadService seguridadService) {
        this.seguridadService = seguridadService;
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@RequestBody CambioPasswordRequest request) {
        seguridadService.cambiarPassword(request.passwordActual(), request.passwordNueva());
    }
}
