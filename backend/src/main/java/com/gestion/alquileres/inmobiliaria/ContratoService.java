package com.gestion.alquileres.inmobiliaria;

import com.gestion.alquileres.comun.Contexto;
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
public class ContratoService {

    private static final Logger log = LoggerFactory.getLogger(ContratoService.class);

    private final ContratoRepository repository;
    private final InmuebleRepository inmuebleRepository;
    private final InquilinoRepository inquilinoRepository;

    public ContratoService(ContratoRepository repository, InmuebleRepository inmuebleRepository,
                           InquilinoRepository inquilinoRepository) {
        this.repository = repository;
        this.inmuebleRepository = inmuebleRepository;
        this.inquilinoRepository = inquilinoRepository;
    }

    public List<Contrato> obtenerPorContexto() {
        log.info("Listando contratos del contexto {}", ContextoHolder.get());
        return repository.findByContextoOrderByFechaInicioDesc(ContextoHolder.get());
    }

    public Contrato guardar(Contrato contrato) {
        Contexto contextoActual = ContextoHolder.get();
        log.info("Guardando contrato entrante: inmuebleId={}, inquilinoId={}, fechaInicio={}, fechaFin={}, importeMensual={}, fianza={}, contexto={}",
                contrato.getInmueble() != null ? contrato.getInmueble().getId() : null,
                contrato.getInquilino() != null ? contrato.getInquilino().getId() : null,
                contrato.getFechaInicio(), contrato.getFechaFin(), contrato.getImporteMensual(), contrato.getFianza(), contextoActual);

        if (contrato.getInmueble() == null || contrato.getInmueble().getId() == null || contrato.getInmueble().getId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un inmueble válido");
        }
        if (contrato.getInquilino() == null || contrato.getInquilino().getId() == null || contrato.getInquilino().getId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un inquilino válido");
        }
        if (contrato.getFechaInicio() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio es obligatoria");
        }
        if (contrato.getFechaFin() != null && contrato.getFechaFin().isBefore(contrato.getFechaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        if (contrato.getImporteMensual() == null || contrato.getImporteMensual().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe mensual debe ser mayor que cero");
        }

        Inmueble inmueble = inmuebleRepository.findById(contrato.getInmueble().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inmueble no encontrado con ID: " + contrato.getInmueble().getId()));

        Inquilino inquilino = inquilinoRepository.findById(contrato.getInquilino().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inquilino no encontrado con ID: " + contrato.getInquilino().getId()));

        if (!inmueble.getContexto().equals(contextoActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Inmueble pertenece a otro contexto: " + inmueble.getContexto() + ", contexto actual: " + contextoActual);
        }

        if (!inquilino.getContexto().equals(contextoActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Inquilino pertenece a otro contexto: " + inquilino.getContexto() + ", contexto actual: " + contextoActual);
        }

        if (repository.existsByContextoAndInmuebleIdAndInquilinoIdAndFechaInicio(
                contextoActual, inmueble.getId(), inquilino.getId(), contrato.getFechaInicio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un contrato registrado con el mismo inmueble, inquilino y fecha de inicio");
        }

        contrato.setInmueble(inmueble);
        contrato.setInquilino(inquilino);
        contrato.setContexto(contextoActual);

        try {
            Contrato guardado = repository.save(contrato);
            log.info("Contrato guardado correctamente con ID: {} (inmueble={}, inquilino={})",
                    guardado.getId(), inmueble.getId(), inquilino.getId());
            return guardado;
        } catch (RuntimeException ex) {
            log.error("Error al guardar contrato: inmuebleId={}, inquilinoId={}",
                    inmueble.getId(), inquilino.getId(), ex);
            throw ex;
        }
    }

    @Transactional
    public Contrato actualizar(Long id, Contrato datos) {
        Contexto contextoActual = ContextoHolder.get();
        log.info("Actualizando contrato ID {}: inmuebleId={}, inquilinoId={}, fechaInicio={}, fechaFin={}, importeMensual={}, fianza={}",
                id,
                datos.getInmueble() != null ? datos.getInmueble().getId() : null,
                datos.getInquilino() != null ? datos.getInquilino().getId() : null,
                datos.getFechaInicio(), datos.getFechaFin(), datos.getImporteMensual(), datos.getFianza());

        Contrato existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contrato no encontrado con ID: " + id));

        if (!existente.getContexto().equals(contextoActual)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Contrato pertenece a otro contexto");
        }

        if (datos.getInmueble() == null || datos.getInmueble().getId() == null || datos.getInmueble().getId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un inmueble válido");
        }
        if (datos.getInquilino() == null || datos.getInquilino().getId() == null || datos.getInquilino().getId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un inquilino válido");
        }
        if (datos.getFechaInicio() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio es obligatoria");
        }
        if (datos.getFechaFin() != null && datos.getFechaFin().isBefore(datos.getFechaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        if (datos.getImporteMensual() == null || datos.getImporteMensual().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe mensual debe ser mayor que cero");
        }

        Inmueble inmueble = inmuebleRepository.findById(datos.getInmueble().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inmueble no encontrado con ID: " + datos.getInmueble().getId()));

        Inquilino inquilino = inquilinoRepository.findById(datos.getInquilino().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inquilino no encontrado con ID: " + datos.getInquilino().getId()));

        if (!inmueble.getContexto().equals(contextoActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Inmueble pertenece a otro contexto: " + inmueble.getContexto());
        }
        if (!inquilino.getContexto().equals(contextoActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Inquilino pertenece a otro contexto: " + inquilino.getContexto());
        }

        existente.setInmueble(inmueble);
        existente.setInquilino(inquilino);
        existente.setFechaInicio(datos.getFechaInicio());
        existente.setFechaFin(datos.getFechaFin());
        existente.setImporteMensual(datos.getImporteMensual());
        existente.setFianza(datos.getFianza());
        existente.setContexto(contextoActual);

        try {
            Contrato actualizado = repository.save(existente);
            log.info("Contrato actualizado correctamente con ID: {}", actualizado.getId());
            return actualizado;
        } catch (RuntimeException ex) {
            log.error("Error al actualizar contrato ID {}", id, ex);
            throw ex;
        }
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando contrato con ID: {} (contexto: {})", id, ContextoHolder.get());
        Contrato contrato = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contrato no encontrado con ID: " + id));

        if (!contrato.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Contrato pertenece a otro contexto");
        }

        try {
            repository.delete(contrato);
            repository.flush();
            log.info("Contrato eliminado correctamente: ID {}", id);
        } catch (DataIntegrityViolationException e) {
            log.warn("No se puede eliminar el contrato ID {}: tiene transacciones asociadas", id, e);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el contrato porque tiene transacciones asociadas");
        } catch (RuntimeException ex) {
            log.error("Error inesperado eliminando contrato ID {}", id, ex);
            throw ex;
        }
    }
}
