package com.gestion.alquileres.inmobiliaria;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InmuebleService {

    private final InmuebleRepository repository;

    public InmuebleService(InmuebleRepository repository) {
        this.repository = repository;
    }

    public List<Inmueble> obtenerPorContexto() {
        return repository.findByContextoOrderByDireccionAsc(ContextoHolder.get());
    }

    public Inmueble guardar(Inmueble inmueble) {
        inmueble.setContexto(ContextoHolder.get());
        return repository.save(inmueble);
    }
}
