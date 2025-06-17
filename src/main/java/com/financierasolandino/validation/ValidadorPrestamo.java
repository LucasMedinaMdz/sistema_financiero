package com.financierasolandino.validation;

import com.financierasolandino.model.Prestamo;
import com.financierasolandino.util.Utilidad;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Scanner;

public class ValidadorPrestamo {
    private static final double MONTO_MIN_PERSONAL = 100000;
    private static final double MONTO_MAX_PERSONAL = 50000000;
    private static final double MONTO_MIN_HIPOTECARIO = 20000000;
    private static final double MONTO_MAX_HIPOTECARIO = 140000000;
    private static final int MIN_CUOTAS_PERSONAL = 6;
    private static final int MAX_CUOTAS_PERSONAL = 60;
    private static final int MIN_CUOTAS_HIPOTECARIO = 12;
    private static final int MAX_CUOTAS_HIPOTECARIO = 360;

    // Se usa NumberFormat para mostrar los valores en formato moneda de Argentina
    private static final NumberFormat formatoMoneda = Utilidad.getArgentinaNumberFormat();

    public static Prestamo.TipoPrestamo validarTipoPrestamo(Scanner scanner) {
        while (true) {
            System.out.println("\nSeleccione el tipo de préstamo:");
            System.out.println("1. Personal");
            System.out.println("2. Hipotecario");
            System.out.println("0. Cancelar");
            System.out.print("Opción: ");
            String input = scanner.nextLine().trim();
            switch (input) {
                case "1":
                    return Prestamo.TipoPrestamo.PERSONAL;
                case "2":
                    return Prestamo.TipoPrestamo.HIPOTECARIO;
                case "0":
                    return null;
                default:
                    System.out.println("❌ Opción inválida. Intente nuevamente.");
            }
        }
    }

    public static Double validarMonto(Scanner scanner, Prestamo.TipoPrestamo tipoPrestamo) {
        if (tipoPrestamo == Prestamo.TipoPrestamo.PERSONAL) {
            return validarMontoPersonal(scanner);
        } else {
            return validarMontoHipotecario(scanner);
        }
    }

