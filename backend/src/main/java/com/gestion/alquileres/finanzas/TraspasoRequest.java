package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.comun.Contexto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TraspasoRequest(
    Long walletOrigenId,
    Contexto contextoDestino,
    BigDecimal importe,
    LocalDate fecha,
    String descripcion
) {
    public TraspasoRequest {
        if (walletOrigenId == null) {
            throw new IllegalArgumentException("walletOrigenId es obligatorio");
        }
        if (contextoDestino == null) {
            throw new IllegalArgumentException("contextoDestino es obligatorio");
        }
        if (importe == null || importe.signum() <= 0) {
            throw new IllegalArgumentException("importe debe ser mayor que cero");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("fecha es obligatoria");
        }
    }
}
