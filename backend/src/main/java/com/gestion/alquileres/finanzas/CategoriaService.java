package com.gestion.alquileres.finanzas;

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
public class CategoriaService {

    private static final Logger log = LoggerFactory.getLogger(CategoriaService.class);

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public List<Categoria> obtenerPorContexto() {
        log.info("Listando categorías del contexto {}", ContextoHolder.get());
        return repository.findByContextoOrderByNombreAsc(ContextoHolder.get());
    }

    public Categoria guardar(Categoria categoria) {
        categoria.setContexto(ContextoHolder.get());
        log.info("Guardando categoría entrante: nombre='{}', contexto={}", categoria.getNombre(), categoria.getContexto());
        try {
            Categoria guardada = repository.save(categoria);
            log.info("Categoría guardada correctamente con ID: {}", guardada.getId());
            return guardada;
        } catch (RuntimeException ex) {
            log.error("Error al guardar categoría: nombre='{}'", categoria.getNombre(), ex);
            throw ex;
        }
    }

    @Transactional
    public Categoria actualizar(Long id, Categoria datos) {
        log.info("Actualizando categoría ID {}: nombre='{}'", id, datos.getNombre());
        Categoria existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Categoria no encontrada con ID: " + id));

        if (!existente.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Categoria pertenece a otro contexto");
        }

        existente.setNombre(datos.getNombre());
        try {
            Categoria actualizada = repository.save(existente);
            log.info("Categoría actualizada correctamente con ID: {}", actualizada.getId());
            return actualizada;
        } catch (DataIntegrityViolationException ex) {
            log.warn("No se puede actualizar la categoría ID {}: nombre duplicado '{}'", id, datos.getNombre());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una categoria con ese nombre en este contexto");
        } catch (RuntimeException ex) {
            log.error("Error al actualizar categoría ID {}", id, ex);
            throw ex;
        }
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando categoría con ID: {} (contexto: {})", id, ContextoHolder.get());
        Categoria categoria = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Categoria no encontrada con ID: " + id));

        if (!categoria.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Categoria pertenece a otro contexto");
        }

        try {
            repository.delete(categoria);
            repository.flush();
            log.info("Categoría eliminada correctamente: ID {} ('{}')", id, categoria.getNombre());
        } catch (DataIntegrityViolationException e) {
            log.warn("No se puede eliminar la categoría ID {}: tiene transacciones asociadas", id, e);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar la categoria porque tiene transacciones asociadas");
        } catch (RuntimeException ex) {
            log.error("Error inesperado eliminando categoría ID {}", id, ex);
            throw ex;
        }
    }
}
