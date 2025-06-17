package com.financierasolandino.dao;

import com.financierasolandino.db.ConexionDB;
import com.financierasolandino.model.Pago;
import com.financierasolandino.model.Prestamo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PagoDAO {
    private static final String SQL_INSERTAR_PAGO = "INSERT INTO pagos (idPrestamo, numeroCuota, montoPagado, fechaPago) VALUES (?, ?, ?, ?)";
    private static final String SQL_OBTENER_PAGOS = "SELECT * FROM pagos WHERE idPrestamo = ? ORDER BY numeroCuota";
    private static final String SQL_VERIFICAR_PAGO = "SELECT COUNT(*) FROM pagos WHERE idPrestamo = ? AND numeroCuota = ?";
    private static final String SQL_ACTUALIZAR_SALDO = "UPDATE prestamos SET saldo_pendiente = saldo_pendiente - ? WHERE idPrestamo = ?";
    private static final String SQL_OBTENER_CUOTA = "SELECT capitalAmortizado FROM cuotas WHERE idPrestamo = ? AND numeroCuota = ?";
    private static final String SQL_OBTENER_PENALIDAD = "SELECT montoPenalidad FROM penalidades WHERE idPrestamo = ? AND numeroCuota = ?";
    private static final String SQL_OBTENER_CUOTAS = "SELECT numeroCuota, fechaVencimiento FROM cuotas WHERE idPrestamo = ? ORDER BY numeroCuota";
    private static final String SQL_ACTUALIZAR_ESTADO = "UPDATE prestamos SET estado = ? WHERE idPrestamo = ?";
    private static final String SQL_CONTAR_PAGOS = "SELECT COUNT(*) FROM pagos WHERE idPrestamo = ?";
    private static final String SQL_OBTENER_NUMERO_CUOTAS = "SELECT numeroCuotas FROM prestamos WHERE idPrestamo = ?";
    private static final String SQL_FORCE_ZERO_SALDO = "UPDATE prestamos SET saldo_pendiente = 0 WHERE idPrestamo = ?";
    private static final String SQL_OBTENER_HISTORIAL_PAGOS_CON_PENALIDAD =
            "SELECT p.idPrestamo, p.numeroCuota, p.montoPagado, p.fechaPago, COALESCE(pen.montoPenalidad, 0.0) AS montoPenalidad " +
                    "FROM pagos p LEFT JOIN penalidades pen ON p.idPrestamo = pen.idPrestamo AND p.numeroCuota = pen.numeroCuota " +
                    "WHERE p.idPrestamo = ? ORDER BY p.numeroCuota";

    public boolean registrarPago(Pago pago, double penalidad) throws ClienteDAOException {
        Connection conn = null;
        try {
            conn = ConexionDB.conectar();
            conn.setAutoCommit(false);

            // Verificar si el pago ya existe
            try (PreparedStatement stmtVerificar = conn.prepareStatement(SQL_VERIFICAR_PAGO)) {
                stmtVerificar.setString(1, pago.getIdPrestamo());
                stmtVerificar.setInt(2, pago.getNumeroCuota());
                ResultSet rs = stmtVerificar.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new ClienteDAOException("La cuota " + pago.getNumeroCuota() + " del préstamo " + pago.getIdPrestamo() + " ya está pagada.");
                }
            }

            // Obtener capitalAmortizado de la cuota
            BigDecimal capitalAmortizado;
            try (PreparedStatement stmtCuota = conn.prepareStatement(SQL_OBTENER_CUOTA)) {
                stmtCuota.setString(1, pago.getIdPrestamo());
                stmtCuota.setInt(2, pago.getNumeroCuota());
                ResultSet rs = stmtCuota.executeQuery();
                if (rs.next()) {
                    capitalAmortizado = rs.getBigDecimal("capitalAmortizado").setScale(2, RoundingMode.HALF_UP);
                } else {
                    throw new ClienteDAOException("Cuota " + pago.getNumeroCuota() + " no encontrada para el préstamo " + pago.getIdPrestamo());
                }
            }

            // Registrar el pago (montoPagado incluye cuota + penalidad)
            try (PreparedStatement stmtPago = conn.prepareStatement(SQL_INSERTAR_PAGO)) {
                stmtPago.setString(1, pago.getIdPrestamo());
                stmtPago.setInt(2, pago.getNumeroCuota());
                stmtPago.setBigDecimal(3, new BigDecimal(pago.getMontoPagado()).setScale(2, RoundingMode.HALF_UP));
                stmtPago.setObject(4, pago.getFechaPago());
                stmtPago.executeUpdate();
            }

            // Actualizar saldo pendiente con capitalAmortizado
            try (PreparedStatement stmtSaldo = conn.prepareStatement(SQL_ACTUALIZAR_SALDO)) {
                stmtSaldo.setBigDecimal(1, capitalAmortizado);
                stmtSaldo.setString(2, pago.getIdPrestamo());
                stmtSaldo.executeUpdate();
            }

            // Verificar si todas las cuotas están pagadas
            int numeroCuotasTotales = 0;
            try (PreparedStatement stmtCuotas = conn.prepareStatement(SQL_OBTENER_NUMERO_CUOTAS)) {
                stmtCuotas.setString(1, pago.getIdPrestamo());
                ResultSet rs = stmtCuotas.executeQuery();
                if (rs.next()) {
                    numeroCuotasTotales = rs.getInt("numeroCuotas");
                }
            }

            int numeroPagos = 0;
            try (PreparedStatement stmtPagos = conn.prepareStatement(SQL_CONTAR_PAGOS)) {
                stmtPagos.setString(1, pago.getIdPrestamo());
                ResultSet rs = stmtPagos.executeQuery();
                if (rs.next()) {
                    numeroPagos = rs.getInt(1);
                }
            }

            // Si todas las cuotas están pagadas, forzar saldo_pendiente a 0
            if (numeroPagos >= numeroCuotasTotales) {
                try (PreparedStatement stmtForceZero = conn.prepareStatement(SQL_FORCE_ZERO_SALDO)) {
                    stmtForceZero.setString(1, pago.getIdPrestamo());
                    stmtForceZero.executeUpdate();
                }
            }

            // Determinar el estado del préstamo
            String nuevoEstado;
            if (numeroPagos >= numeroCuotasTotales) {
                nuevoEstado = Prestamo.EstadoPrestamo.CANCELADO.name();
            } else {
                boolean hayMora = false;
                try (PreparedStatement stmtCuotas = conn.prepareStatement(SQL_OBTENER_CUOTAS)) {
                    stmtCuotas.setString(1, pago.getIdPrestamo());
                    ResultSet rs = stmtCuotas.executeQuery();
                    LocalDate hoy = LocalDate.now();
                    while (rs.next()) {
                        LocalDate fechaVencimiento = rs.getObject("fechaVencimiento", LocalDate.class);
                        int numeroCuota = rs.getInt("numeroCuota");
                        try (PreparedStatement stmtPago = conn.prepareStatement(SQL_VERIFICAR_PAGO)) {
                            stmtPago.setString(1, pago.getIdPrestamo());
                            stmtPago.setInt(2, numeroCuota);
                            ResultSet rsPago = stmtPago.executeQuery();
                            boolean pagada = rsPago.next() && rsPago.getInt(1) > 0;
                            if (!pagada && fechaVencimiento.isBefore(hoy)) {
                                hayMora = true;
                                break;
                            }
                        }
                    }
                }
                nuevoEstado = hayMora ? Prestamo.EstadoPrestamo.EN_MORA.name() : Prestamo.EstadoPrestamo.ACTIVO.name();
            }

            // Actualizar estado del préstamo
            try (PreparedStatement stmtEstado = conn.prepareStatement(SQL_ACTUALIZAR_ESTADO)) {
                stmtEstado.setString(1, nuevoEstado);
                stmtEstado.setString(2, pago.getIdPrestamo());
                stmtEstado.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new ClienteDAOException("Error al hacer rollback: " + ex.getMessage(), ex);
                }
            }
            throw new ClienteDAOException("Error al registrar el pago: " + e.getMessage(), e);
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

    public List<Pago> obtenerPagos(String idPrestamo) throws ClienteDAOException {
        List<Pago> pagos = new ArrayList<>();
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_PAGOS)) {
            stmt.setString(1, idPrestamo);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Pago pago = new Pago(
                        rs.getString("idPrestamo"),
                        rs.getInt("numeroCuota"),
                        rs.getBigDecimal("montoPagado").doubleValue(),
                        rs.getObject("fechaPago", LocalDate.class)
                );
                pagos.add(pago);
            }
            return pagos;
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al obtener los pagos del préstamo " + idPrestamo, e);
        }
    }

    public List<Pago> obtenerHistorialPagos(String idPrestamo) throws ClienteDAOException {
        List<Pago> historial = new ArrayList<>();
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_HISTORIAL_PAGOS_CON_PENALIDAD)) {
            stmt.setString(1, idPrestamo);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Pago pago = new Pago(
                        rs.getString("idPrestamo"),
                        rs.getInt("numeroCuota"),
                        rs.getBigDecimal("montoPagado").doubleValue(),
                        rs.getObject("fechaPago", LocalDate.class)
                );
                historial.add(pago);
            }
            return historial;
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al obtener el historial de pagos del préstamo " + idPrestamo, e);
        }
    }

    public double obtenerPenalidad(String idPrestamo, int numeroCuota) throws ClienteDAOException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_PENALIDAD)) {
            stmt.setString(1, idPrestamo);
            stmt.setInt(2, numeroCuota);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("montoPenalidad").doubleValue();
            }
            return 0.0;
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al obtener la penalidad para la cuota " + numeroCuota + " del préstamo " + idPrestamo, e);
        }
    }
}