package com.gestion.alquileres.inmobiliaria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inquilinos")
public class Inquilino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(length = 30)
    private String documento;

    @OneToMany(mappedBy = "inquilino")
    private List<Contrato> contratos = new ArrayList<>();

    protected Inquilino() {
    }

    public Inquilino(String nombre, String email, String telefono, String documento) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.documento = documento;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public List<Contrato> getContratos() {
        return contratos;
    }
}
