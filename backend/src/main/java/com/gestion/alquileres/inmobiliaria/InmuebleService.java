package com.gestion.alquileres.inmobiliaria;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InmuebleService {

    private static final Logger log = LoggerFactory.getLogger(InmuebleService.class);

    private final InmuebleRepository repository;

    public InmuebleService(InmuebleRepository repository) {
        this.repository = repository;
    }

    public List<Inmueble> obtenerPorContexto() {
        log.info("Listando inmuebles del contexto {}", ContextoHolder.get());
        return repository.findByContextoOrderByDireccionAsc(ContextoHolder.get());
    }

    public Inmueble guardar(Inmueble inmueble) {
        inmueble.setContexto(ContextoHolder.get());
        log.info("Guardando inmueble entrante: tipo={}, direccion='{}', referencia='{}', metros={}, contexto={}",
                inmueble.getTipo(), inmueble.getDireccion(), inmueble.getReferencia(), inmueble.getMetros(), inmueble.getContexto());
        try {
            Inmueble guardado = repository.save(inmueble);
            log.info("Inmueble guardado correctamente con ID: {}", guardado.getId());
            return guardado;
        } catch (RuntimeException ex) {
            log.error("Error al guardar inmueble: direccion='{}'", inmueble.getDireccion(), ex);
            throw ex;
        }
    }

    @Transactional
    public Inmueble actualizar(Long id, Inmueble datos) {
        log.info("Actualizando inmueble ID {}: tipo={}, direccion='{}', referencia='{}', metros={}",
                id, datos.getTipo(), datos.getDireccion(), datos.getReferencia(), datos.getMetros());
        Inmueble existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inmueble no encontrado con ID: " + id));

        if (!existente.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Inmueble pertenece a otro contexto");
        }

        existente.setTipo(datos.getTipo());
        existente.setDireccion(datos.getDireccion());
        existente.setReferencia(datos.getReferencia());
        existente.setMetros(datos.getMetros());

        try {
            Inmueble actualizada = repository.save(existente);
            log.info("Inmueble actualizado correctamente con ID: {}", actualizada.getId());
            return actualizada;
        } catch (RuntimeException ex) {
            log.error("Error al actualizar inmueble ID {}", id, ex);
            throw ex;
        }
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando inmueble con ID: {} (contexto: {})", id, ContextoHolder.get());
        Inmueble inmueble = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inmueble no encontrado con ID: " + id));

        if (!inmueble.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Inmueble pertenece a otro contexto");
        }

        try {
            repository.delete(inmueble);
            repository.flush();
            log.info("Inmueble eliminado correctamente: ID {} ('{}')", id, inmueble.getDireccion());
        } catch (DataIntegrityViolationException e) {
            log.warn("No se puede eliminar el inmueble ID {}: tiene contratos asociados", id, e);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el inmueble porque tiene contratos asociados");
        } catch (RuntimeException ex) {
            log.error("Error inesperado eliminando inmueble ID {}", id, ex);
            throw ex;
        }
    }
}
