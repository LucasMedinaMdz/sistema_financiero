package com.financierasolandino.service;

import com.financierasolandino.model.Cuota;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalculadoraCuotas {
    public static List<Cuota> calcularCuota(String idPrestamo, double monto, double tasaInteres, int numeroCuotas,
                                            LocalDate fechaCreacion) {
        if (monto <= 0 || tasaInteres < 0 || numeroCuotas <= 0) {
            throw new IllegalArgumentException("Monto, tasa de interés y número de cuotas deben ser positivos.");
        }

        List<Cuota> cuotas = new ArrayList<>();
        BigDecimal montoBD = new BigDecimal(monto).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tasaInteresBD = new BigDecimal(tasaInteres).setScale(4, RoundingMode.HALF_UP);
        BigDecimal tasaMensual = tasaInteresBD.divide(new BigDecimal("1200"), 8, RoundingMode.HALF_UP); // TNA a mensual
        BigDecimal cuotaFija = calcularCuotaFija(montoBD, tasaMensual, numeroCuotas);
        BigDecimal saldoPendiente = montoBD;

        for (int i = 1; i <= numeroCuotas; i++) {
            // Calcular intereses
            BigDecimal intereses = saldoPendiente.multiply(tasaMensual).setScale(2, RoundingMode.HALF_UP);

            // Calcular capital amortizado
            BigDecimal capitalAmortizado;
            if (i == numeroCuotas) {
                // Última cuota: amortizar todo el saldo restante
                capitalAmortizado = saldoPendiente.setScale(2, RoundingMode.HALF_UP);
                cuotaFija = capitalAmortizado.add(intereses).setScale(2, RoundingMode.HALF_UP);
            } else {
                capitalAmortizado = cuotaFija.subtract(intereses).setScale(2, RoundingMode.HALF_UP);
            }

            // Actualizar saldo pendiente
            saldoPendiente = saldoPendiente.subtract(capitalAmortizado).setScale(2, RoundingMode.HALF_UP);

            // Crear cuota
            LocalDate fechaVencimiento = fechaCreacion.plusMonths(i);
            Cuota cuota = new Cuota(idPrestamo, i, cuotaFija.doubleValue(), tasaInteres, fechaVencimiento, capitalAmortizado.doubleValue());
            cuotas.add(cuota);
        }

        // Verificar que el saldo final sea 0
        if (saldoPendiente.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Error en cálculo de cuotas: saldo pendiente final no es cero: " + saldoPendiente);
        }

        return cuotas;
    }

    private static BigDecimal calcularCuotaFija(BigDecimal monto, BigDecimal tasaMensual, int numeroCuotas) {
        // Calcular (1 + tasaMensual)^numeroCuotas
        BigDecimal unoMasTasa = BigDecimal.ONE.add(tasaMensual);
        BigDecimal baseElevada = unoMasTasa.pow(numeroCuotas, new java.math.MathContext(10, RoundingMode.HALF_UP));

        // Calcular 1 / (1 + tasaMensual)^numeroCuotas
        BigDecimal denominador = BigDecimal.ONE.divide(baseElevada, 10, RoundingMode.HALF_UP);

        // Calcular 1 - [1 / (1 + tasaMensual)^numeroCuotas]
        BigDecimal factor = BigDecimal.ONE.subtract(denominador);

        // Calcular cuota: monto * (tasaMensual / factor)
        return monto.multiply(tasaMensual).divide(factor, 2, RoundingMode.HALF_UP);
    }
}