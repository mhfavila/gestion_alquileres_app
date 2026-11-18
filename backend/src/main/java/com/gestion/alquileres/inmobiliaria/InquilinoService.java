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
public class InquilinoService {

    private static final Logger log = LoggerFactory.getLogger(InquilinoService.class);

    private final InquilinoRepository repository;

    public InquilinoService(InquilinoRepository repository) {
        this.repository = repository;
    }

    public List<Inquilino> obtenerPorContexto() {
        log.info("Listando inquilinos del contexto {}", ContextoHolder.get());
        return repository.findByContextoOrderByNombreAsc(ContextoHolder.get());
    }

    public Inquilino guardar(Inquilino inquilino) {
        inquilino.setContexto(ContextoHolder.get());
        log.info("Guardando inquilino entrante: nombre='{}', email='{}', telefono='{}', documento='{}', contexto={}",
                inquilino.getNombre(), inquilino.getEmail(), inquilino.getTelefono(), inquilino.getDocumento(), inquilino.getContexto());
        try {
            Inquilino guardado = repository.save(inquilino);
            log.info("Inquilino guardado correctamente con ID: {}", guardado.getId());
            return guardado;
        } catch (DataIntegrityViolationException ex) {
            log.warn("No se puede guardar inquilino: documento duplicado '{}' en contexto {}", inquilino.getDocumento(), inquilino.getContexto());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un inquilino con ese documento en este contexto");
        } catch (RuntimeException ex) {
            log.error("Error al guardar inquilino: nombre='{}'", inquilino.getNombre(), ex);
            throw ex;
        }
    }

    @Transactional
    public Inquilino actualizar(Long id, Inquilino datos) {
        log.info("Actualizando inquilino ID {}: nombre='{}', email='{}', documento='{}'",
                id, datos.getNombre(), datos.getEmail(), datos.getDocumento());
        Inquilino existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inquilino no encontrado con ID: " + id));

        if (!existente.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Inquilino pertenece a otro contexto");
        }

        existente.setNombre(datos.getNombre());
        existente.setEmail(datos.getEmail());
        existente.setTelefono(datos.getTelefono());
        existente.setDocumento(datos.getDocumento());

        try {
            Inquilino actualizada = repository.save(existente);
            log.info("Inquilino actualizado correctamente con ID: {}", actualizada.getId());
            return actualizada;
        } catch (DataIntegrityViolationException ex) {
            log.warn("No se puede actualizar el inquilino ID {}: documento duplicado '{}'", id, datos.getDocumento());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un inquilino con ese documento en este contexto");
        } catch (RuntimeException ex) {
            log.error("Error al actualizar inquilino ID {}", id, ex);
            throw ex;
        }
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando inquilino con ID: {} (contexto: {})", id, ContextoHolder.get());
        Inquilino inquilino = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inquilino no encontrado con ID: " + id));

        if (!inquilino.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Inquilino pertenece a otro contexto");
        }

        try {
            repository.delete(inquilino);
            repository.flush();
            log.info("Inquilino eliminado correctamente: ID {} ('{}')", id, inquilino.getNombre());
        } catch (DataIntegrityViolationException e) {
            log.warn("No se puede eliminar el inquilino ID {}: tiene contratos asociados", id, e);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el inquilino porque tiene contratos asociados");
        } catch (RuntimeException ex) {
            log.error("Error inesperado eliminando inquilino ID {}", id, ex);
            throw ex;
        }
    }
}
