package com.gestion.alquileres.finanzas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestion.alquileres.comun.Contexto;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByContextoOrderByNombreAsc(Contexto contexto);

    Categoria findByContextoAndNombreIgnoreCase(Contexto contexto, String nombre);
}
