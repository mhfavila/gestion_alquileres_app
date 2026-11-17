package com.gestion.alquileres.inmobiliaria;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional
    public void eliminar(Long id) {
        Inmueble inmueble = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inmueble no encontrado con ID: " + id));

        if (!inmueble.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Inmueble pertenece a otro contexto");
        }

        try {
            repository.delete(inmueble);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el inmueble porque tiene contratos asociados");
        }
    }
}
