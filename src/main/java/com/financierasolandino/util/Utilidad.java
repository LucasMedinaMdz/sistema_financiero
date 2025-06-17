package com.financierasolandino.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public class Utilidad {

    /**
     * Metodo auxiliar para obtener el NumberFormat con el Locale de Argentina
     * y configurado específicamente para formato monetario (2 decimales).
     */
    public static NumberFormat getArgentinaNumberFormat() {
        // Se crea los símbolos de formato para el Locale de Argentina
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "AR"));
        // Se configura explícitamente la coma como separador decimal
        symbols.setDecimalSeparator(',');
        // Se configura explícitamente el punto como separador de miles
        symbols.setGroupingSeparator('.');

        // Se define el patrón:
        // #,##0    -> Asegura separador de miles y al menos un dígito antes del decimal
        // .00      -> Asegura siempre dos dígitos después del decimal, rellenando con ceros si es necesario
        DecimalFormat format = new DecimalFormat("'$'#,##0.00", symbols);

        // Opcional: Permite asegurarse de que el redondeo se haga como se espera en finanzas
        // HALF_UP es el redondeo estándar (ej. 1.235 -> 1.24, 1.234 -> 1.23)
        format.setRoundingMode(java.math.RoundingMode.HALF_UP);

        return format;
    }
}