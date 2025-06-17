package com.financierasolandino.dao;

import com.financierasolandino.db.ConexionDB;
import com.financierasolandino.model.Cuota;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CuotaDAO {
    private static final String SQL_INSERTAR_CUOTA = "INSERT INTO cuotas (idPrestamo, numeroCuota, montoCuota, tasaAplicada, fechaVencimiento, capitalAmortizado) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_OBTENER_CUOTAS = "SELECT * FROM cuotas WHERE idPrestamo = ? ORDER BY numeroCuota";

    public boolean registrarCuota(Cuota cuota) throws ClienteDAOException {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_CUOTA)) {
            stmt.setString(1, cuota.getIdPrestamo());
            stmt.setInt(2, cuota.getNumeroCuota());
            stmt.setBigDecimal(3, new BigDecimal(cuota.getMontoCuota()).setScale(2, RoundingMode.HALF_UP));
            stmt.setBigDecimal(4, new BigDecimal(cuota.getTasaAplicada()).setScale(4, RoundingMode.HALF_UP));
            stmt.setObject(5, cuota.getFechaVencimiento());
            stmt.setBigDecimal(6, new BigDecimal(cuota.getCapitalAmortizado()).setScale(2, RoundingMode.HALF_UP));
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al registrar la cuota número " + cuota.getNumeroCuota() + " del préstamo " + cuota.getIdPrestamo(), e);
        }
    }

    public List<Cuota> obtenerCuotas(String idPrestamo) throws ClienteDAOException {
        List<Cuota> cuotas = new ArrayList<>();
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_CUOTAS)) {
            stmt.setString(1, idPrestamo);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Cuota cuota = new Cuota(
                        rs.getString("idPrestamo"),
                        rs.getInt("numeroCuota"),
                        rs.getBigDecimal("montoCuota").doubleValue(),
                        rs.getBigDecimal("tasaAplicada").doubleValue(),
                        rs.getObject("fechaVencimiento", LocalDate.class),
                        rs.getBigDecimal("capitalAmortizado").doubleValue()
                );
                cuotas.add(cuota);
            }
            return cuotas;
        } catch (SQLException e) {
            throw new ClienteDAOException("Error al obtener las cuotas del préstamo " + idPrestamo, e);
        }
    }
}