package com.financierasolandino.dao;

import java.util.Optional;

public interface DAO<T, K> {
    boolean verificarExistenciaCliente(K id) throws ClienteDAOException;
    boolean registrarCliente(T entidad) throws ClienteDAOException;
    Optional<T> obtenerCliente(K id) throws ClienteDAOException;
}
