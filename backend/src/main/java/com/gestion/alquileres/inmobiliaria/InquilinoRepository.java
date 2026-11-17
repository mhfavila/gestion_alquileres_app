package com.gestion.alquileres.inmobiliaria;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestion.alquileres.comun.Contexto;

@Repository
public interface InquilinoRepository extends JpaRepository<Inquilino, Long> {

    List<Inquilino> findByContextoOrderByNombreAsc(Contexto contexto);
}
