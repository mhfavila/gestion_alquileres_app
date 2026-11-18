package com.gestion.alquileres.inmobiliaria;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gestion.alquileres.comun.Contexto;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    List<Contrato> findByContextoOrderByFechaInicioDesc(Contexto contexto);

    @Query("""
            SELECT COUNT(c) > 0
            FROM Contrato c
            WHERE c.contexto = :contexto
              AND c.inmueble.id = :inmuebleId
              AND c.inquilino.id = :inquilinoId
              AND c.fechaInicio = :fechaInicio
            """)
    boolean existsByContextoAndInmuebleIdAndInquilinoIdAndFechaInicio(
            @Param("contexto") Contexto contexto,
            @Param("inmuebleId") Long inmuebleId,
            @Param("inquilinoId") Long inquilinoId,
            @Param("fechaInicio") LocalDate fechaInicio);
}
