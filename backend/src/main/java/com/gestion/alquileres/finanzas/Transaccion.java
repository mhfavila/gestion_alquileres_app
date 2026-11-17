package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.comun.Contexto;
import com.gestion.alquileres.comun.TipoTransaccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "transacciones")
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTransaccion tipo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Contexto contexto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "url_comprobante", length = 1000)
    private String urlComprobante;

    @Column(name = "ocr_procesado", nullable = false)
    private boolean ocrProcesado = false;

    protected Transaccion() {
    }

    public Transaccion(BigDecimal importe, TipoTransaccion tipo, LocalDate fecha, String descripcion,
                       Wallet wallet, Categoria categoria) {
        if (wallet == null || categoria == null || wallet.getContexto() != categoria.getContexto()) {
            throw new IllegalArgumentException("Wallet y categoria deben pertenecer al mismo contexto");
        }
        setImporte(importe);
        this.tipo = tipo;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.wallet = wallet;
        this.categoria = categoria;
        this.contexto = wallet.getContexto();
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        if (importe == null || importe.signum() <= 0) {
            throw new IllegalArgumentException("El importe debe ser mayor que cero");
        }
        this.importe = importe;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransaccion tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Contexto getContexto() {
        return contexto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getUrlComprobante() {
        return urlComprobante;
    }

    public void setUrlComprobante(String urlComprobante) {
        this.urlComprobante = urlComprobante;
    }

    public boolean isOcrProcesado() {
        return ocrProcesado;
    }

    public void setOcrProcesado(boolean ocrProcesado) {
        this.ocrProcesado = ocrProcesado;
    }
}
