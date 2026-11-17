package com.gestion.alquileres.inmobiliaria;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gestion.alquileres.comun.Contexto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inmuebles")
public class Inmueble {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoInmueble tipo;

    @Column(nullable = false, length = 250)
    private String direccion;

    @Column(length = 50)
    private String referencia;

    @Column
    private Integer metros;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Contexto contexto;

    @JsonIgnore
    @OneToMany(mappedBy = "inmueble")
    private List<Contrato> contratos = new ArrayList<>();

    protected Inmueble() {
    }

    public Inmueble(TipoInmueble tipo, String direccion, String referencia, Integer metros, Contexto contexto) {
        this.tipo = tipo;
        this.direccion = direccion;
        this.referencia = referencia;
        this.metros = metros;
        this.contexto = contexto;
    }

    public Long getId() {
        return id;
    }

    public TipoInmueble getTipo() {
        return tipo;
    }

    public void setTipo(TipoInmueble tipo) {
        this.tipo = tipo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public Integer getMetros() {
        return metros;
    }

    public void setMetros(Integer metros) {
        this.metros = metros;
    }

    public Contexto getContexto() {
        return contexto;
    }

    public void setContexto(Contexto contexto) {
        this.contexto = contexto;
    }

    public List<Contrato> getContratos() {
        return contratos;
    }
}
