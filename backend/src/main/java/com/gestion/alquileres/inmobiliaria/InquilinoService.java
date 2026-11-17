package com.gestion.alquileres.inmobiliaria;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional
    public void eliminar(Long id) {
        Inquilino inquilino = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inquilino no encontrado con ID: " + id));

        if (!inquilino.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Inquilino pertenece a otro contexto");
        }

        try {
            repository.delete(inquilino);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el inquilino porque tiene contratos asociados");
        }
    }
}
