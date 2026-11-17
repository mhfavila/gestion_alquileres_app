package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.stereotype.Service;

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
}
