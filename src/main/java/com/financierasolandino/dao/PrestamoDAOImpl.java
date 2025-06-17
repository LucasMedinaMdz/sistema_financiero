package com.financierasolandino.dao;

import com.financierasolandino.db.ConexionDB;
import com.financierasolandino.model.Cuota;
import com.financierasolandino.model.Pago;
import com.financierasolandino.model.Prestamo;
import com.financierasolandino.util.Utilidad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class PrestamoDAOImpl implements PrestamoDAO {
    private static final String SQL_CREAR_PRESTAMO = "INSERT INTO prestamos (idPrestamo, idCliente, monto, tasaInteres, numeroCuotas, tipoPrestamo, fecha_creacion, saldo_pendiente, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_VERIFICAR_PRESTAMO = "SELECT COUNT(*) FROM prestamos WHERE idPrestamo = ?";
    private static final String SQL_OBTENER_PRESTAMOS_CLIENTE = "SELECT * FROM prestamos WHERE idCliente = ?";
    private static final String SQL_OBTENER_PRESTAMO = "SELECT * FROM prestamos WHERE idPrestamo = ?";
    private static final String SQL_ACTUALIZAR_ESTADO = "UPDATE prestamos SET estado = ? WHERE idPrestamo = ?";
    private static final String SQL_INSERTAR_PENALIDAD = "INSERT INTO penalidades (idPrestamo, numeroCuota, montoPenalidad, fechaAplicacion) VALUES (?, ?, ?, ?)";
    private static final String SQL_VERIFICAR_PENALIDAD = "SELECT COUNT(*) FROM penalidades WHERE idPrestamo = ? AND numeroCuota = ?";
    private static final String SQL_OBTENER_PENALIDADES = "SELECT SUM(montoPenalidad) AS totalPenalidades FROM penalidades WHERE idPrestamo = ?";
    private static final String SQL_OBTENER_PENALIDADES_CUOTAS = "SELECT numeroCuota, montoPenalidad FROM penalidades WHERE idPrestamo = ?";
    private final NumberFormat formatoMoneda = Utilidad.getArgentinaNumberFormat();

    @Override
    public boolean crearPrestamo(Prestamo prestamo) throws ClienteDAOException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_CREAR_PRESTAMO)) {
            stmt.setString(1, prestamo.getIdPrestamo());
            stmt.setString(2, prestamo.getIdCliente());
            stmt.setBigDecimal(3, new BigDecimal(prestamo.getMonto()).setScale(2, RoundingMode.HALF_UP));
            stmt.setBigDecimal(4, new BigDecimal(prestamo.getTasaInteres()).setScale(4, RoundingMode.HALF_UP));
            stmt.setInt(5, prestamo.getNumeroCuotas());
            stmt.setString(6, prestamo.getTipoPrestamo().name());
            stmt.setObject(7, prestamo.getFechaCreacion());
            stmt.setBigDecimal(8, new BigDecimal(prestamo.getSaldoPendiente()).setScale(2, RoundingMode.HALF_UP));
            stmt.setString(9, prestamo.getEstado().name());
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al crear el préstamo: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verificarExistenciaPrestamo(String idPrestamo) throws ClienteDAOException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_VERIFICAR_PRESTAMO)) {
            stmt.setString(1, idPrestamo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al verificar la existencia del préstamo: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Prestamo> obtenerPrestamosPorCliente(String idCliente) throws ClienteDAOException {
        List<Prestamo> prestamos = new ArrayList<>();
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_PRESTAMOS_CLIENTE)) {
            stmt.setString(1, idCliente);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Prestamo prestamo = new Prestamo(
                        rs.getString("idPrestamo"),
                        rs.getString("idCliente"),
                        rs.getBigDecimal("monto").doubleValue(),
                        rs.getBigDecimal("tasaInteres").doubleValue(),
                        rs.getInt("numeroCuotas"),
                        Prestamo.TipoPrestamo.valueOf(rs.getString("tipoPrestamo")),
                        rs.getObject("fecha_creacion", LocalDate.class),
                        rs.getBigDecimal("saldo_pendiente").doubleValue(),
                        Prestamo.EstadoPrestamo.valueOf(rs.getString("estado"))
                );
                prestamos.add(prestamo);
            }
            return prestamos;
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al obtener los préstamos del cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Prestamo> obtenerPrestamo(String idPrestamo) throws ClienteDAOException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_PRESTAMO)) {
            stmt.setString(1, idPrestamo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Prestamo prestamo = new Prestamo(
                        rs.getString("idPrestamo"),
                        rs.getString("idCliente"),
                        rs.getBigDecimal("monto").doubleValue(),
                        rs.getBigDecimal("tasaInteres").doubleValue(),
                        rs.getInt("numeroCuotas"),
                        Prestamo.TipoPrestamo.valueOf(rs.getString("tipoPrestamo")),
                        rs.getObject("fecha_creacion", LocalDate.class),
                        rs.getBigDecimal("saldo_pendiente").doubleValue(),
                        Prestamo.EstadoPrestamo.valueOf(rs.getString("estado"))
                );
                return Optional.of(prestamo);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al obtener el préstamo: " + e.getMessage(), e);
        }
    }

    @Override
    public void consultarEstadoPrestamo(String idPrestamo, CuotaDAO cuotaDAO, PagoDAO pagoDAO) throws ClienteDAOException {
        Optional<Prestamo> prestamoOpt = obtenerPrestamo(idPrestamo);
        if (!prestamoOpt.isPresent()) {
            throw new ClienteDAOException("Préstamo con ID " + idPrestamo + " no encontrado.");
        }
        Prestamo prestamo = prestamoOpt.get();

        List<Cuota> cuotas;
        List<Pago> pagos;
        try {
            cuotas = cuotaDAO.obtenerCuotas(idPrestamo);
            pagos = pagoDAO.obtenerPagos(idPrestamo);
        } catch (ClienteDAOException e) {
            throw new ClienteDAOException("Error al obtener cuotas o pagos del préstamo " + idPrestamo + ": " + e.getMessage(), e);
        }

        // Verificar mora y aplicar penalidades
        List<Cuota> cuotasEnMora = verificarMora(cuotas, pagos);
        double totalPenalidadesNuevas = calcularPenalidad(cuotasEnMora, idPrestamo);
        if (!cuotasEnMora.isEmpty()) {
            aplicarPenalidad(idPrestamo, cuotasEnMora);
        }

        // Obtener penalidades totales acumuladas
        double totalPenalidadesAcumuladas = obtenerTotalPenalidades(idPrestamo);

        // Calcular total de cuotas pendientes incluyendo penalidades de cuotas en mora
        double totalCuotasPendientes = calcularSaldoPendiente(cuotas, pagos);
        // Sumar penalidades de cuotas en mora actuales
        double penalidadesCuotasEnMora = 0.0;
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_PENALIDADES_CUOTAS)) {
            stmt.setString(1, idPrestamo);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int numeroCuota = rs.getInt("numeroCuota");
                if (cuotasEnMora.stream().anyMatch(c -> c.getNumeroCuota() == numeroCuota)) {
                    penalidadesCuotasEnMora += rs.getDouble("montoPenalidad");
                }
            }
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al obtener penalidades de cuotas en mora: " + e.getMessage(), e);
        }
        totalCuotasPendientes += penalidadesCuotasEnMora;

        int cuotasPagadas = pagos.size();

        System.out.println("\n=== Estado del Préstamo ID " + idPrestamo + " ===");
        System.out.println("Tipo de Préstamo: " + prestamo.getTipoPrestamo());
        System.out.println("Estado del Préstamo: " + prestamo.getEstado());
        System.out.println("Monto Original: " + formatoMoneda.format(prestamo.getMonto()));
        System.out.println("Número Total de Cuotas: " + prestamo.getNumeroCuotas());
        System.out.println("Cuotas Pagadas: " + cuotasPagadas);
        System.out.println("Cuotas Pendientes: " + (prestamo.getNumeroCuotas() - cuotasPagadas));
        System.out.println("Cuotas en Mora: " + cuotasEnMora.size());
        if (!cuotasEnMora.isEmpty()) {
            System.out.println("⚠ ALERTA: El préstamo tiene cuotas en mora con penalidades aplicadas.");
            System.out.println("  Cuotas en Mora:");
            for (Cuota cuota : cuotasEnMora) {
                double penalidad = obtenerPenalidad(idPrestamo, cuota.getNumeroCuota());
                System.out.println("    Cuota " + cuota.getNumeroCuota() +
                        ": " + formatoMoneda.format(cuota.getMontoCuota()) +
                        ", Vencimiento: " + cuota.getFechaVencimiento() +
                        ", Penalidad: " + formatoMoneda.format(penalidad));
            }
        }
        System.out.println("  Total Penalidades Acumuladas: " + formatoMoneda.format(totalPenalidadesAcumuladas) +
                " (incluye penalidades de cuotas en mora actuales y de cuotas previamente pagadas)");
        System.out.println("Saldo Pendiente (Capital): " + formatoMoneda.format(prestamo.getSaldoPendiente()));
        System.out.println("Total de Cuotas Pendientes: " + formatoMoneda.format(totalCuotasPendientes));
        System.out.println("Nota: El Saldo Pendiente (Capital) refleja el capital restante, mientras que el Total de Cuotas Pendientes incluye capital, intereses y penalidades de cuotas en mora actuales.");

        // Preguntar si desea ver el historial de pagos
        System.out.print("\n¿Desea ver el historial de pagos? (si/no): ");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String respuesta = scanner.nextLine().trim();
            String respuestaNormalizada = Normalizer.normalize(respuesta.toLowerCase(), Normalizer.Form.NFD)
                    .replaceAll("[^\\p{ASCII}]", "");
            if (respuestaNormalizada.equals("si")) {
                mostrarHistorialPagos(idPrestamo, pagoDAO, formatoMoneda);
                break;
            } else if (respuestaNormalizada.equals("no")) {
                break;
            } else {
                System.out.println("❌ Respuesta no válida. Escriba 'si' o 'no'.");
                System.out.print("¿Desea ver el historial de pagos? (si/no): ");
            }
        }
    }

    private List<Cuota> verificarMora(List<Cuota> cuotas, List<Pago> pagos) {
        List<Cuota> cuotasEnMora = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (Cuota cuota : cuotas) {
            boolean pagada = pagos.stream().anyMatch(p -> p.getNumeroCuota() == cuota.getNumeroCuota());
            if (!pagada && cuota.getFechaVencimiento().isBefore(hoy)) {
                cuotasEnMora.add(cuota);
            }
        }
        return cuotasEnMora;
    }

    private double calcularPenalidad(List<Cuota> cuotasEnMora, String idPrestamo) throws ClienteDAOException {
        double totalPenalidades = 0.0;
        Connection conn = null;
        try {
            conn = ConexionDB.conectar();
            conn.setAutoCommit(false);

            for (Cuota cuota : cuotasEnMora) {
                // Verificar si la penalidad ya fue aplicada
                try (PreparedStatement stmtVerificar = conn.prepareStatement(SQL_VERIFICAR_PENALIDAD)) {
                    stmtVerificar.setString(1, idPrestamo);
                    stmtVerificar.setInt(2, cuota.getNumeroCuota());
                    ResultSet rs = stmtVerificar.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        continue; // Penalidad ya aplicada
                    }
                }

                double penalidad = cuota.getMontoCuota() * 0.05; // 5% penalidad
                totalPenalidades += penalidad;

                // Registrar penalidad
                try (PreparedStatement stmtPenalidad = conn.prepareStatement(SQL_INSERTAR_PENALIDAD)) {
                    stmtPenalidad.setString(1, idPrestamo);
                    stmtPenalidad.setInt(2, cuota.getNumeroCuota());
                    stmtPenalidad.setDouble(3, penalidad);
                    stmtPenalidad.setObject(4, LocalDate.now());
                    stmtPenalidad.executeUpdate();
                }
            }

            conn.commit();
            return Math.round(totalPenalidades * 100.0) / 100.0;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new ClienteDAOException("Error al hacer rollback: " + ex.getMessage(), ex);
                }
            }
            throw new ClienteDAOException("Error al calcular y registrar penalidades: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    throw new ClienteDAOException("Error al cerrar la conexión: " + e.getMessage(), e);
                }
            }
        }
    }

    private void aplicarPenalidad(String idPrestamo, List<Cuota> cuotasEnMora) throws ClienteDAOException {
        if (cuotasEnMora.isEmpty()) return;

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_ESTADO)) {
            stmt.setString(1, Prestamo.EstadoPrestamo.EN_MORA.name());
            stmt.setString(2, idPrestamo);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al actualizar el estado del préstamo " + idPrestamo + ": " + e.getMessage(), e);
        }
    }

    private double obtenerPenalidad(String idPrestamo, int numeroCuota) throws ClienteDAOException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement("SELECT montoPenalidad FROM penalidades WHERE idPrestamo = ? AND numeroCuota = ?")) {
            stmt.setString(1, idPrestamo);
            stmt.setInt(2, numeroCuota);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("montoPenalidad").doubleValue();
            }
            return 0.0;
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al obtener penalidad para cuota " + numeroCuota + ": " + e.getMessage(), e);
        }
    }

    private double obtenerTotalPenalidades(String idPrestamo) throws ClienteDAOException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_PENALIDADES)) {
            stmt.setString(1, idPrestamo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                BigDecimal totalPenalidades = rs.getBigDecimal("totalPenalidades");
                return totalPenalidades != null ? totalPenalidades.doubleValue() : 0.0;
            }
            return 0.0;
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al obtener el total de penalidades para el préstamo " + idPrestamo + ": " + e.getMessage(), e);
        }
    }

    public void mostrarHistorialPagos(String idPrestamo, PagoDAO pagoDAO, NumberFormat formatoMoneda) throws ClienteDAOException {
        List<Pago> historial = pagoDAO.obtenerHistorialPagos(idPrestamo);
        if (historial.isEmpty()) {
            System.out.println("No hay pagos registrados para el préstamo ID " + idPrestamo + ".");
            return;
        }

        System.out.println("\n=== Historial de Pagos del Préstamo ID " + idPrestamo + " ===");
        for (Pago pago : historial) {
            double penalidad = pagoDAO.obtenerPenalidad(idPrestamo, pago.getNumeroCuota());
            System.out.println("Cuota " + pago.getNumeroCuota() +
                    ": " + formatoMoneda.format(pago.getMontoPagado()) +
                    (penalidad > 0 ? " (Incluye penalidad: " + formatoMoneda.format(penalidad) + ")" : "") +
                    ", Fecha: " + pago.getFechaPago());
        }
    }

    private double calcularSaldoPendiente(List<Cuota> cuotas, List<Pago> pagos) {
        BigDecimal saldoPendiente = BigDecimal.ZERO;
        for (Cuota cuota : cuotas) {
            boolean pagada = pagos.stream().anyMatch(p -> p.getNumeroCuota() == cuota.getNumeroCuota());
            if (!pagada) {
                saldoPendiente = saldoPendiente.add(new BigDecimal(cuota.getMontoCuota()).setScale(2, RoundingMode.HALF_UP));
            }
        }
        return saldoPendiente.doubleValue();
    }

    @Override
    public boolean verificarExistenciaCliente(String id) throws ClienteDAOException {
        throw new ClienteDAOException("Operación no soportada: verificarExistenciaCliente no es aplicable para PrestamoDAO.");
    }

    @Override
    public boolean registrarCliente(Prestamo entidad) throws ClienteDAOException {
        throw new ClienteDAOException("Operación no soportada: registrarCliente no es aplicable para PrestamoDAO.");
    }

    @Override
    public Optional<Prestamo> obtenerCliente(String id) throws ClienteDAOException {
        throw new ClienteDAOException("Operación no soportada: obtenerCliente no es aplicable para PrestamoDAO.");
    }
}