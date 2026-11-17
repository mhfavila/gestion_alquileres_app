package com.gestion.alquileres.inmobiliaria;

import com.gestion.alquileres.comun.Contexto;
import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ContratoService {

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
        return repository.findByContextoOrderByFechaInicioDesc(ContextoHolder.get());
    }

    public Contrato guardar(Contrato contrato) {
        Contexto contextoActual = ContextoHolder.get();

        if (contrato.getInmueble() == null || contrato.getInmueble().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inmueble es requerido");
        }
        if (contrato.getInquilino() == null || contrato.getInquilino().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inquilino es requerido");
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

        contrato.setInmueble(inmueble);
        contrato.setInquilino(inquilino);
        contrato.setContexto(contextoActual);

        return repository.save(contrato);
    }
}
