package com.financierasolandino.dao;

import com.financierasolandino.db.ConexionDB;
import com.financierasolandino.util.Utilidad;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class ReporteDAO {
    private static final String SQL_CLIENTES_CON_PRESTAMOS_ACTIVOS =
            "SELECT c.idCliente, c.nombre, p.idPrestamo, p.saldo_pendiente, p.estado " +
                    "FROM clientes c " +
                    "JOIN prestamos p ON c.idCliente = p.idCliente " +
                    "WHERE p.estado IN ('ACTIVO', 'EN_MORA') " +
                    "ORDER BY c.idCliente, p.idPrestamo";

    private static final String SQL_PROYECTAR_INGRESOS =
            "SELECT cu.idPrestamo, cu.numeroCuota, cu.montoCuota, cu.fechaVencimiento, " +
                    "COALESCE(pen.montoPenalidad, 0) as montoPenalidad " +
                    "FROM cuotas cu " +
                    "LEFT JOIN pagos pg ON cu.idPrestamo = pg.idPrestamo AND cu.numeroCuota = pg.numeroCuota " +
                    "LEFT JOIN penalidades pen ON cu.idPrestamo = pen.idPrestamo AND cu.numeroCuota = pen.numeroCuota " +
                    "WHERE cu.idPrestamo = ? AND pg.idPrestamo IS NULL AND cu.fechaVencimiento <= ?";

    private static final String SQL_CLIENTES_EN_MORA =
            "SELECT c.idCliente, c.nombre, p.idPrestamo, cu.numeroCuota, cu.montoCuota, pen.montoPenalidad, cu.fechaVencimiento " +
                    "FROM clientes c " +
                    "JOIN prestamos p ON c.idCliente = p.idCliente " +
                    "JOIN cuotas cu ON p.idPrestamo = cu.idPrestamo " +
                    "LEFT JOIN pagos pg ON cu.idPrestamo = pg.idPrestamo AND cu.numeroCuota = pg.numeroCuota " +
                    "LEFT JOIN penalidades pen ON cu.idPrestamo = pen.idPrestamo AND cu.numeroCuota = pen.numeroCuota " +
                    "WHERE p.estado = 'EN_MORA' AND pg.idPrestamo IS NULL AND cu.fechaVencimiento < ? " +
                    "ORDER BY c.idCliente, p.idPrestamo, cu.numeroCuota";

    //private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
    private final NumberFormat formatoMoneda = Utilidad.getArgentinaNumberFormat();
    /**
     * Obtiene los clientes con préstamos activos o en mora, mostrando sus préstamos y saldos pendientes.
     */
    public void obtenerClientesConPrestamosActivos() throws ClienteDAOException {
        List<String> resultados = new ArrayList<>();
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_CLIENTES_CON_PRESTAMOS_ACTIVOS);
             ResultSet rs = stmt.executeQuery()) {

            String clienteActual = "";
            while (rs.next()) {
                String idCliente = rs.getString("idCliente");
                String nombre = rs.getString("nombre");
                String idPrestamo = rs.getString("idPrestamo");
                double saldoPendiente = rs.getDouble("saldo_pendiente");
                String estado = rs.getString("estado");

                // Verificar si el cliente ya ha sido procesado
                if (!idCliente.equals(clienteActual)) {
                    // Si no es el mismo cliente, agregar el nombre del cliente
                    resultados.add("\nCliente: " + nombre + " (DNI: " + idCliente + ")");
                    clienteActual = idCliente;
                }

                // Traducir el estado del préstamo a algo más legible
                String estadoDescripcion = "";
                if (estado.equalsIgnoreCase("ACTIVO")) {
                    estadoDescripcion = "Activo - Pagos al día";
                } else if (estado.equalsIgnoreCase("EN_MORA")) {
                    estadoDescripcion = "En mora - Pagos atrasados";
                }

                // Formatear cada línea para que todo esté alineado
                String idPrestamoFormateado = String.format("%-36s", idPrestamo); // ID de préstamo con 36 caracteres de ancho
                String saldoPendienteFormateado = String.format("%-15s", formatoMoneda.format(saldoPendiente)); // Saldo pendiente con 20 caracteres de ancho
                String estadoFormateado = String.format("%-30s", estadoDescripcion); // Estado con 30 caracteres de ancho

                // Agregar la información del préstamo con formato
                resultados.add(String.format("  Préstamo ID: %s | Saldo Pendiente: %s | Estado: %s",
                        idPrestamoFormateado, saldoPendienteFormateado, estadoFormateado));
            }

            // Si no se encontraron resultados, mostrar mensaje adecuado
            if (resultados.isEmpty()) {
                System.out.println("No hay clientes con préstamos activos o en mora.");
            } else {
                // Mostrar los resultados
                System.out.println("\n=== Clientes con Préstamos Activos o en Mora ===");
                resultados.forEach(System.out::println);
            }

        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al obtener clientes con préstamos activos", e);
        }
    }

    /**
     * Proyecta los ingresos por cuotas pendientes de un préstamo específico en un período de tiempo,
     * incluyendo penalidades por cuotas en mora, con una salida resumida.
     * @param idPrestamo ID del préstamo a proyectar.
     * @param meses Número de meses a proyectar (por ejemplo, 12 para un año).
     */
    public void proyectarIngresos(String idPrestamo, int meses) throws ClienteDAOException {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaFin = hoy.plusMonths(meses);
        double totalCuotasMora = 0.0;
        double totalPenalidades = 0.0;
        double totalCuotasPendientes = 0.0;
        int cuotasEnMora = 0;
        int cuotasPendientes = 0;
        int totalCuotasPrestamo = 0;
        int cuotasPagadas = 0;
        LocalDate fechaCreacion = null;
        LocalDate ultimaFecha = null;

        // Formateador de fechas
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Obtener información del préstamo
        String sqlPrestamo = "SELECT numeroCuotas, fecha_creacion FROM prestamos WHERE idPrestamo = ?";
        String sqlUltimaCuota = "SELECT MAX(fechaVencimiento) as ultimaFecha FROM cuotas WHERE idPrestamo = ?";
        String sqlCuotasPagadas = "SELECT COUNT(*) as cuotasPagadas FROM pagos WHERE idPrestamo = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmtPrestamo = conn.prepareStatement(sqlPrestamo);
             PreparedStatement stmtUltima = conn.prepareStatement(sqlUltimaCuota);
             PreparedStatement stmtPagadas = conn.prepareStatement(sqlCuotasPagadas)) {

            // Obtener número de cuotas y fecha de creación
            stmtPrestamo.setString(1, idPrestamo);
            ResultSet rsPrestamo = stmtPrestamo.executeQuery();
            if (rsPrestamo.next()) {
                totalCuotasPrestamo = rsPrestamo.getInt("numeroCuotas");
                fechaCreacion = rsPrestamo.getObject("fecha_creacion", LocalDate.class);
            } else {
                throw new ClienteDAOException("Préstamo ID " + idPrestamo + " no encontrado.");
            }

            // Obtener la fecha de vencimiento de la última cuota
            stmtUltima.setString(1, idPrestamo);
            ResultSet rsUltima = stmtUltima.executeQuery();
            if (rsUltima.next()) {
                ultimaFecha = rsUltima.getObject("ultimaFecha", LocalDate.class);
            }

            // Obtener número de cuotas pagadas
            stmtPagadas.setString(1, idPrestamo);
            ResultSet rsPagadas = stmtPagadas.executeQuery();
            if (rsPagadas.next()) {
                cuotasPagadas = rsPagadas.getInt("cuotasPagadas");
            }

            // Consultar cuotas
            try (PreparedStatement stmt = conn.prepareStatement(SQL_PROYECTAR_INGRESOS)) {
                stmt.setString(1, idPrestamo);
                stmt.setDate(2, java.sql.Date.valueOf(fechaFin));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    int numeroCuota = rs.getInt("numeroCuota");
                    double montoCuota = rs.getDouble("montoCuota");
                    LocalDate fechaVencimiento = rs.getObject("fechaVencimiento", LocalDate.class);
                    double montoPenalidad = rs.getDouble("montoPenalidad");

                    // Si la cuota está en mora y no tiene penalidad registrada, calcularla
                    if (fechaVencimiento.isBefore(hoy) && montoPenalidad == 0) {
                        montoPenalidad = montoCuota * 0.05; // 5% de penalidad
                        // Registrar la penalidad en la base de datos
                        try (PreparedStatement penalidadStmt = conn.prepareStatement(
                                "INSERT INTO penalidades (idPrestamo, numeroCuota, montoPenalidad, fechaAplicacion) VALUES (?, ?, ?, ?)")) {
                            penalidadStmt.setString(1, idPrestamo);
                            penalidadStmt.setInt(2, numeroCuota);
                            penalidadStmt.setDouble(3, montoPenalidad);
                            penalidadStmt.setDate(4, java.sql.Date.valueOf(hoy));
                            penalidadStmt.executeUpdate();
                        }
                    }

                    if (fechaVencimiento.isBefore(hoy)) {
                        // Cuotas en mora
                        cuotasEnMora++;
                        totalCuotasMora += montoCuota;
                        totalPenalidades += montoPenalidad;
                    } else {
                        // Cuotas pendientes (no en mora)
                        cuotasPendientes++;
                        totalCuotasPendientes += montoCuota;
                    }
                }

                if (cuotasEnMora == 0 && cuotasPendientes == 0) {
                    System.out.println("No hay cuotas pendientes para el préstamo ID " + idPrestamo + " en el período especificado.");
                } else {
                    System.out.println("\n=== Proyección de Ingresos para Préstamo ID " + idPrestamo + " ===");
                    System.out.println("Período de proyección: " + hoy.format(formatter) + " a " + fechaFin.format(formatter) + " (" + meses + " meses)");
                    System.out.println("Préstamo con " + totalCuotasPrestamo + " cuotas en total | cuotas pagadas: " + cuotasPagadas +
                            " | fecha creación: " + (fechaCreacion != null ? fechaCreacion.format(formatter) : "N/A") +
                            " | última cuota vence el: " + (ultimaFecha != null ? ultimaFecha.format(formatter) : "N/A"));
                    if (meses > totalCuotasPrestamo) {
                        System.out.println("Nota: La proyección incluye todas las cuotas pendientes del préstamo, ya que el período solicitado (" +
                                meses + " meses) excede la duración del préstamo (" + totalCuotasPrestamo + " meses).");
                    }
                    System.out.println();
                    if (cuotasEnMora > 0) {
                        System.out.println("Cuotas pendientes (en mora): " + cuotasEnMora + " cuota(s) | Penalidad por mora: " +
                                formatoMoneda.format(totalPenalidades) + " | Total sin penalidad: " +
                                formatoMoneda.format(totalCuotasMora) + " | Total con penalidades: " +
                                formatoMoneda.format(totalCuotasMora + totalPenalidades));
                    } else {
                        System.out.println("No hay cuotas en mora.");
                    }
                    if (cuotasPendientes > 0) {
                        System.out.println("Cuotas pendientes (no en mora): " + cuotasPendientes + " cuota(s) | Total: " +
                                formatoMoneda.format(totalCuotasPendientes));
                    } else {
                        System.out.println("No hay cuotas pendientes futuras.");
                    }
                    double totalIngresos = totalCuotasMora + totalCuotasPendientes + totalPenalidades;
                    System.out.println("\nIngresos proyectados totales: " + formatoMoneda.format(totalIngresos));
                }
            }
        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al proyectar ingresos para el préstamo ID " + idPrestamo, e);
        }
    }

    public void obtenerClientesEnMora() throws ClienteDAOException {
        List<String> resultados = new ArrayList<>();
        double totalPenalidadesGlobal = 0.0;

        String SQL_TOTAL_PENALIDADES_PRESTAMO =
                "SELECT SUM(montoPenalidad) AS totalPenalidad " +
                        "FROM penalidades WHERE idPrestamo = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_CLIENTES_EN_MORA);
             PreparedStatement penalidadStmt = conn.prepareStatement(SQL_TOTAL_PENALIDADES_PRESTAMO)) {

            stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
            ResultSet rs = stmt.executeQuery();

            String clienteActual = "";
            String prestamoActual = "";
            String idPrestamoAnterior = null;
            double totalPenalidadPrestamo = 0.0;

            while (rs.next()) {
                String idCliente = rs.getString("idCliente");
                String nombre = rs.getString("nombre");
                String idPrestamo = rs.getString("idPrestamo");
                int numeroCuota = rs.getInt("numeroCuota");
                double montoCuota = rs.getDouble("montoCuota");
                double montoPenalidad = rs.getDouble("montoPenalidad");
                LocalDate fechaVencimiento = rs.getObject("fechaVencimiento", LocalDate.class);

                if (!idCliente.equals(clienteActual)) {
                    resultados.add("\nCliente: " + nombre + " (DNI: " + idCliente + ")");
                    clienteActual = idCliente;
                    prestamoActual = "";
                }

                if (!idPrestamo.equals(prestamoActual)) {
                    // Consultar penalidades acumuladas para este préstamo
                    penalidadStmt.setString(1, idPrestamo);
                    try (ResultSet penalidadRs = penalidadStmt.executeQuery()) {
                        if (penalidadRs.next()) {
                            totalPenalidadPrestamo = penalidadRs.getDouble("totalPenalidad");
                            totalPenalidadesGlobal += totalPenalidadPrestamo;
                        } else {
                            totalPenalidadPrestamo = 0;
                        }
                    }

                    resultados.add("  Préstamo ID: " + idPrestamo);
                    resultados.add("    Penalidad acumulada: " + formatoMoneda.format(totalPenalidadPrestamo));
                    prestamoActual = idPrestamo;
                }

                resultados.add("    Cuota " + numeroCuota + ": " + formatoMoneda.format(montoCuota) +
                        ", Vencimiento: " + fechaVencimiento +
                        (montoPenalidad > 0 ? ", Penalidad: " + formatoMoneda.format(montoPenalidad) : ""));
            }

            if (resultados.isEmpty()) {
                System.out.println("No hay clientes con préstamos en mora.");
            } else {
                System.out.println("\n=== Clientes con Préstamos en Mora ===");
                resultados.forEach(System.out::println);
                System.out.println("\nSuma total penalidades: " + formatoMoneda.format(totalPenalidadesGlobal));
            }

        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al obtener clientes en mora", e);
        }
    }

    /**
     * Exporta los datos de los clientes a un archivo CSV.
     * @param idCliente ID del cliente para filtrar (opcional, puede ser null para todos los clientes).
     * @return Nombre del archivo generado, o null si no hay datos.
     * @throws ClienteDAOException Si ocurre un error durante la exportación.
     */
    public String exportarDatosClientes(String idCliente) throws ClienteDAOException {
        String sql = idCliente == null ?
                "SELECT idCliente, nombre, direccion, telefono, correoElectronico FROM clientes" :
                "SELECT idCliente, nombre, direccion, telefono, correoElectronico FROM clientes WHERE idCliente = ?";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nombreArchivo = "exportacion/clientes_" + timestamp + ".csv";

        // Crear directorio exportacion si no existe
        crearDirectorioExportacion();

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

            if (idCliente != null) {
                stmt.setString(1, idCliente);
            }

            ResultSet rs = stmt.executeQuery();

            // Verificar si hay datos
            if (!rs.next()) {
                System.out.println("❌ No se encontraron clientes para exportar" + (idCliente != null ? " con DNI " + idCliente : "") + ".");
                return null;
            }

            // Volver al inicio del ResultSet
            rs.beforeFirst();

            try (FileWriter writer = new FileWriter(nombreArchivo, StandardCharsets.UTF_8)) {
                // Agregar BOM para UTF-8
                writer.write('\uFEFF');

                // Escribir encabezados
                writer.write("ID Cliente,Nombre,Dirección,Teléfono,Correo Electrónico\n");

                // Escribir datos
                while (rs.next()) {
                    String id = rs.getString("idCliente");
                    String nombre = rs.getString("nombre");
                    String direccion = rs.getString("direccion");
                    String telefono = rs.getString("telefono");
                    String correo = rs.getString("correoElectronico");
                    writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                            escapeCsv(id), escapeCsv(nombre), escapeCsv(direccion), escapeCsv(telefono), escapeCsv(correo)));
                }

                //System.out.println("Datos de clientes exportados a: " + nombreArchivo);
                return nombreArchivo;
            }
        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al exportar datos de clientes", e);
        } catch (IOException e) {
            throw new ClienteDAOException("❌ Error al escribir el archivo " + nombreArchivo, e);
        }
    }

    /**
     * Exporta los datos de los préstamos a un archivo CSV, incluyendo cuotas pagadas, pendientes y en mora.
     * @param idCliente ID del cliente para filtrar (opcional, puede ser null para todos los préstamos).
     * @return Nombre del archivo generado, o null si no hay datos.
     * @throws ClienteDAOException Si ocurre un error durante la exportación.
     */
    public String exportarDatosPrestamos(String idCliente) throws ClienteDAOException {
        String sql = idCliente == null ?
                "SELECT idPrestamo, idCliente, monto, tasaInteres, numeroCuotas, tipoPrestamo, fecha_creacion, saldo_pendiente, estado FROM prestamos" :
                "SELECT idPrestamo, idCliente, monto, tasaInteres, numeroCuotas, tipoPrestamo, fecha_creacion, saldo_pendiente, estado FROM prestamos WHERE idCliente = ?";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nombreArchivo = "exportacion/prestamos_" + timestamp + ".csv";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Crear directorio exportacion si no existe
        crearDirectorioExportacion();

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

            if (idCliente != null) {
                stmt.setString(1, idCliente);
            }

            ResultSet rs = stmt.executeQuery();

            // Verificar si hay datos
            if (!rs.next()) {
                System.out.println("❌ No se encontraron préstamos para exportar" + (idCliente != null ? " para el cliente con DNI " + idCliente : "") + ".");
                return null;
            }

            // Volver al inicio del ResultSet
            rs.beforeFirst();

            try (FileWriter writer = new FileWriter(nombreArchivo, StandardCharsets.UTF_8)) {
                // Agregar BOM para UTF-8
                writer.write('\uFEFF');

                // Escribir encabezados
                writer.write("ID Préstamo,ID Cliente,Monto,Tasa Interés,Número Cuotas,Tipo Préstamo,Fecha Creación,Saldo Pendiente,Estado,Cuotas Pagadas,Cuotas Pendientes,Cuotas en Mora\n");

                // Escribir datos
                while (rs.next()) {
                    String idPrestamo = rs.getString("idPrestamo");
                    String idCli = rs.getString("idCliente");
                    double monto = rs.getDouble("monto");
                    double tasaInteres = rs.getDouble("tasaInteres");
                    int numeroCuotas = rs.getInt("numeroCuotas");
                    String tipoPrestamo = rs.getString("tipoPrestamo");
                    LocalDate fechaCreacion = rs.getObject("fecha_creacion", LocalDate.class);
                    double saldoPendiente = rs.getDouble("saldo_pendiente");
                    String estado = rs.getString("estado");

                    // Calcular cuotas pagadas
                    int cuotasPagadas = getCuotasPagadas(conn, idPrestamo);

                    // Calcular cuotas en mora
                    int cuotasEnMora = getCuotasEnMora(conn, idPrestamo);

                    // Calcular cuotas pendientes (no pagadas y no vencidas)
                    int cuotasPendientes = numeroCuotas - cuotasPagadas - cuotasEnMora;

                    writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%.2f\",\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\",\"%d\",\"%d\"\n",
                            escapeCsv(idPrestamo), escapeCsv(idCli), formatoMoneda.format(monto), tasaInteres,
                            numeroCuotas, escapeCsv(tipoPrestamo),
                            fechaCreacion != null ? fechaCreacion.format(formatter) : "N/A",
                            formatoMoneda.format(saldoPendiente), escapeCsv(estado),
                            cuotasPagadas, cuotasPendientes, cuotasEnMora));
                }

                //System.out.println("Datos de préstamos exportados a: " + nombreArchivo);
                return nombreArchivo;
            }
        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al exportar datos de préstamos", e);
        } catch (IOException e) {
            throw new ClienteDAOException("❌ Error al escribir el archivo " + nombreArchivo, e);
        }
    }

    /**
     * Exporta el historial de pagos a un archivo CSV, incluyendo penalidades.
     * @param idPrestamo ID del préstamo para filtrar (opcional, puede ser null para todos los pagos).
     * @return Nombre del archivo generado, o null si no hay datos.
     * @throws ClienteDAOException Si ocurre un error durante la exportación.
     */
    public String exportarHistorialPagos(String idPrestamo) throws ClienteDAOException {
        String sql = idPrestamo == null ?
                "SELECT p.idPrestamo, p.numeroCuota, p.montoPagado, p.fechaPago, COALESCE(pen.montoPenalidad, 0) AS montoPenalidad " +
                        "FROM pagos p LEFT JOIN penalidades pen ON p.idPrestamo = pen.idPrestamo AND p.numeroCuota = pen.numeroCuota" :
                "SELECT p.idPrestamo, p.numeroCuota, p.montoPagado, p.fechaPago, COALESCE(pen.montoPenalidad, 0) AS montoPenalidad " +
                        "FROM pagos p LEFT JOIN penalidades pen ON p.idPrestamo = pen.idPrestamo AND p.numeroCuota = pen.numeroCuota " +
                        "WHERE p.idPrestamo = ?";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nombreArchivo = "exportacion/pagos_" + timestamp + ".csv";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Crear directorio exportacion si no existe
        crearDirectorioExportacion();

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

            if (idPrestamo != null) {
                stmt.setString(1, idPrestamo);
            }

            ResultSet rs = stmt.executeQuery();

            // Verificar si hay datos
            if (!rs.next()) {
                System.out.println("❌ No se encontraron pagos para exportar" + (idPrestamo != null ? " para el préstamo ID " + idPrestamo : "") + ".");
                return null;
            }

            // Volver al inicio del ResultSet
            rs.beforeFirst();

            try (FileWriter writer = new FileWriter(nombreArchivo, StandardCharsets.UTF_8)) {
                // Agregar BOM para UTF-8
                writer.write('\uFEFF');

                // Escribir encabezados
                writer.write("ID Préstamo,Número Cuota,Monto Pagado,Fecha Pago,Penalidad\n");

                // Escribir datos
                while (rs.next()) {
                    String id = rs.getString("idPrestamo");
                    int numeroCuota = rs.getInt("numeroCuota");
                    double montoPagado = rs.getDouble("montoPagado");
                    LocalDate fechaPago = rs.getObject("fechaPago", LocalDate.class);
                    double penalidad = rs.getDouble("montoPenalidad");
                    writer.write(String.format("\"%s\",\"%d\",\"%s\",\"%s\",\"%s\"\n",
                            escapeCsv(id), numeroCuota, formatoMoneda.format(montoPagado),
                            fechaPago != null ? fechaPago.format(formatter) : "N/A",
                            penalidad > 0 ? formatoMoneda.format(penalidad) : "$0,00"));
                }

                //System.out.println("Historial de pagos exportado a: " + nombreArchivo);
                return nombreArchivo;
            }
        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al exportar historial de pagos", e);
        } catch (IOException e) {
            throw new ClienteDAOException("❌ Error al escribir el archivo " + nombreArchivo, e);
        }
    }

    /**
     * Exporta los datos de clientes con cuotas en mora a un archivo CSV.
     * @param idCliente ID del cliente para filtrar (opcional, puede ser null para todos los clientes en mora).
     * @return Nombre del archivo generado, o null si no hay datos.
     * @throws ClienteDAOException Si ocurre un error durante la exportación.
     */
    public String exportarClientesEnMora(String idCliente) throws ClienteDAOException {
        String sql = idCliente == null ?
                "SELECT p.idPrestamo, c.idCliente, c.nombre, cu.numeroCuota, cu.montoCuota, cu.fechaVencimiento, COALESCE(pen.montoPenalidad, 0) AS montoPenalidad " +
                        "FROM clientes c " +
                        "JOIN prestamos p ON c.idCliente = p.idCliente " +
                        "JOIN cuotas cu ON p.idPrestamo = cu.idPrestamo " +
                        "LEFT JOIN pagos pg ON cu.idPrestamo = pg.idPrestamo AND cu.numeroCuota = pg.numeroCuota " +
                        "LEFT JOIN penalidades pen ON cu.idPrestamo = pen.idPrestamo AND cu.numeroCuota = pen.numeroCuota " +
                        "WHERE p.estado = 'EN_MORA' AND pg.idPrestamo IS NULL AND cu.fechaVencimiento < ? " +
                        "ORDER BY c.idCliente, p.idPrestamo, cu.numeroCuota" :
                "SELECT p.idPrestamo, c.idCliente, c.nombre, cu.numeroCuota, cu.montoCuota, cu.fechaVencimiento, COALESCE(pen.montoPenalidad, 0) AS montoPenalidad " +
                        "FROM clientes c " +
                        "JOIN prestamos p ON c.idCliente = p.idCliente " +
                        "JOIN cuotas cu ON p.idPrestamo = cu.idPrestamo " +
                        "LEFT JOIN pagos pg ON cu.idPrestamo = pg.idPrestamo AND cu.numeroCuota = pg.numeroCuota " +
                        "LEFT JOIN penalidades pen ON cu.idPrestamo = pen.idPrestamo AND cu.numeroCuota = pen.numeroCuota " +
                        "WHERE p.estado = 'EN_MORA' AND pg.idPrestamo IS NULL AND cu.fechaVencimiento < ? AND c.idCliente = ? " +
                        "ORDER BY c.idCliente, p.idPrestamo, cu.numeroCuota";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nombreArchivo = "exportacion/clientes_en_mora_" + timestamp + ".csv";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Crear directorio exportacion si no existe
        crearDirectorioExportacion();

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

            stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
            if (idCliente != null) {
                stmt.setString(2, idCliente);
            }

            ResultSet rs = stmt.executeQuery();

            // Verificar si hay datos
            if (!rs.next()) {
                System.out.println(idCliente == null ?
                        "✅ No se encontraron clientes con cuotas en mora." :
                        "✅ El cliente con DNI " + idCliente + " no tiene cuotas en mora.");
                return null;
            }

            // Volver al inicio del ResultSet
            rs.beforeFirst();

            try (FileWriter writer = new FileWriter(nombreArchivo, StandardCharsets.UTF_8)) {
                // Agregar BOM para UTF-8
                writer.write('\uFEFF');

                // Escribir encabezados
                writer.write("ID Préstamo,ID Cliente,Nombre,Número Cuota,Monto Cuota,Fecha Vencimiento,Penalidad,Total a Pagar\n");

                // Escribir datos
                while (rs.next()) {
                    String idPrestamo = rs.getString("idPrestamo");
                    String idCli = rs.getString("idCliente");
                    String nombre = rs.getString("nombre");
                    int numeroCuota = rs.getInt("numeroCuota");
                    double montoCuota = rs.getDouble("montoCuota");
                    LocalDate fechaVencimiento = rs.getObject("fechaVencimiento", LocalDate.class);
                    double penalidad = rs.getDouble("montoPenalidad");
                    double totalAPagar = montoCuota + penalidad;
                    writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%d\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                            escapeCsv(idPrestamo), escapeCsv(idCli), escapeCsv(nombre), numeroCuota,
                            formatoMoneda.format(montoCuota),
                            fechaVencimiento != null ? fechaVencimiento.format(formatter) : "N/A",
                            penalidad > 0 ? formatoMoneda.format(penalidad) : "0,00",
                            formatoMoneda.format(totalAPagar)));
                }

                //System.out.println("Datos de clientes en mora exportados a: " + nombreArchivo);
                return nombreArchivo;
            }
        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al exportar datos de clientes en mora", e);
        } catch (IOException e) {
            throw new ClienteDAOException("❌ Error al escribir el archivo " + nombreArchivo, e);
        }
    }

    /**
     * Obtiene el número de cuotas pagadas para un préstamo.
     * @param conn Conexión a la base de datos.
     * @param idPrestamo ID del préstamo.
     * @return Número de cuotas pagadas.
     * @throws SQLException Si ocurre un error en la consulta.
     */
    private int getCuotasPagadas(Connection conn, String idPrestamo) throws SQLException {
        String sql = "SELECT COUNT(*) as cuotasPagadas FROM pagos WHERE idPrestamo = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idPrestamo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("cuotasPagadas");
            }
            return 0;
        }
    }

    /**
     * Obtiene el número de cuotas en mora para un préstamo.
     * @param conn Conexión a la base de datos.
     * @param idPrestamo ID del préstamo.
     * @return Número de cuotas en mora.
     * @throws SQLException Si ocurre un error en la consulta.
     */
    private int getCuotasEnMora(Connection conn, String idPrestamo) throws SQLException {
        String sql = "SELECT COUNT(*) as cuotasEnMora " +
                "FROM cuotas cu " +
                "LEFT JOIN pagos pg ON cu.idPrestamo = pg.idPrestamo AND cu.numeroCuota = pg.numeroCuota " +
                "WHERE cu.idPrestamo = ? AND pg.idPrestamo IS NULL AND cu.fechaVencimiento < ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idPrestamo);
            stmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("cuotasEnMora");
            }
            return 0;
        }
    }

    /**
     * Escapa comas y comillas en campos para formato CSV.
     * @param value Valor a escapar.
     * @return Valor escapado.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    /**
     * Crea el directorio 'exportacion' si no existe.
     */
    private void crearDirectorioExportacion() {
        File directorio = new File("exportacion");
        if (!directorio.exists()) {
            if (directorio.mkdirs()) {
                System.out.println("Directorio 'exportacion' creado.");
            } else {
                System.err.println("No se pudo crear el directorio 'exportacion'.");
            }
        }
    }
}