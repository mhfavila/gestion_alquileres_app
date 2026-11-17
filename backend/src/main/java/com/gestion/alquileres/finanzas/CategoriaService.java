package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public List<Categoria> obtenerPorContexto() {
        return repository.findByContextoOrderByNombreAsc(ContextoHolder.get());
    }

    public Categoria guardar(Categoria categoria) {
        categoria.setContexto(ContextoHolder.get());
        return repository.save(categoria);
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Categoria no encontrada con ID: " + id));

        if (!categoria.getContexto().equals(ContextoHolder.get())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Categoria pertenece a otro contexto");
        }

        try {
            repository.delete(categoria);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar la categoria porque tiene transacciones asociadas");
        }
    }
}
