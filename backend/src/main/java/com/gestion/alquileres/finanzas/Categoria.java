package com.gestion.alquileres.finanzas;

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
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "categorias",
        uniqueConstraints = @UniqueConstraint(columnNames = {"nombre", "contexto"})
)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Contexto contexto;

    @OneToMany(mappedBy = "categoria")
    private List<Transaccion> transacciones = new ArrayList<>();

    protected Categoria() {
    }

    public Categoria(String nombre, Contexto contexto) {
        this.nombre = nombre;
        this.contexto = contexto;
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
