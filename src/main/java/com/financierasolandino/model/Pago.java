package com.financierasolandino.model;

import java.time.LocalDate;

public class Pago {
    private String idPrestamo;
    private int numeroCuota;
    private double montoPagado;
    private LocalDate fechaPago;

    public Pago(String idPrestamo, int numeroCuota, double montoPagado, LocalDate fechaPago) {
        this.idPrestamo = idPrestamo;
        this.numeroCuota = numeroCuota;
        this.montoPagado = montoPagado;
        this.fechaPago = fechaPago;
    }

    // Getters y Setters
    public String getIdPrestamo() {
        return idPrestamo;
    }

    public int getNumeroCuota() {
        return numeroCuota;
    }

    public double getMontoPagado() {
        return montoPagado;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }
}