    private static Double validarMontoPersonal(Scanner scanner) {

        System.out.println("\n::::::::::::::::: 💼 PRÉSTAMO PERSONAL ::::::::::::::::::");
        System.out.println("—————————————————————————————————————————————————————————");
        System.out.println("📘 Préstamo de libre destino, sin necesidad de garantía.");
        System.out.println();
        System.out.println("📏 Requisitos:");
        System.out.println("  ● Monto del préstamo: $100.000 a $50.000.000");
        System.out.println("  ● Cuotas: entre 6 y 60 meses");
        System.out.println();
        System.out.println("💡 Ingresá el monto deseado para continuar.");
        System.out.println("—————————————————————————————————————————————————————————\n");


        while (true) {
            System.out.print("Ingrese el monto del préstamo (" + formatoMoneda.format(MONTO_MIN_PERSONAL) + " - " + formatoMoneda.format(MONTO_MAX_PERSONAL) + ") o 'cancelar': ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("cancelar")) {
                return null;
            }

            // Reemplaza la coma por punto si es necesario
            input = input.replace(',', '.');

            try {
                // Usa BigDecimal para analizar el input
                BigDecimal montoBD = new BigDecimal(input);
                // Verifica si es un número entero (sin parte decimal)
                if (montoBD.stripTrailingZeros().scale() > 0) {
                    System.out.println("Error: El monto debe ser un número entero sin decimales.");
                    continue;
                }
                double monto = montoBD.doubleValue();
                if (monto >= MONTO_MIN_PERSONAL && monto <= MONTO_MAX_PERSONAL) {
                    return monto;
                } else {
                    System.out.println("El monto debe estar entre " + formatoMoneda.format(MONTO_MIN_PERSONAL) +
                            " y " + formatoMoneda.format(MONTO_MAX_PERSONAL) + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Entrada inválida. Ingrese un número válido.");
            }
        }
    }

    private static Double validarMontoHipotecario(Scanner scanner) {

        System.out.println("\n:::::::::::::::: 🏠 PRÉSTAMO HIPOTECARIO ::::::::::::::::");
        System.out.println("—————————————————————————————————————————————————————————");
        System.out.println("📘 Solicitás dinero usando una propiedad como garantía.");
        System.out.println();
        System.out.println("💰 Podés solicitar hasta el 80% del valor de la propiedad.");
        System.out.println("Ej: propiedad de $50.000.000 → préstamo hasta $40.000.000.");;
        System.out.println();
        System.out.println("📏 Requisitos:");
        System.out.println("  ● Valor de propiedad: $25.000.000 a $175.000.000");
        System.out.println("  ● Monto del préstamo: $20.000.000 a $140.000.000");
        System.out.println("  ● Cuotas: entre 12 y 360 meses");
        System.out.println();
        System.out.println("💡 Ingresá el valor de tu propiedad para continuar.");
        System.out.println("—————————————————————————————————————————————————————————\n");

        // Validar el valor de la propiedad
        double valorPropiedad;
        while (true) {
            System.out.print("Ingrese el valor de la propiedad o 'cancelar': ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("cancelar")) {
                return null;
            }

            // Reemplaza la coma por punto si es necesario
            input = input.replace(',', '.');

            // Verifica si el número tiene demasiados dígitos
            String soloDigitos = input.replaceAll("[^0-9]", ""); // Elimina puntos, comas, etc.
            if (soloDigitos.length() > 12) {
                System.out.println("❌ Ingresó una cifra gigantesca. Verifique e intente nuevamente.");
                continue;
            }

            try {
                BigDecimal valorPropiedadBD = new BigDecimal(input);
                if (valorPropiedadBD.stripTrailingZeros().scale() > 0) {
                    System.out.println("❌ Error: El valor de la propiedad debe ser un número entero sin decimales.");
                    continue;
                }
                valorPropiedad = valorPropiedadBD.doubleValue();
                if (valorPropiedad <= 0) {
                    System.out.println("❌ El valor de la propiedad debe ser positivo.");
                    continue;
                }
                double montoMaximo = valorPropiedad * 0.8;
                if (montoMaximo < MONTO_MIN_HIPOTECARIO) {
                    System.out.println("❌ Error: El 80% del valor de la propiedad (" + formatoMoneda.format(montoMaximo) +
                            ") es menor al monto mínimo requerido (" + formatoMoneda.format(MONTO_MIN_HIPOTECARIO) + ").");
                    System.out.println("Por favor, ingrese un valor de propiedad mayor o 'cancelar' para salir.");
                    continue;
                }
                if (montoMaximo > MONTO_MAX_HIPOTECARIO) {
                    System.out.println("❌ Error: El 80% del valor de la propiedad (" + formatoMoneda.format(montoMaximo) +
                            ") es mayor al monto máximo permitido (" + formatoMoneda.format(MONTO_MAX_HIPOTECARIO) + ").");
                    System.out.println("Por favor, ingrese un valor de propiedad menor o 'cancelar' para salir.");
                    continue;
                }
                // Mostrar el monto máximo con hasta 2 decimales
                BigDecimal montoMaximoBD = new BigDecimal(montoMaximo).setScale(2, BigDecimal.ROUND_DOWN);
                System.out.println("Monto máximo disponible (80% del valor de la propiedad): " + formatoMoneda.format(montoMaximoBD));
                break; // Salir del bucle de validación del valor de la propiedad
            } catch (NumberFormatException e) {
                System.out.println("❌ Entrada inválida. Ingrese un número válido.");
            }
        }

        // Validar el monto del préstamo
        while (true) {
            double montoMaximo = Math.min(valorPropiedad * 0.8, MONTO_MAX_HIPOTECARIO);
            BigDecimal montoMaximoBD = new BigDecimal(montoMaximo).setScale(2, BigDecimal.ROUND_DOWN);
            System.out.print("Ingrese el monto del préstamo (hasta 2 decimales, " + formatoMoneda.format(MONTO_MIN_HIPOTECARIO) +
                    " - " + formatoMoneda.format(montoMaximoBD) + ") o 'cancelar': ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("cancelar")) {
                return null;
            }

            // Reemplaza la coma por punto
            input = input.replace(',', '.');

            try {
                BigDecimal montoBD = new BigDecimal(input);
                // Verifica que el número tenga a lo sumo 2 decimales
                if (montoBD.scale() > 2) {
                    System.out.println("❌ Error: El monto debe tener a lo sumo 2 decimales.");
                    continue;
                }
                double monto = montoBD.doubleValue();
                if (monto >= MONTO_MIN_HIPOTECARIO && monto <= montoMaximo) {
                    // Redondear el monto a 2 decimales para consistencia
                    return montoBD.setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
                } else {
                    System.out.println("El monto debe estar entre " + formatoMoneda.format(MONTO_MIN_HIPOTECARIO) +
                            " y " + formatoMoneda.format(montoMaximoBD) + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Entrada inválida. Ingrese un número válido.");
            }
        }
    }

    public static Integer validarNumeroCuotas(Scanner scanner, Prestamo.TipoPrestamo tipoPrestamo) {
        if (tipoPrestamo == Prestamo.TipoPrestamo.PERSONAL) {
            return validarNumeroCuotasPersonal(scanner);
        } else {
            return validarNumeroCuotasHipotecario(scanner);
        }
    }

    private static Integer validarNumeroCuotasPersonal(Scanner scanner) {
        System.out.println("\nPlazos y Tasas de Interés (TNA) disponibles:");
        System.out.println("Plazo\t\tTNA");
        System.out.println("6 meses\t\t92.00%");
        System.out.println("12 meses\t93.00%");
        System.out.println("24 meses\t95.00%");
        System.out.println("36 meses\t94.00%");
        System.out.println("48 meses\t93.00%");
        System.out.println("60 meses\t94.00%");
        while (true) {
            System.out.print("Ingrese el número de cuotas (6-60) o 'cancelar': ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("cancelar")) {
                return null;
            }
            try {
                int cuotas = Integer.parseInt(input);
                if (cuotas >= MIN_CUOTAS_PERSONAL && cuotas <= MAX_CUOTAS_PERSONAL) {
                    return cuotas;
                } else {
                    System.out.println("El número de cuotas debe estar entre 6 y 60.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Entrada inválida. Ingrese un número entero.");
            }
        }
    }

    private static Integer validarNumeroCuotasHipotecario(Scanner scanner) {
        while (true) {
            System.out.print("Ingrese el número de cuotas (12-360) o 'cancelar': ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("cancelar")) {
                return null;
            }
            try {
                int cuotas = Integer.parseInt(input);
                if (cuotas >= MIN_CUOTAS_HIPOTECARIO && cuotas <= MAX_CUOTAS_HIPOTECARIO) {
                    return cuotas;
                } else {
                    System.out.println("El número de cuotas debe estar entre 12 y 360.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Entrada inválida. Ingrese un número entero.");
            }
        }
    }

    public static Double validarTasaInteres(Scanner scanner, Prestamo.TipoPrestamo tipoPrestamo, int numeroCuotas) {
        if (tipoPrestamo == Prestamo.TipoPrestamo.PERSONAL) {
            return obtenerTasaPersonal(numeroCuotas);
        } else {
            return validarTasaHipotecario(scanner);
        }
    }

    private static Double obtenerTasaPersonal(int numeroCuotas) {
        if (numeroCuotas <= 6) return 92.00;
        else if (numeroCuotas <= 12) return 93.00;
        else if (numeroCuotas <= 24) return 95.00;
        else if (numeroCuotas <= 36) return 94.00;
        else if (numeroCuotas <= 48) return 93.00;
        else return 94.00;
    }

    public static Double validarTasaHipotecario(Scanner scanner) {
        while (true) {
            System.out.println("\n¿Es cliente del banco?");
            System.out.println("1. Sí");
            System.out.println("2. No");
            System.out.println("0. Salir");
            System.out.print("Opción: ");
            String input = scanner.nextLine().trim();
            switch (input) {
                case "1":
                    return 9.50;
                case "2":
                    return 12.50;
                case "0":
                    return null;
                default:
                    System.out.println("Opción inválida. Intente nuevamente.");
            }
        }
    }
}