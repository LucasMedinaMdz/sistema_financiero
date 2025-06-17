package com.financierasolandino.dao;

import com.financierasolandino.model.Prestamo;

import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;

public interface PrestamoDAO extends DAO<Prestamo, String> {
    boolean crearPrestamo(Prestamo prestamo) throws ClienteDAOException;
    boolean verificarExistenciaPrestamo(String idPrestamo) throws ClienteDAOException;
    List<Prestamo> obtenerPrestamosPorCliente(String idCliente) throws ClienteDAOException;
    Optional<Prestamo> obtenerPrestamo(String idPrestamo) throws ClienteDAOException;
    void consultarEstadoPrestamo(String idPrestamo, CuotaDAO cuotaDAO, PagoDAO pagoDAO) throws ClienteDAOException;
    void mostrarHistorialPagos(String idPrestamo, PagoDAO pagoDAO, NumberFormat currencyFormat) throws ClienteDAOException;
}