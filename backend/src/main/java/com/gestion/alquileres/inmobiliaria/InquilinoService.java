package com.gestion.alquileres.inmobiliaria;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InquilinoService {

    private final InquilinoRepository repository;

    public InquilinoService(InquilinoRepository repository) {
        this.repository = repository;
    }

    public List<Inquilino> obtenerPorContexto() {
        return repository.findByContextoOrderByNombreAsc(ContextoHolder.get());
    }

    public Inquilino guardar(Inquilino inquilino) {
        inquilino.setContexto(ContextoHolder.get());
        return repository.save(inquilino);
    }
}
