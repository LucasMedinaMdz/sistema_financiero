package com.financierasolandino.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Cuota {
    private String idPrestamo;
    private int numeroCuota;
    private BigDecimal montoCuota;
    private BigDecimal tasaAplicada;
    private LocalDate fechaVencimiento;
    private BigDecimal capitalAmortizado;

    public Cuota(String idPrestamo, int numeroCuota, double montoCuota, double tasaAplicada, LocalDate fechaVencimiento, double capitalAmortizado) {
        this.idPrestamo = idPrestamo;
        this.numeroCuota = numeroCuota;
        this.montoCuota = new BigDecimal(montoCuota).setScale(2, RoundingMode.HALF_UP);
        this.tasaAplicada = new BigDecimal(tasaAplicada).setScale(4, RoundingMode.HALF_UP);
        this.fechaVencimiento = fechaVencimiento;
        this.capitalAmortizado = new BigDecimal(capitalAmortizado).setScale(2, RoundingMode.HALF_UP);
    }

    public String getIdPrestamo() {
        return idPrestamo;
    }

    public int getNumeroCuota() {
        return numeroCuota;
    }

    public double getMontoCuota() {
        return montoCuota.doubleValue();
    }

    public double getTasaAplicada() {
        return tasaAplicada.doubleValue();
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public double getCapitalAmortizado() {
        return capitalAmortizado.doubleValue();
    }
}