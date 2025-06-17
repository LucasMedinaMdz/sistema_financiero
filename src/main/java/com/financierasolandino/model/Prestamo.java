package com.financierasolandino.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Prestamo {
    private String idPrestamo;
    private String idCliente;
    private BigDecimal monto;
    private BigDecimal tasaInteres;
    private int numeroCuotas;
    private TipoPrestamo tipoPrestamo;
    private LocalDate fechaCreacion;
    private BigDecimal saldoPendiente;
    private EstadoPrestamo estado;

    public enum TipoPrestamo {
        PERSONAL, HIPOTECARIO
    }

    public enum EstadoPrestamo {
        ACTIVO, CANCELADO, EN_MORA
    }

    public Prestamo(String idPrestamo, String idCliente, double monto, double tasaInteres, int numeroCuotas,
                    TipoPrestamo tipoPrestamo, LocalDate fechaCreacion, double saldoPendiente, EstadoPrestamo estado) {
        this.idPrestamo = idPrestamo;
        this.idCliente = idCliente;
        this.monto = new BigDecimal(monto).setScale(2, RoundingMode.HALF_UP);
        this.tasaInteres = new BigDecimal(tasaInteres).setScale(4, RoundingMode.HALF_UP);
        this.numeroCuotas = numeroCuotas;
        this.tipoPrestamo = tipoPrestamo;
        this.fechaCreacion = fechaCreacion;
        this.saldoPendiente = new BigDecimal(saldoPendiente).setScale(2, RoundingMode.HALF_UP);
        this.estado = estado;
    }

    // Getters y setters
    public String getIdPrestamo() {
        return idPrestamo;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public double getMonto() {
        return monto.doubleValue();
    }

    public double getTasaInteres() {
        return tasaInteres.doubleValue();
    }

    public int getNumeroCuotas() {
        return numeroCuotas;
    }

    public TipoPrestamo getTipoPrestamo() {
        return tipoPrestamo;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public double getSaldoPendiente() {
        return saldoPendiente.doubleValue();
    }

    public EstadoPrestamo getEstado() {
        return estado;
    }
}