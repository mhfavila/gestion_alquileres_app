package com.gestion.alquileres.finanzas;

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
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Contexto contexto;

    @JsonIgnore
    @OneToMany(mappedBy = "wallet")
    private List<Transaccion> transacciones = new ArrayList<>();

    protected Wallet() {
    }

    public Wallet(String nombre, Contexto contexto) {
        this.nombre = nombre;
        this.contexto = contexto;
    }

    public void addTransaccion(Transaccion transaccion) {
        transacciones.add(transaccion);
        transaccion.setWallet(this);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Contexto getContexto() {
        return contexto;
    }

    public void setContexto(Contexto contexto) {
        this.contexto = contexto;
    }

    public List<Transaccion> getTransacciones() {
        return transacciones;
    }
}
