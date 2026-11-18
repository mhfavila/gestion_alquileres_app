package com.gestion.alquileres.finanzas;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestion.alquileres.comun.Contexto;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    // Spring Data JPA crea la query SQL automáticamente solo con leer el nombre del método
    List<Transaccion> findByContextoOrderByFechaDesc(Contexto contexto);

    List<Transaccion> findByContextoAndFechaBetweenOrderByFechaDesc(
            Contexto contexto, LocalDate fechaInicio, LocalDate fechaFin);
}
