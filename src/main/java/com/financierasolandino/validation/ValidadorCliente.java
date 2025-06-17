package com.financierasolandino.validation;

import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

public class ValidadorCliente {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // Regex para dirección más específica: "Calle Av. Nombre 123 Ciudad"
    // Al menos: letras y espacios para la calle, un espacio, un número, un espacio, y ciudad (letras y espacios)
    private static final Pattern DIRECCION_ESPECIFICA_PATTERN = Pattern.compile(
            "^[a-zA-ZáéíóúÁÉÍÓÚñÑ .]{2,50} \\d{1,5} [a-zA-ZáéíóúÁÉÍÓÚñÑ ]{2,30}$"
    );

    public static String validarDNI(Scanner scanner) {
        while (true) {
            System.out.print("Ingrese DNI del cliente o 'cancelar' para salir: ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.equalsIgnoreCase("cancelar")) return null;

            if (input.isEmpty()) {
                System.out.println("Error: El DNI no puede estar vacío.");
                continue;
            }

            if (input.matches("\\d{8}")) {
                // DNI válido de 8 dígitos
                return input;
            } else if (input.matches("\\d{7}")) {
                // 7 dígitos: anteponer 0
                System.out.println("Aviso: Se detectaron 7 dígitos. Se antepone un '0' automáticamente.");
                return "0" + input;
            } else if (input.matches("[MF]\\d{7}")) {
                // Empieza con M o F + 7 dígitos
                return input;
            } else {
                System.out.println("Error: DNI inválido. Debe ser 8 dígitos, 7 dígitos (se antepone 0), o 'M'/'F' seguido de 7 dígitos.");
            }
        }
    }

    public static String validarNombre(Scanner scanner) {
        while (true) {
            System.out.print("Ingrese nombre completo (solo letras y espacios) o 'cancelar' para salir: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("cancelar")) return null;

            if (input.isEmpty()) {
                System.out.println("Error: El nombre no puede estar vacío.");
            } else if (input.length() < 3) {
                System.out.println("Error: El nombre debe tener al menos 3 caracteres.");
            } else if (input.length() > 50) {
                System.out.println("Error: El nombre no puede tener más de 50 caracteres.");
            } else if (!input.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$")) {
                System.out.println("Error: El nombre solo puede contener letras y espacios.");
            } else if (input.matches(".* {2,}.*")) {
                System.out.println("Error: El nombre no puede contener múltiples espacios seguidos.");
            } else if (input.matches(".*([a-zA-ZáéíóúÁÉÍÓÚñÑ])\\1\\1.*")) {
                System.out.println("Error: El nombre no puede contener tres letras iguales consecutivas.");
            } else if (input.split(" ").length < 2) {
                System.out.println("Error: Debe ingresar al menos un nombre y un apellido.");
            } else {
                return capitalizarNombre(input);
            }
        }
    }

    private static String capitalizarNombre(String input) {
        String[] palabras = input.toLowerCase().split(" ");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                resultado.append(Character.toUpperCase(palabra.charAt(0)))
                        .append(palabra.substring(1))
                        .append(" ");
            }
        }
        return resultado.toString().trim();
    }


    public static String validarDireccion(Scanner scanner) {
        while (true) {
            System.out.print("Ingrese dirección (formato: Calle/Nom. Av. + número + ciudad) o 'cancelar' para salir: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("cancelar")) return null;

            if (input.isEmpty()) {
                System.out.println("Error: La dirección no puede estar vacía.");
            } else if (input.length() < 5) {
                System.out.println("Error: La dirección debe tener al menos 5 caracteres.");
            } else if (input.length() > 50) {
                System.out.println("Error: La dirección no puede exceder los 50 caracteres.");
            } else if (!input.matches("^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ .,°]+$")) {
                System.out.println("Error: La dirección solo puede contener letras, números, espacios, punto (.), coma (,) y símbolo de grado (°).");
            } else if (input.matches(".* {2,}.*")) {
                System.out.println("Error: La dirección no puede contener múltiples espacios seguidos.");
            } else if (input.matches(".* [.,°].*")) {
                System.out.println("Error: No debe haber espacio antes de los signos de puntuación (., °).");
            } else if (!DIRECCION_ESPECIFICA_PATTERN.matcher(input).matches()) {
                System.out.println("Error: La dirección debe tener formato: Calle o Av. + número + ciudad (ejemplo: 'Av. Libertador 1234 Mendoza').");
            } else {
                return capitalizarDireccion(input);
            }
        }
    }

    private static String capitalizarDireccion(String input) {
        String[] palabras = input.toLowerCase().split(" ");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                resultado.append(Character.toUpperCase(palabra.charAt(0)))
                        .append(palabra.substring(1))
                        .append(" ");
            }
        }
        return resultado.toString().trim();
    }


    public static String validarTelefono(Scanner scanner) {
        while (true) {
            System.out.print("Ingrese teléfono (10 a 15 dígitos) o 'cancelar' para salir: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("cancelar")) return null;

            String cleaned = input.replaceAll("[\\s\\-().]", "");

            if (cleaned.indexOf('+') > 0) {
                System.out.println("Error: El símbolo '+' solo puede aparecer al inicio.");
                continue;
            }

            String digitsOnly = cleaned.startsWith("+") ? cleaned.substring(1) : cleaned;

            if (!digitsOnly.matches("\\d+")) {
                System.out.println("Error: El teléfono solo puede contener dígitos numéricos y opcionalmente un '+' al inicio.");
                continue;
            }

            int length = digitsOnly.length();
            if (length < 10 || length > 15) {
                System.out.println("Error: El teléfono debe tener entre 10 y 15 dígitos.");
                continue;
            }

            return cleaned;
        }
    }


    private static final Set<String> DOMINIOS_VALIDOS = Set.of(
            "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "empresa.com", "icloud.com", "protonmail.com", "zoho.com", "itu.uncu.edu.ar", "uncu.edu.ar"
    );

    public static String validarCorreoElectronico(Scanner scanner) {
        while (true) {
            System.out.print("Ingrese correo electrónico o 'cancelar' para salir: ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equalsIgnoreCase("cancelar")) return null;

            if (input.length() > 100) {
                System.out.println("Error: El correo no puede tener más de 100 caracteres.");
                continue;
            }

            if (input.startsWith(".") || input.endsWith(".")) {
                System.out.println("Error: El correo no puede comenzar ni terminar con un punto.");
                continue;
            }

            if (!EMAIL_PATTERN.matcher(input).matches()) {
                System.out.println("Error: El correo electrónico no es válido en formato.");
                continue;
            }

            // Extraer dominio después de '@'
            String dominio = input.substring(input.indexOf('@') + 1);
            if (!DOMINIOS_VALIDOS.contains(dominio)) {
                System.out.println("Error: El dominio del correo no es válido. Dominios permitidos: " + DOMINIOS_VALIDOS);
                continue;
            }

            return input;
        }
    }
}