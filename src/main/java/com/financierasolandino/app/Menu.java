package com.financierasolandino.app;

import com.financierasolandino.dao.*;
import com.financierasolandino.db.ConexionDB;
import com.financierasolandino.model.Cliente;
import com.financierasolandino.model.Cuota;
import com.financierasolandino.model.Pago;
import com.financierasolandino.model.Prestamo;
import com.financierasolandino.service.CalculadoraCuotas;
import com.financierasolandino.util.Utilidad;
import com.financierasolandino.validation.ValidadorCliente;
import com.financierasolandino.validation.ValidadorPrestamo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Menu {
    private final ClienteDAO clienteDAO;
    private final PrestamoDAO prestamoDAO;
    private final CuotaDAO cuotaDAO;
    private final PagoDAO pagoDAO;
    private final Scanner scanner;
    private final ReporteDAO reporteDAO;
    private final NumberFormat formatoMoneda = Utilidad.getArgentinaNumberFormat();

    public Menu() {
        this.clienteDAO = new ClienteDAO();
        this.prestamoDAO = new PrestamoDAOImpl();
        this.cuotaDAO = new CuotaDAO();
        this.pagoDAO = new PagoDAO();
        this.scanner = new Scanner(System.in);
        this.reporteDAO = new ReporteDAO();
    }

    public void iniciar() {
        while (true) {
            try {
                mostrarMenu();
                int opcion = obtenerOpcion();
                if (opcion == 0) {
                    System.out.println("Saliendo del sistema...");
                    break;
                }
                procesarOpcion(opcion);
            } catch (VolverMenuPrincipalException e) {
                System.out.println("↩️ Regresando al menú principal...");
                pausarConsola();
            }
        }
        scanner.close();
    }

    private void mostrarMenu() {
        System.out.println("\n════════════════════════════════════════════");
        System.out.println("       📊 SISTEMA DE GESTIÓN FINANCIERA");
        System.out.println("════════════════════════════════════════════");
        System.out.println(" 1️⃣  Gestión de Clientes");
        System.out.println(" 2️⃣  Gestión de Préstamos");
        System.out.println(" 3️⃣  Consultas y Reportes");
        System.out.println(" 4️⃣  Exportación de Datos");
        System.out.println(" 0️⃣  Salir");
        System.out.println("════════════════════════════════════════════");
        System.out.print("Seleccione una opción: ");
    }

    public class VolverMenuPrincipalException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private int obtenerOpcion() {
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.print("Seleccione una opción: ");
                continue; // Ignora entradas vacías
            }
            if (input.equalsIgnoreCase("m")) {
                throw new VolverMenuPrincipalException();
            }
            if (input.equalsIgnoreCase("q")) {
                System.out.println("Saliendo del sistema...");
                System.exit(0);
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                return -1; // Opción inválida
            }
        }
    }


    private void procesarOpcion(int opcion) {
        switch (opcion) {
            case 1 -> mostrarMenuClientes();
            case 2 -> mostrarMenuPrestamos();
            case 3 -> mostrarMenuConsultasReportes();
            case 4 -> mostrarMenuExportacion();
            case 0 -> {}
            default -> System.out.println("❌ Opción inválida. Intente nuevamente.");
        }
    }

    private void mostrarMenuClientes() {
        while (true) {
            System.out.println("\n📁 ─── Gestión de Clientes ─── 📁");
            System.out.println(" 1. Registrar nuevo cliente");
            System.out.println(" 2. Consultar cliente por DNI");
            System.out.println(" 3. Editar datos de cliente");
            System.out.println(" 4. Listar todos los clientes");
            System.out.println(" ------------------------------");
            System.out.println(" 0. Menú anterior");
            System.out.println(" Q. Salir del sistema");
            System.out.println("───────────────────────────────");
            System.out.print("Seleccione una opción: ");
            int opcion = obtenerOpcion();
            switch (opcion) {
                case 1 -> registrarCliente();
                case 2 -> consultarCliente();
                case 3 -> editarCliente();
                case 4 -> listarClientes();
                case 0 -> { return; }
                default -> System.out.println("❌ Opción inválida. Intente nuevamente.");
            }
        }
    }

    private void mostrarMenuPrestamos() {
        while (true) {
            System.out.println("\n🏦 ─── GESTIÓN DE PRÉSTAMOS ─── 🏦");
            System.out.println(" 1. Crear nuevo préstamo");
            System.out.println(" 2. Mostrar préstamos de cliente");
            System.out.println(" -------------------------------");
            System.out.println(" 0. Menú anterior");
            System.out.println(" Q. Salir del sistema");
            System.out.println("───────────────────────────────");
            System.out.print("Seleccione una opción: ");
            int opcion = obtenerOpcion();
            switch (opcion) {
                case 1 -> crearPrestamo();
                case 2 -> mostrarPrestamosCliente();
                case 0 -> { return; }
                default -> System.out.println("❌ Opción inválida. Intente nuevamente.");
            }
        }
    }

    private void mostrarMenuGestionPrestamo(String idPrestamo) {
        while (true) {
            System.out.println("\n💳 ─── GESTIÓN DE UN PRÉSTAMO ─── 💳");
            System.out.println(" 1. Resumen de cuotas");
            System.out.println(" 2. Estado del préstamo");
            System.out.println(" 3. Pagos y Cuotas");
            System.out.println(" ------------------------------------");
            System.out.println(" 0. Menú anterior");
            System.out.println(" M. Menú principal");
            System.out.println(" Q. Salir del sistema");
            System.out.println("─────────────────────────────────────");
            System.out.print("Seleccione una opción: ");
            int opcion = obtenerOpcion();
            switch (opcion) {
                case 1 -> mostrarCuota(idPrestamo);
                case 2 -> consultarEstadoPrestamo(idPrestamo);
                case 3 -> mostrarMenuPagosYCuotas(idPrestamo);
                case 0 -> { return; }
                default -> System.out.println("❌ Opción inválida. Intente nuevamente.");
            }
        }
    }

    private void mostrarMenuPagosYCuotas(String idPrestamo) {
        while (true) {
            System.out.println("\n💰 ──────── PAGOS Y CUOTAS ──────── 💰");
            System.out.println(" 1. Registrar pago de cuota");
            System.out.println(" 2. Consultar cuotas pendientes");
            System.out.println(" 3. Ver historial de pagos");
            System.out.println(" -----------------------------------");
            System.out.println(" 0. Menú anterior");
            System.out.println(" M. Menú principal");
            System.out.println(" Q. Salir del sistema");
            System.out.println("────────────────────────────────────");
            System.out.print("Seleccione una opción: ");
            int opcion = obtenerOpcion();
            switch (opcion) {
                case 1 -> registrarPago(idPrestamo);
                case 2 -> consultarPagosYCuotasPendientes(idPrestamo);
                case 3 -> consultarHistorialPagos(idPrestamo);
                case 0 -> { return; }
                default -> System.out.println("❌ Opción inválida. Intente nuevamente.");
            }
        }
    }

    private void mostrarMenuConsultasReportes() {
        while (true) {
            System.out.println("\n📄 ───── Consultas y Reportes ───── 📄");
            System.out.println(" 1. Reporte de préstamos activos");
            System.out.println(" 2. Reporte de clientes en mora y penalidades");
            System.out.println(" 3. Proyección de ingresos");
            System.out.println(" ----------------------------------");
            System.out.println(" 0. Menú anterior");
            System.out.println(" Q. Salir del sistema");
            System.out.println("───────────────────────────────────");
            System.out.print("Seleccione una opción: ");
            int opcion = obtenerOpcion();
            switch (opcion) {
                case 1 -> consultarClientesConPrestamosActivos();
                case 2 -> consultarClientesEnMora();
                case 3 -> proyectarIngresos();
                case 0 -> { return; }
                default -> System.out.println("❌ Opción inválida. Intente nuevamente.");
            }
        }
    }

    private void mostrarMenuExportacion() {
        while (true) {
            System.out.println("\n📤 ─── Exportación de Datos ─── 📤");
            System.out.println(" 1. Exportar datos de clientes");
            System.out.println(" 2. Exportar datos de préstamos");
            System.out.println(" 3. Exportar historial de pagos");
            System.out.println(" 4. Exportar clientes en mora");
            System.out.println(" -------------------------------");
            System.out.println(" 0. Menú anterior");
            System.out.println(" Q. Salir del sistema");
            System.out.println("────────────────────────────────");
            System.out.print("Seleccione una opción: ");
            int opcion = obtenerOpcion();
            try {
                switch (opcion) {
                    case 1 -> exportarClientes();
                    case 2 -> exportarPrestamos();
                    case 3 -> exportarPagos();
                    case 4 -> exportarClientesEnMora();
                    case 0 -> { return; }
                    default -> System.out.println("❌ Opción inválida. Intente nuevamente.");
                }
            } catch (ClienteDAOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private String registrarCliente(String dniInicial) {
        try {
            String idCliente = dniInicial != null ? dniInicial : ValidadorCliente.validarDNI(scanner);
            if (idCliente == null) {
                System.out.println("❌ Registro cancelado.");
                pausarConsola();
                return null;
            }
            while (clienteDAO.verificarExistenciaCliente(idCliente)) {
                System.out.println("❌ Error: El DNI " + idCliente + " ya está registrado.");
                idCliente = ValidadorCliente.validarDNI(scanner);
                if (idCliente == null) {
                    System.out.println("❌ Registro cancelado.");
                    pausarConsola();
                    return null;
                }
            }
            String nombre = ValidadorCliente.validarNombre(scanner);
            if (nombre == null) {
                System.out.println("❌ Registro cancelado.");
                pausarConsola();
                return null;
            }
            String direccion = ValidadorCliente.validarDireccion(scanner);
            if (direccion == null) {
                System.out.println("❌ Registro cancelado.");
                pausarConsola();
                return null;
            }
            String telefono = ValidadorCliente.validarTelefono(scanner);
            if (telefono == null) {
                System.out.println("❌ Registro cancelado.");
                pausarConsola();
                return null;
            }
            String correo = ValidadorCliente.validarCorreoElectronico(scanner);
            if (correo == null) {
                System.out.println("❌ Registro cancelado.");
                pausarConsola();
                return null;
            }

            while (true) {
                System.out.println("\n📋 Resumen del registro:");
                System.out.println("1. DNI: " + idCliente);
                System.out.println("2. Nombre: " + nombre);
                System.out.println("3. Dirección: " + direccion);
                System.out.println("4. Teléfono: " + telefono);
                System.out.println("5. Correo electrónico: " + correo);
                System.out.print("¿Desea confirmar el registro? (si/no): ");
                String confirmacion = scanner.nextLine().trim().toLowerCase();

                if (confirmacion.equals("si")) break;
                if (confirmacion.equals("no")) {
                    System.out.print("¿Desea editar algún campo? Ingrese el número (1-5) o 'cancelar' para anular el registro: ");
                    String opcion = scanner.nextLine().trim().toLowerCase();
                    switch (opcion) {
                        case "1":
                            String nuevoDNI = ValidadorCliente.validarDNI(scanner);
                            if (nuevoDNI == null) {
                                System.out.println("❌ Registro cancelado.");
                                pausarConsola();
                                return null;
                            }
                            while (clienteDAO.verificarExistenciaCliente(nuevoDNI)) {
                                System.out.println("❌ El DNI " + nuevoDNI + " ya está registrado. Intente con otro.");
                                nuevoDNI = ValidadorCliente.validarDNI(scanner);
                                if (nuevoDNI == null) {
                                    System.out.println("❌ Registro cancelado.");
                                    pausarConsola();
                                    return null;
                                }
                            }
                            idCliente = nuevoDNI;
                            break;
                        case "2":
                            nombre = ValidadorCliente.validarNombre(scanner);
                            if (nombre == null) {
                                System.out.println("❌ Registro cancelado.");
                                pausarConsola();
                                return null;
                            }
                            break;
                        case "3":
                            direccion = ValidadorCliente.validarDireccion(scanner);
                            if (direccion == null) {
                                System.out.println("❌ Registro cancelado.");
                                pausarConsola();
                                return null;
                            }
                            break;
                        case "4":
                            telefono = ValidadorCliente.validarTelefono(scanner);
                            if (telefono == null) {
                                System.out.println("❌ Registro cancelado.");
                                pausarConsola();
                                return null;
                            }
                            break;
                        case "5":
                            correo = ValidadorCliente.validarCorreoElectronico(scanner);
                            if (correo == null) {
                                System.out.println("❌ Registro cancelado.");
                                pausarConsola();
                                return null;
                            }
                            break;
                        case "cancelar":
                            System.out.println("❌ Registro cancelado.");
                            pausarConsola();
                            return null;
                        default:
                            System.out.println("❌ Opción inválida. Intente nuevamente.");
                            break;
                    }
                } else {
                    System.out.println("❌ Respuesta no válida. Escriba 'si' o 'no'.");
                }
            }

            Cliente cliente = new Cliente(idCliente, nombre, direccion, telefono, correo);
            boolean registrado = clienteDAO.registrarCliente(cliente);
            if (registrado) {
                System.out.println("✅ Cliente registrado exitosamente.");
                return idCliente;
            } else {
                System.out.println("❌ No se pudo registrar el cliente.");
                return null;
            }
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
            return null;
        }
    }

    private String registrarCliente() {
        return registrarCliente(null); // Llama al metodo sobrecargado sin DNI inicial
    }

    private void consultarCliente() {
        try {
            String idCliente = ValidadorCliente.validarDNI(scanner);
            if (idCliente == null) {
                System.out.println("❌ Operación cancelada.");
                pausarConsola();
                return;
            }
            Optional<Cliente> cliente = clienteDAO.consultarCliente(idCliente);
            if (cliente.isPresent()) {
                Cliente c = cliente.get();
                System.out.println("\nCliente: " + c.getNombre() +
                        " | DNI: " + c.getIdCliente() +
                        " | Dirección: " + c.getDireccion() +
                        " | Teléfono: " + c.getTelefono() +
                        " | Correo: " + c.getCorreoElectronico());
                List<Prestamo> prestamos = prestamoDAO.obtenerPrestamosPorCliente(idCliente);
                if (prestamos.isEmpty()) {
                    System.out.println("❌ El cliente no tiene préstamos registrados.");
                } else {
                    System.out.println("\nPréstamos registrados:");
                    for (Prestamo p : prestamos) {
                        System.out.println(":: ID Préstamo: " + p.getIdPrestamo() +
                                " | Monto: " + formatoMoneda.format(p.getMonto()) +
                                " | Tasa de Interés: " + String.format("%.2f", p.getTasaInteres()) + "%" +
                                " | Cuotas: " + p.getNumeroCuotas() +
                                " | Tipo: " + p.getTipoPrestamo() +
                                " | Fecha Creación: " + p.getFechaCreacion() +
                                " | Saldo Pendiente: " + formatoMoneda.format(p.getSaldoPendiente()) +
                                " | Estado: " + p.getEstado());
                        List<Cuota> cuotas = cuotaDAO.obtenerCuotas(p.getIdPrestamo());
                        if (!cuotas.isEmpty()) {
                            System.out.println("    Cuota Mensual: " + formatoMoneda.format(cuotas.get(0).getMontoCuota()));
                        }
                        List<Pago> pagos = pagoDAO.obtenerPagos(p.getIdPrestamo());
                        if (!pagos.isEmpty()) {
                            System.out.println("    Pagos realizados:");
                            for (Pago pago : pagos) {
                                System.out.println("      Cuota " + pago.getNumeroCuota() +
                                        ": " + formatoMoneda.format(pago.getMontoPagado()) +
                                        ", Fecha: " + pago.getFechaPago());
                            }
                        } else {
                            System.out.println("    No hay pagos registrados.");
                        }
                    }
                }
                pausarConsola();
            } else {
                System.out.println("❌ Cliente no encontrado.");
                pausarConsola();
            }
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }

    private void editarCliente() {
        try {
            String idCliente = ValidadorCliente.validarDNI(scanner);
            if (idCliente == null) {
                System.out.println("❌ Edición cancelada.");
                pausarConsola();
                return;
            }
            if (!clienteDAO.verificarExistenciaCliente(idCliente)) {
                System.out.println("❌ Cliente no encontrado.");
                pausarConsola();
                return;
            }
            Optional<Cliente> clienteExistente = clienteDAO.obtenerCliente(idCliente);
            if (clienteExistente.isEmpty()) {
                System.out.println("❌ Error al obtener los datos del cliente.");
                pausarConsola();
                return;
            }
            Cliente cliente = clienteExistente.get();
            String nombre = cliente.getNombre();
            String direccion = cliente.getDireccion();
            String telefono = cliente.getTelefono();
            String correo = cliente.getCorreoElectronico();

            while (true) {
                System.out.println("\n=== Editar Cliente ===");
                System.out.println("DNI: " + idCliente);
                System.out.println("Nombre: " + nombre);
                System.out.println("1. Dirección actual: " + direccion);
                System.out.println("2. Teléfono actual: " + telefono);
                System.out.println("3. Correo electrónico actual: " + correo);
                System.out.println("0. Cancelar");
                System.out.print("Seleccione un campo para editar: ");
                int opcion = obtenerOpcion();

                if (opcion == 0) {
                    System.out.println("❌ Edición cancelada.");
                    pausarConsola();
                    return;
                }
                switch (opcion) {
                    case 1 -> {
                        String nuevaDireccion = ValidadorCliente.validarDireccion(scanner);
                        if (nuevaDireccion == null) {
                            System.out.println("Modificación cancelada.");
                            pausarConsola();
                            continue;
                        }
                        System.out.println("Dirección nueva: " + nuevaDireccion);
                        if (confirmarOperacion("¿Desea guardar esta modificación? (si/no): ")) {
                            direccion = nuevaDireccion;
                            cliente = new Cliente(idCliente, nombre, direccion, telefono, correo);
                            if (clienteDAO.editarCliente(cliente)) {
                                System.out.println("✅ Dirección actualizada.");
                                pausarConsola();
                            } else {
                                System.out.println("❌ No se pudo actualizar la dirección.");
                                pausarConsola();
                            }
                        } else {
                            System.out.println("No se realizaron cambios.");
                            pausarConsola();
                        }
                    }
                    case 2 -> {
                        String nuevoTelefono = ValidadorCliente.validarTelefono(scanner);
                        if (nuevoTelefono == null) {
                            System.out.println("Modificación cancelada.");
                            pausarConsola();
                            continue;
                        }
                        System.out.println("Teléfono nuevo: " + nuevoTelefono);
                        if (confirmarOperacion("¿Desea guardar esta modificación? (si/no): ")) {
                            telefono = nuevoTelefono;
                            cliente = new Cliente(idCliente, nombre, direccion, telefono, correo);
                            if (clienteDAO.editarCliente(cliente)) {
                                System.out.println("✅ Teléfono actualizado.");
                                pausarConsola();
                            } else {
                                System.out.println("❌ No se pudo actualizar el teléfono.");
                                pausarConsola();
                            }
                        } else {
                            System.out.println("No se realizaron cambios.");
                            pausarConsola();
                        }
                    }
                    case 3 -> {
                        String nuevoCorreo = ValidadorCliente.validarCorreoElectronico(scanner);
                        if (nuevoCorreo == null) {
                            System.out.println("Modificación cancelada.");
                            continue;
                        }
                        System.out.println("Correo electrónico nuevo: " + nuevoCorreo);
                        if (confirmarOperacion("¿Desea guardar esta modificación? (si/no): ")) {
                            correo = nuevoCorreo;
                            cliente = new Cliente(idCliente, nombre, direccion, telefono, correo);
                            if (clienteDAO.editarCliente(cliente)) {
                                System.out.println("✅ Correo actualizado.");
                                pausarConsola();
                            } else {
                                System.out.println("❌ No se pudo actualizar el correo.");
                                pausarConsola();
                            }
                        } else {
                            System.out.println("No se realizaron cambios.");
                            pausarConsola();
                        }
                    }
                    default -> System.out.println("Opción inválida.");
                }
            }
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }

    private boolean confirmarOperacion(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String respuesta = scanner.nextLine().trim();
            String respuestaNormalizada = Normalizer.normalize(respuesta.toLowerCase(), Normalizer.Form.NFD)
                    .replaceAll("[^\\p{ASCII}]", "");
            if (respuestaNormalizada.equals("si")) return true;
            if (respuestaNormalizada.equals("no")) return false;
            System.out.println("❌ Respuesta no válida. Escriba 'si' o 'no'.");
        }
    }

    private void listarClientes() {
        try {
            List<Cliente> clientes = clienteDAO.listarClientes();
            if (clientes.isEmpty()) {
                System.out.println("No hay clientes registrados.");
                pausarConsola();
            } else {
                System.out.println("\nLista de clientes:");
                for (Cliente c : clientes) {
                    System.out.println("DNI: " + c.getIdCliente() +
                            " | Nombre: " + c.getNombre() +
                            " | Dirección: " + c.getDireccion() +
                            " | Teléfono: " + c.getTelefono() +
                            " | Correo: " + c.getCorreoElectronico());
                }
                pausarConsola();
            }
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }

    private void crearPrestamo() {
        while (true) { // Bucle principal para permitir nuevas simulaciones
            try {
                // Introducción clara
                System.out.println("\n════════════════════════════════════════════════════════════════");
                System.out.println("                  🧮 Simulador de Préstamos");
                System.out.println("════════════════════════════════════════════════════════════════");
                System.out.println("Simule un préstamo PERSONAL o HIPOTECARIO.");
                System.out.println();
                System.out.println("Pasos:");
                System.out.println("  1️⃣ Elegí tipo, monto y cuotas.");
                System.out.println("  2️⃣ Revisá y confirmá los datos.");
                System.out.println("  3️⃣ Obtené tu cuota mensual.");
                System.out.println();
                System.out.println("💡 Nota: Clientes registrados antes de comenzar obtienen una");
                System.out.println("   tasa preferencial del 9.50% en préstamos hipotecarios.");
                System.out.println("   Nuevos clientes aplican tasa estándar del 12.50%.");
                System.out.println("═══════════════════════════════════════════════════════════════\n");



                // Paso 1: Recolectar datos del préstamo
                Prestamo.TipoPrestamo tipoPrestamo = ValidadorPrestamo.validarTipoPrestamo(scanner);
                if (tipoPrestamo == null) {
                    System.out.println("❌ Operación cancelada.");
                    pausarConsola();
                    return; // Salir al menú principal
                }

                // Tasa inicial por defecto (12.50% para hipotecarios, se ajusta según el cliente)
                Double tasaInteres = (tipoPrestamo == Prestamo.TipoPrestamo.HIPOTECARIO) ? 12.50 : null;

                Double monto = ValidadorPrestamo.validarMonto(scanner, tipoPrestamo);
                if (monto == null) {
                    System.out.println("❌ Operación cancelada.");
                    pausarConsola();
                    return;
                }

                Integer numeroCuotas = ValidadorPrestamo.validarNumeroCuotas(scanner, tipoPrestamo);
                if (numeroCuotas == null) {
                    System.out.println("❌ Operación cancelada.");
                    pausarConsola();
                    return;
                }

                if (tipoPrestamo == Prestamo.TipoPrestamo.PERSONAL) {
                    tasaInteres = ValidadorPrestamo.validarTasaInteres(scanner, tipoPrestamo, numeroCuotas);
                    if (tasaInteres == null) {
                        System.out.println("❌ Operación cancelada.");
                        pausarConsola();
                        return;
                    }
                }

                // Paso 2: Permitir edición de datos
                while (true) {
                    System.out.println("\n=== Revisión de Datos del Préstamo ===");
                    System.out.println("Tipo de Préstamo: " + tipoPrestamo);
                    System.out.println("Monto: " + formatoMoneda.format(monto));
                    System.out.println("Número de Cuotas: " + numeroCuotas + "  (TNA: " +
                            (tasaInteres != null ? String.format("%.2f%%", tasaInteres) : "No definida") +
                            (tipoPrestamo == Prestamo.TipoPrestamo.HIPOTECARIO ? ", sujeta a verificación de cliente" : "") + ")");

                    // Menú de opciones
                    System.out.println("\nSeleccione: ");
                    System.out.println("1. Editar monto");
                    System.out.println("2. Editar número de cuotas");
                    System.out.println("3. Simular Préstamo");
                    System.out.println("0. Cancelar");

                    // Pedir la opción al usuario
                    System.out.print("Seleccione una opción: ");
                    String input = scanner.nextLine().trim().toLowerCase();

                    if (input.equals("cancelar") || input.equals("0")) {
                        System.out.println("❌ Operación cancelada.");
                        pausarConsola();
                        return;
                    }

                    if (input.equals("simular") || input.equals("3")) {
                        // Validar número de cuotas antes de simular
                        if (tipoPrestamo == Prestamo.TipoPrestamo.HIPOTECARIO && (numeroCuotas < 12 || numeroCuotas > 360)) {
                            System.out.println("❌ Error: Para préstamos hipotecarios, el número de cuotas debe estar entre 12 y 360.");
                            System.out.println("Por favor, edite el número de cuotas (opción 2).");
                            continue;
                        }
                        if (tipoPrestamo == Prestamo.TipoPrestamo.PERSONAL && (numeroCuotas < 6 || numeroCuotas > 60)) {
                            System.out.println("❌ Error: Para préstamos personales, el número de cuotas debe estar entre 6 y 60.");
                            System.out.println("Por favor, edite el número de cuotas (opción 2).");
                            continue;
                        }
                        if (tasaInteres == null && tipoPrestamo == Prestamo.TipoPrestamo.PERSONAL) {
                            System.out.println("❌ Debe especificar una tasa de interés para el préstamo personal.");
                            tasaInteres = ValidadorPrestamo.validarTasaInteres(scanner, tipoPrestamo, numeroCuotas);
                            if (tasaInteres == null) {
                                System.out.println("❌ Operación cancelada.");
                                pausarConsola();
                                return;
                            }
                        }
                        break;
                    }

                    switch (input) {
                        case "1":
                            monto = ValidadorPrestamo.validarMonto(scanner, tipoPrestamo);
                            if (monto == null) {
                                System.out.println("❌ Operación cancelada.");
                                pausarConsola();
                                return;
                            }
                            break;
                        case "2":
                            numeroCuotas = ValidadorPrestamo.validarNumeroCuotas(scanner, tipoPrestamo);
                            if (numeroCuotas == null) {
                                System.out.println("❌ Operación cancelada.");
                                pausarConsola();
                                return;
                            }
                            if (tipoPrestamo == Prestamo.TipoPrestamo.PERSONAL) {
                                tasaInteres = ValidadorPrestamo.validarTasaInteres(scanner, tipoPrestamo, numeroCuotas);
                                if (tasaInteres == null) {
                                    System.out.println("❌ Operación cancelada.");
                                    pausarConsola();
                                    return;
                                }
                            }
                            break;
                        default:
                            System.out.println("❌ Opción inválida. Ingrese un número (0-3).");
                    }
                }

                // Paso 3: Simulación de la cuota
                List<Cuota> cuotasPrevias = CalculadoraCuotas.calcularCuota("", monto, tasaInteres, numeroCuotas, LocalDate.now());
                double cuotaMensualEstimada = cuotasPrevias.get(0).getMontoCuota();

                System.out.println("\n=== Simulación del Préstamo ===");
                System.out.println("Tipo de Préstamo: " + tipoPrestamo);
                System.out.println("Monto: " + formatoMoneda.format(monto));
                System.out.println("Número de Cuotas: " + numeroCuotas);
                System.out.println("Tasa de Interés (TNA): " + String.format("%.2f%%", tasaInteres) +
                        (tipoPrestamo == Prestamo.TipoPrestamo.HIPOTECARIO ? " (sujeta a verificación de cliente)" : ""));
                System.out.println("Cuota Mensual Estimada: " + formatoMoneda.format(cuotaMensualEstimada));
                System.out.println("Sistema de Amortización: Francés");
                System.out.println("\nNota: Esta es una simulación. Para registrar el préstamo, se requerirá un DNI válido.");

                // Preguntar si desea continuar
                if (!confirmarOperacion("¿Desea registrar este préstamo? (si/no): ")) {
                    System.out.println("❌ Operación cancelada. La simulación no se ha guardado.");

                    // Menú para simular nuevo préstamo o cancelar
                    while (true) {
                        System.out.println("\nSeleccione una opción:");
                        System.out.println("1. Simular un nuevo préstamo");
                        System.out.println("0. Cancelar");

                        System.out.print("Seleccione una opción: ");
                        String input = scanner.nextLine().trim().toLowerCase();

                        if (input.equals("0")) {
                            System.out.println("❌ Operación cancelada.");
                            pausarConsola();
                            return; // Salir al menú principal
                        }

                        if (input.equals("1")) {
                            System.out.println("Iniciando un nuevo préstamo...");
                            break; // Rompe el bucle interno para reiniciar el proceso
                        } else {
                            System.out.println("❌ Opción inválida. Ingrese '0' para cancelar o '1' para simular un nuevo préstamo.");
                        }
                    }
                    continue; // Reinicia el bucle principal para una nueva simulación
                }

                // Paso 4: Validar DNI y ajustar tasa si es necesario
                String idCliente = ValidadorCliente.validarDNI(scanner);
                if (idCliente == null) {
                    System.out.println("❌ Operación cancelada.");
                    pausarConsola();
                    return;
                }

                // Verificar si el cliente estaba registrado antes de iniciar el proceso
                boolean esClientePreexistente = clienteDAO.verificarExistenciaCliente(idCliente);
                boolean esNuevoCliente = false;

                if (!esClientePreexistente) {
                    System.out.println("❌ No existe un cliente registrado con el DNI " + idCliente + ".");
                    if (confirmarOperacion("¿Desea registrar un nuevo cliente con este DNI? (si/no): ")) {
                        String nuevoDNI = registrarCliente(idCliente); // Obtiene el DNI registrado
                        if (nuevoDNI != null && clienteDAO.verificarExistenciaCliente(nuevoDNI)) {
                            idCliente = nuevoDNI; // Actualiza idCliente con el DNI registrado
                            esNuevoCliente = true;
                            //System.out.println("    Su primer préstamo usará la tasa estándar de 12.50%.");

                        } else {
                            System.out.println("❌ No se pudo registrar el cliente. El préstamo no se ha creado.");
                            pausarConsola();
                            return;
                        }
                    } else {
                        System.out.println("❌ Operación cancelada. El préstamo no se ha creado.");
                        pausarConsola();
                        return;
                    }
                }

                // Ajustar tasa de interés para préstamos hipotecarios
                double tasaOriginal = tasaInteres;
                if (tipoPrestamo == Prestamo.TipoPrestamo.HIPOTECARIO && esClientePreexistente) {
                    tasaInteres = 9.50;
                    if (tasaOriginal != tasaInteres) {
                        System.out.println("✅ Como cliente registrado, se aplicará una tasa preferencial de 9.50% (en lugar de 12.50%).");
                        cuotasPrevias = CalculadoraCuotas.calcularCuota("", monto, tasaInteres, numeroCuotas, LocalDate.now());
                        cuotaMensualEstimada = cuotasPrevias.get(0).getMontoCuota();
                        pausarConsola();
                    }
                } else if (tipoPrestamo == Prestamo.TipoPrestamo.HIPOTECARIO && esNuevoCliente) {
                    System.out.println("Nota: Como cliente recién registrado, se aplicará la tasa estándar de 12.50% para este primer préstamo.");
                    pausarConsola();
                }

                // Confirmación final solo para clientes preexistentes
                if (esClientePreexistente) {
                    while (true) {
                        System.out.println("\n=== Confirmación Final ===");
                        System.out.println("Cliente DNI: " + idCliente);
                        System.out.println("Tipo de Préstamo: " + tipoPrestamo);
                        System.out.println("Monto: " + formatoMoneda.format(monto));
                        System.out.println("Cuotas: " + numeroCuotas);
                        System.out.println("Tasa de Interés: " + String.format("%.2f%%", tasaInteres) +
                                (tipoPrestamo == Prestamo.TipoPrestamo.HIPOTECARIO ?
                                        " (tasa preferencial para clientes registrados)" : ""));
                        System.out.println("Cuota Mensual: " + formatoMoneda.format(cuotaMensualEstimada));

                        System.out.println("\nConfirmar:");
                        System.out.println("1. Sí");
                        System.out.println("2. No");

                        System.out.print("Seleccione una opción: ");
                        String input = scanner.nextLine().trim().toLowerCase();

                        // Validación de la opción "1" o "sí"
                        if (input.equals("1") || input.equals("sí") || input.equals("si")) {
                            break; // Confirmar y continuar con el registro
                        }
                        // Validación de la opción "2", "no" o "cancelar"
                        else if (input.equals("2") || input.equals("no") || input.equals("cancelar")) {
                            System.out.println("❌ Operación cancelada.");
                            pausarConsola();
                            return; // Salir al menú principal
                        }
                        // Si no es una opción válida
                        else {
                            System.out.println("❌ Opción inválida. Ingrese '1' para confirmar o '2' para cancelar.");
                        }
                    }
                }

                // Registrar el préstamo
                String idPrestamo = UUID.randomUUID().toString();
                LocalDate fechaCreacion = LocalDate.now();
                double saldoPendiente = monto;
                Prestamo.EstadoPrestamo estado = Prestamo.EstadoPrestamo.ACTIVO;
                Prestamo prestamo = new Prestamo(idPrestamo, idCliente, monto, tasaInteres, numeroCuotas, tipoPrestamo,
                        fechaCreacion, saldoPendiente, estado);
                boolean creado = prestamoDAO.crearPrestamo(prestamo);
                if (!creado) {
                    System.out.println("❌ No se pudo crear el préstamo. Verifique la conexión con la base de datos.");
                    pausarConsola();
                    return;
                }

                // Registrar las cuotas
                List<Cuota> cuotas = CalculadoraCuotas.calcularCuota(idPrestamo, monto, tasaInteres, numeroCuotas, fechaCreacion);
                for (Cuota cuota : cuotas) {
                    cuotaDAO.registrarCuota(cuota);
                }

                // Mostrar detalles del préstamo creado
                System.out.println("\n✅ Préstamo creado exitosamente con ID: " + idPrestamo);
                System.out.println("Detalles:");
                System.out.println("- Cliente DNI: " + idCliente);
                System.out.println("- Tipo de Préstamo: " + tipoPrestamo);
                System.out.println("- Monto: " + formatoMoneda.format(monto));
                System.out.println("- Cuota Mensual: " + formatoMoneda.format(cuotaMensualEstimada));
                System.out.println("- Total Cuotas: " + numeroCuotas);
                System.out.println("- Tasa de Interés: " + String.format("%.2f%%", tasaInteres));
                pausarConsola();
                return; // Salir al menú principal después de crear el préstamo

            } catch (ClienteDAOException e) {
                System.out.println(e.getMessage());
                pausarConsola();
                return; // Salir al menú principal en caso de error
            }
        }
    }

    private void mostrarPrestamosCliente() {
        try {
            String idCliente = ValidadorCliente.validarDNI(scanner);
            if (idCliente == null) {
                System.out.println("❌ Operación cancelada.");
                pausarConsola();
                return;
            }
            if (!clienteDAO.verificarExistenciaCliente(idCliente)) {
                System.out.println("❌ Cliente no encontrado.");
                pausarConsola();
                return;
            }
            List<Prestamo> prestamos = prestamoDAO.obtenerPrestamosPorCliente(idCliente);
            if (prestamos.isEmpty()) {
                System.out.println("❌ El cliente con DNI " + idCliente + " no tiene préstamos registrados.");
                pausarConsola();
                return;
            }

            // Seleccionar el préstamo
            String idPrestamo = seleccionarPrestamo(prestamos, idCliente);
            if (idPrestamo == null) {
                System.out.println("Operación cancelada.");
                pausarConsola();
                return;
            }

            mostrarMenuGestionPrestamo(idPrestamo);
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }

    private void mostrarCuota(String idPrestamo) {
        try {
            boolean existe = prestamoDAO.verificarExistenciaPrestamo(idPrestamo);
            if (!existe) {
                System.out.println("❌ Préstamo no encontrado.");
                pausarConsola();
                return;
            }
            List<Cuota> cuotas = cuotaDAO.obtenerCuotas(idPrestamo);
            if (cuotas.isEmpty()) {
                System.out.println("❌ No se encontraron cuotas para el préstamo.");
                pausarConsola();
                return;
            }
            System.out.println("\n=== Cuota del Préstamo ID " + idPrestamo + " ===");
            System.out.println("Cuota Mensual: " + formatoMoneda.format(cuotas.get(0).getMontoCuota()) +
                    " (Tasa: " + cuotas.get(0).getTasaAplicada() + "%)");
            System.out.println("Número total de cuotas: " + cuotas.size());
            pausarConsola();
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }


    private Map<Integer, Cuota> mostrarEstadoCuotas(String idPrestamo, List<Cuota> cuotas, List<Pago> pagos, boolean paraRegistrarPago, AtomicBoolean salir) throws ClienteDAOException {
        Map<Integer, Cuota> mapaOpciones = new HashMap<>();
        int cuotasPagadas = pagos.size();
        List<Cuota> cuotasEnMora = new ArrayList<>();
        List<Cuota> cuotasPendientesNoMora = new ArrayList<>();

        // Separar cuotas en mora y no en mora
        LocalDate hoy = LocalDate.now();
        for (Cuota cuota : cuotas) {
            boolean pagada = pagos.stream().anyMatch(p -> p.getNumeroCuota() == cuota.getNumeroCuota());
            if (!pagada) {
                if (cuota.getFechaVencimiento().isBefore(hoy)) {
                    cuotasEnMora.add(cuota);
                } else {
                    cuotasPendientesNoMora.add(cuota);
                }
            }
        }

        // Combinar todas las cuotas pendientes
        List<Cuota> todasCuotasPendientes = new ArrayList<>();
        todasCuotasPendientes.addAll(cuotasEnMora);
        todasCuotasPendientes.addAll(cuotasPendientesNoMora);

        // Mostrar estado general
        System.out.println("\n=== Estado de las cuotas para el préstamo ID " + idPrestamo + " ===");
        System.out.println("Cuotas pagadas: " + cuotasPagadas);
        System.out.println("Cuotas pendientes: " + todasCuotasPendientes.size());

        if (todasCuotasPendientes.isEmpty()) {
            System.out.println("✅ Todas las cuotas están pagadas.");
            pausarConsola();
            //salir.set(true); // Indicar que no hay más acciones posibles
            return new HashMap<>();
        }

        // Configuración de paginación
        int cuotasPorPagina = 5;
        int totalPaginas = (int) Math.ceil((double) todasCuotasPendientes.size() / cuotasPorPagina);
        int paginaActual = 1;

        while (true) {
            // Calcular índices para la página actual
            int inicio = (paginaActual - 1) * cuotasPorPagina;
            int fin = Math.min(inicio + cuotasPorPagina, todasCuotasPendientes.size());

            // Mostrar cuotas de la página actual
            System.out.println("\n--- Página " + paginaActual + " de " + totalPaginas + " ---");
            mapaOpciones.clear(); // Limpiar el mapa para la página actual
            int opcion = 1;
            for (int i = inicio; i < fin; i++) {
                Cuota cuota = todasCuotasPendientes.get(i);
                boolean enMora = cuota.getFechaVencimiento().isBefore(hoy);
                double penalidad = enMora ? cuota.getMontoCuota() * 0.05 : 0.0;

                System.out.println(opcion + ". Cuota " + cuota.getNumeroCuota() + ": " +
                        formatoMoneda.format(cuota.getMontoCuota()) +
                        " - Pendiente, vence el " + cuota.getFechaVencimiento() +
                        (enMora ? " (En mora, penalidad: " + formatoMoneda.format(penalidad) + ")" : ""));
                mapaOpciones.put(opcion, cuota);
                opcion++;
            }

            // Mostrar opciones según el contexto
            System.out.println("\nOpciones:");
            if (paraRegistrarPago) {
                System.out.println("1-" + mapaOpciones.size() + ". Seleccionar cuota para pagar");
            }
            System.out.print(paginaActual > 1 ? "A. Página anterior | " : "");
            System.out.print(paginaActual < totalPaginas ? "S. Página siguiente | " : "");
            System.out.println("0. Volver");

            // Solicitar opción al usuario
            System.out.print("Seleccione una opción: ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("0") || input.equals("volver")) {
                salir.set(true); // Indicar que el usuario quiere salir
                return new HashMap<>();
            }

            if (paraRegistrarPago && input.matches("\\d+")) {
                try {
                    int seleccionCuota = Integer.parseInt(input);
                    if (mapaOpciones.containsKey(seleccionCuota)) {
                        salir.set(false); // Continuar con la selección de cuota
                        Map<Integer, Cuota> resultado = new HashMap<>();
                        resultado.put(seleccionCuota, mapaOpciones.get(seleccionCuota)); // Retornar solo la cuota seleccionada
                        return resultado; // Retornar inmediatamente
                    } else {
                        System.out.println("❌ Opción inválida. Seleccione un número entre 1 y " + mapaOpciones.size() + ".");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("❌ Entrada inválida. Ingrese un número, 'A', 'S' o '0'.");
                    continue;
                }
            }

            if (input.equals("a") && paginaActual > 1) {
                paginaActual--;
            } else if (input.equals("s") && paginaActual < totalPaginas) {
                paginaActual++;
            } else {
                System.out.println("❌ Opción inválida. Use 'A', 'S', '0', o un número de cuota (1-" + mapaOpciones.size() + ") si está pagando.");
            }
        }
    }

    private void consultarPagosYCuotasPendientes(String idPrestamo) {
        try {
            // Validar existencia del préstamo
            boolean existe = prestamoDAO.verificarExistenciaPrestamo(idPrestamo);
            if (!existe) {
                System.out.println("❌ Préstamo no encontrado.");
                pausarConsola();
                return;
            }

            // Obtener cuotas y pagos
            List<Cuota> cuotas = cuotaDAO.obtenerCuotas(idPrestamo);
            if (cuotas.isEmpty()) {
                System.out.println("❌ No se encontraron cuotas para el préstamo.");
                pausarConsola();
                return;
            }
            List<Pago> pagos = pagoDAO.obtenerPagos(idPrestamo);

            // Mostrar estado de las cuotas
            AtomicBoolean salir = new AtomicBoolean(false);
            mostrarEstadoCuotas(idPrestamo, cuotas, pagos, false, salir);
            // No se necesita manejar 'salir' aca, ya que no hay selección de cuota
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }

    private void registrarPago(String idPrestamo) {
        try {
            // Validar existencia del préstamo
            boolean existe = prestamoDAO.verificarExistenciaPrestamo(idPrestamo);
            if (!existe) {
                System.out.println("❌ Préstamo no encontrado.");
                pausarConsola();
                return;
            }

            while (true) {
                // Obtener cuotas y pagos
                List<Cuota> cuotas = cuotaDAO.obtenerCuotas(idPrestamo);
                if (cuotas.isEmpty()) {
                    System.out.println("❌ No se encontraron cuotas para el préstamo.");
                    pausarConsola();
                    return;
                }
                List<Pago> pagos = pagoDAO.obtenerPagos(idPrestamo);

                // Mostrar estado de las cuotas y obtener la cuota seleccionada
                AtomicBoolean salir = new AtomicBoolean(false);
                Map<Integer, Cuota> mapaOpciones = mostrarEstadoCuotas(idPrestamo, cuotas, pagos, true, salir);
//                if (salir.get() || mapaOpciones.isEmpty()) {
//                    System.out.println("❌ Operación cancelada.");
//                    pausarConsola();
//                    return; // Salir si el usuario eligió "Volver" o no hay cuotas
//                }

                if (mapaOpciones.isEmpty() && !salir.get()) {
                    // Caso: todas las cuotas están pagadas
                    return; // Salir sin mensaje de cancelación
                }
                if (salir.get()) {
                    System.out.println("↩️ Volviendo al menú anterior...");
                    pausarConsola();
                    return; // Salir si el usuario eligió "Volver"
                }

                // Obtener la cuota seleccionada (debería haber solo una en el mapa)
                if (mapaOpciones.size() != 1) {
                    System.out.println("❌ Error: Se esperaba una sola cuota seleccionada.");
                    pausarConsola();
                    continue;
                }
                Map.Entry<Integer, Cuota> entry = mapaOpciones.entrySet().iterator().next();
                Cuota cuota = entry.getValue();
                int numeroCuota = cuota.getNumeroCuota();

                // Calcular penalidad
                double penalidad = 0.0;
                LocalDate hoy = LocalDate.now();
                boolean enMora = cuota.getFechaVencimiento().isBefore(hoy);
                if (enMora) {
                    penalidad = pagoDAO.obtenerPenalidad(idPrestamo, numeroCuota);
                    if (penalidad == 0.0) {
                        penalidad = cuota.getMontoCuota() * 0.05;
                        try (Connection conn = ConexionDB.conectar();
                             PreparedStatement stmt = conn.prepareStatement("INSERT INTO penalidades (idPrestamo, numeroCuota, montoPenalidad, fechaAplicacion) VALUES (?, ?, ?, ?)")) {
                            stmt.setString(1, idPrestamo);
                            stmt.setInt(2, numeroCuota);
                            stmt.setDouble(3, penalidad);
                            stmt.setObject(4, LocalDate.now());
                            stmt.executeUpdate();
                        }
                    }
                }

                // Confirmar pago
                double montoTotal = cuota.getMontoCuota() + penalidad;
                String mensajeConfirmacion = String.format("¿Confirma el pago de la cuota %d por $%s? (si/no): ",
                        numeroCuota, formatoMoneda.format(montoTotal));
                if (!confirmarOperacion(mensajeConfirmacion)) {
                    System.out.println("❌ Operación cancelada.");
                    pausarConsola();
                    continue;
                }

                // Asignar la fecha de pago como la fecha actual automáticamente
                LocalDate fechaPago = LocalDate.now();

                // Registrar el pago
                Pago pago = new Pago(idPrestamo, numeroCuota, montoTotal, fechaPago);
                boolean registrado = pagoDAO.registrarPago(pago, penalidad);
                if (registrado) {
                    System.out.println("✅ Pago de la cuota " + numeroCuota + " registrado exitosamente.");
                } else {
                    System.out.println("❌ No se pudo registrar el pago.");
                    pausarConsola();
                    continue;
                }

                // Verificar si hay más cuotas pendientes
                pagos = pagoDAO.obtenerPagos(idPrestamo); // Actualizar pagos
                if (pagos.size() >= cuotas.size()) {
                    System.out.println("✅ Todas las cuotas están pagadas.");
                    pausarConsola();
                    return;
                }

                // Preguntar si desea pagar otra cuota
                if (!confirmarOperacion("¿Desea pagar otra cuota? (si/no): ")) {
                    System.out.println("Operación finalizada.");
                    pausarConsola();
                    return;
                }
            }
        } catch (ClienteDAOException | SQLException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }

    private void consultarEstadoPrestamo(String idPrestamo) {
        try {
            boolean existe = prestamoDAO.verificarExistenciaPrestamo(idPrestamo);
            if (!existe) {
                System.out.println("❌ Préstamo no encontrado.");
                pausarConsola();
                return;
            }
            prestamoDAO.consultarEstadoPrestamo(idPrestamo, cuotaDAO, pagoDAO);
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }

    private void consultarHistorialPagos(String idPrestamo) {
        try {
            boolean existe = prestamoDAO.verificarExistenciaPrestamo(idPrestamo);
            if (!existe) {
                System.out.println("❌ Préstamo no encontrado.");
                pausarConsola();
                return;
            }
            prestamoDAO.mostrarHistorialPagos(idPrestamo, pagoDAO, formatoMoneda);
            pausarConsola();
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }

    private void consultarClientesConPrestamosActivos() {
        try {
            reporteDAO.obtenerClientesConPrestamosActivos();
            pausarConsola();
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }

    private void consultarClientesEnMora() {
        try {
            reporteDAO.obtenerClientesEnMora();
            pausarConsola();
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }

    private void proyectarIngresos() {
        try {
            // Validar el DNI del cliente
            String idCliente = ValidadorCliente.validarDNI(scanner);
            if (idCliente == null) {
                System.out.println("❌ Operación cancelada.");
                pausarConsola();
                return;
            }

            // Verificar existencia del cliente
            if (!clienteDAO.verificarExistenciaCliente(idCliente)) {
                System.out.println("❌ Cliente con DNI " + idCliente + " no encontrado.");
                pausarConsola();
                return;
            }

            // Obtener los préstamos del cliente
            List<Prestamo> prestamos = prestamoDAO.obtenerPrestamosPorCliente(idCliente);
            if (prestamos.isEmpty()) {
                System.out.println("❌ El cliente con DNI " + idCliente + " no tiene préstamos registrados.");
                pausarConsola();
                return;
            }

            // Seleccionar el préstamo
            String idPrestamo = seleccionarPrestamo(prestamos, idCliente);
            if (idPrestamo == null) {
                System.out.println("❌ Operación cancelada.");
                pausarConsola();
                return;
            }

            // Solicitar el número de meses para la proyección
            Integer meses = null;
            while (meses == null) {
                System.out.print("Ingrese el número de meses para la proyección (1-60) o 'cancelar': ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("cancelar")) {
                    System.out.println("❌ Operación cancelada.");
                    pausarConsola();
                    return;
                }
                try {
                    int valor = Integer.parseInt(input);
                    if (valor >= 1 && valor <= 60) {
                        meses = valor;
                    } else {
                        System.out.println("❌ Número de meses debe estar entre 1 y 60.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("❌ Entrada inválida. Debe ser un número.");
                }
            }

            // Realizar la proyección de ingresos
            reporteDAO.proyectarIngresos(idPrestamo, meses);
            pausarConsola();
        } catch (ClienteDAOException e) {
            System.out.println(e.getMessage());
            pausarConsola();
        }
    }


    private void exportarClientes() throws ClienteDAOException {
        // Preguntar si desea exportar todos los clientes
        if (confirmarOperacion("¿Desea exportar todos los clientes? (sí/no): ")) {
            if (!confirmarOperacion("¿Confirma la exportación de datos de todos los clientes? (sí/no): ")) {
                System.out.println("❌ Operación cancelada.");
                pausarConsola();
                return;
            }
            String archivo = reporteDAO.exportarDatosClientes(null);
            if (archivo != null) {
                System.out.println("✅ Datos de clientes exportados a: " + archivo);
                pausarConsola();
            }
            return;
        }

        // Solicitar DNI directamente con validarDNI
        String idCliente = ValidadorCliente.validarDNI(scanner);

        // Manejar cancelación
        if (idCliente == null) {
            System.out.println("❌ Operación cancelada.");
            pausarConsola();
            return;
        }

        // Verificar existencia del cliente
        if (!clienteDAO.verificarExistenciaCliente(idCliente)) {
            System.out.println("❌ Cliente con DNI " + idCliente + " no encontrado.");
            pausarConsola();
            return;
        }

        // Confirmar exportación
        if (!confirmarOperacion("¿Confirma la exportación de datos del cliente con DNI " + idCliente + "? (sí/no): ")) {
            System.out.println("❌ Operación cancelada.");
            pausarConsola();
            return;
        }

        // Exportar datos del cliente
        String archivo = reporteDAO.exportarDatosClientes(idCliente);
        if (archivo != null) {
            System.out.println("✅ Datos del cliente (DNI:" + idCliente +") exportados a: " + archivo);
            pausarConsola();
        }
    }

    private void exportarPrestamos() throws ClienteDAOException {
        // Preguntar si desea exportar todos los préstamos
        if (confirmarOperacion("¿Desea exportar todos los préstamos? (sí/no): ")) {
            if (!confirmarOperacion("¿Confirma la exportación de datos de todos los préstamos? (sí/no): ")) {
                System.out.println("❌ Operación cancelada.");
                pausarConsola();
                return;
            }
            String archivo = reporteDAO.exportarDatosPrestamos(null);
            if (archivo != null) {
                System.out.println("✅ Datos de préstamos exportados a: " + archivo);
                pausarConsola();
            }
            return;
        }

        // Solicitar DNI directamente con validarDNI
        String idCliente = ValidadorCliente.validarDNI(scanner);

        // Manejar cancelación
        if (idCliente == null) {
            System.out.println("❌ Operación cancelada.");
            pausarConsola();
            return;
        }

        // Verificar existencia del cliente
        if (!clienteDAO.verificarExistenciaCliente(idCliente)) {
            System.out.println("❌ Cliente con DNI " + idCliente + " no encontrado.");
            pausarConsola();
            return;
        }

        // Verificar si el cliente tiene préstamos
        List<Prestamo> prestamos = prestamoDAO.obtenerPrestamosPorCliente(idCliente);
        if (prestamos.isEmpty()) {
            System.out.println("❌ El cliente con DNI " + idCliente + " no tiene préstamos registrados.");
            pausarConsola();
            return;
        }

        // Confirmar exportación
        if (!confirmarOperacion("¿Confirma la exportación de datos de los préstamos del cliente con DNI " + idCliente + "? (sí/no): ")) {
            System.out.println("❌ Operación cancelada.");
            pausarConsola();
            return;
        }

        // Exportar datos de los préstamos
        String archivo = reporteDAO.exportarDatosPrestamos(idCliente);
        if (archivo != null) {
            System.out.println("✅ Datos de préstamos - cliente (DNI:" + idCliente +") exportados a: " + archivo);
            pausarConsola();
        }
    }

    private void exportarClientesEnMora() throws ClienteDAOException {
        // Preguntar si desea exportar todos los clientes en mora
        if (confirmarOperacion("¿Desea exportar todos los clientes en mora? (sí/no): ")) {
            if (!confirmarOperacion("¿Confirma la exportación de datos de todos los clientes en mora? (sí/no): ")) {
                System.out.println("Operación cancelada.");
                pausarConsola();
                return;
            }
            String archivo = reporteDAO.exportarClientesEnMora(null);
            if (archivo != null) {
                System.out.println("✅ Datos de clientes en mora exportados a: " + archivo);
                pausarConsola();
            }
            return;
        }

        // Solicitar DNI directamente con validarDNI
        String idCliente = ValidadorCliente.validarDNI(scanner);

        // Manejar cancelación
        if (idCliente == null) {
            System.out.println("❌ Operación cancelada.");
            pausarConsola();
            return;
        }

        // Verificar existencia del cliente
        if (!clienteDAO.verificarExistenciaCliente(idCliente)) {
            System.out.println("❌ Cliente con DNI " + idCliente + " no encontrado.");
            pausarConsola();
            return;
        }

        // Confirmar exportación
        if (!confirmarOperacion("¿Confirma la exportación de datos del cliente con DNI " + idCliente + " en mora? (sí/no): ")) {
            System.out.println("❌ Operación cancelada.");
            pausarConsola();
            return;
        }

        // Exportar datos del cliente en mora
        String archivo = reporteDAO.exportarClientesEnMora(idCliente);
        if (archivo != null) {
            System.out.println("✅ Datos del cliente (DNI:" + idCliente +") en mora exportados a: " + archivo);
            pausarConsola();
        }
    }

    private void exportarPagos() throws ClienteDAOException {
        // Validar el DNI del cliente
        String idCliente = ValidadorCliente.validarDNI(scanner);
        if (idCliente == null) {
            System.out.println("❌ Operación cancelada.");
            pausarConsola();
            return;
        }

        // Verificar existencia del cliente
        if (!clienteDAO.verificarExistenciaCliente(idCliente)) {
            System.out.println("❌ Cliente con DNI " + idCliente + " no encontrado.");
            pausarConsola();
            return;
        }

        // Obtener los préstamos del cliente
        List<Prestamo> prestamos = prestamoDAO.obtenerPrestamosPorCliente(idCliente);
        if (prestamos.isEmpty()) {
            System.out.println("❌ El cliente con DNI " + idCliente + " no tiene préstamos registrados.");
            pausarConsola();
            return;
        }

        // Seleccionar el préstamo
        String idPrestamo = seleccionarPrestamo(prestamos, idCliente);
        if (idPrestamo == null) {
            System.out.println("❌ Operación cancelada.");
            pausarConsola();
            return;
        }

        // Confirmar exportación
        if (!confirmarOperacion("¿Confirma la exportación del historial de pagos del préstamo con ID " + idPrestamo + "? (sí/no): ")) {
            System.out.println("❌ Operación cancelada.");
            pausarConsola();
            return;
        }

        // Exportar historial de pagos
        String archivo = reporteDAO.exportarHistorialPagos(idPrestamo);
        if (archivo != null) {
            System.out.println("✅ Historial de pagos exportado a: " + archivo);
            pausarConsola();
        }
    }

    private String seleccionarPrestamo(List<Prestamo> prestamos, String idCliente) {
        String idPrestamo = null;
        while (idPrestamo == null) {
            System.out.println("\nPréstamos registrados para el cliente con DNI " + idCliente + ":");
            for (int i = 0; i < prestamos.size(); i++) {
                Prestamo p = prestamos.get(i);
                System.out.println((i + 1) + ". ID Préstamo: " + p.getIdPrestamo() +
                        " | Monto: " + formatoMoneda.format(p.getMonto()) +
                        " | Tipo: " + p.getTipoPrestamo() +
                        " | Estado: " + p.getEstado());
            }
            System.out.print("Seleccione el número del préstamo (1-" + prestamos.size() + ") o 'cancelar': ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("cancelar")) {
                return null; // Indica cancelación
            }
            try {
                int indice = Integer.parseInt(input) - 1;
                if (indice >= 0 && indice < prestamos.size()) {
                    idPrestamo = prestamos.get(indice).getIdPrestamo();
                } else {
                    System.out.println("❌ Selección inválida. Ingrese un número entre 1 y " + prestamos.size() + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Entrada inválida. Ingrese un número.");
            }
        }
        return idPrestamo;
    }

    private void pausarConsola() {
        System.out.print("\nPresione Enter para continuar...");
        // Consumir cualquier entrada residual hasta un Enter limpio
        while (scanner.hasNextLine()) {
            String residual = scanner.nextLine().trim();
            if (residual.isEmpty()) {
                break; // Enter puro detectado
            }
            System.out.print("Por favor, presione solo Enter para continuar...");
        }
    }

    public static void main(String[] args) {
        Menu menu = new Menu();
        menu.iniciar();
    }
}