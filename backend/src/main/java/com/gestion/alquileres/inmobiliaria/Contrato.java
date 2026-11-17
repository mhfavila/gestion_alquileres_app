package com.gestion.alquileres.inmobiliaria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contratos")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmueble_id", nullable = false)
    private Inmueble inmueble;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquilino_id", nullable = false)
    private Inquilino inquilino;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "importe_mensual", nullable = false, precision = 14, scale = 2)
    private BigDecimal importeMensual;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal fianza;

    protected Contrato() {
    }

    public Contrato(Inmueble inmueble, Inquilino inquilino, LocalDate fechaInicio, LocalDate fechaFin,
                    BigDecimal importeMensual, BigDecimal fianza) {
        this.inmueble = inmueble;
        this.inquilino = inquilino;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.importeMensual = importeMensual;
        this.fianza = fianza;
    }

    public Long getId() {
        return id;
    }

    public Inmueble getInmueble() {
        return inmueble;
    }

    public void setInmueble(Inmueble inmueble) {
        this.inmueble = inmueble;
    }

    public Inquilino getInquilino() {
        return inquilino;
    }

    public void setInquilino(Inquilino inquilino) {
        this.inquilino = inquilino;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public BigDecimal getImporteMensual() {
        return importeMensual;
    }

    public void setImporteMensual(BigDecimal importeMensual) {
        this.importeMensual = importeMensual;
    }

    public BigDecimal getFianza() {
        return fianza;
    }

    public void setFianza(BigDecimal fianza) {
        this.fianza = fianza;
    }
}
