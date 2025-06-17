package com.financierasolandino.dao;

import com.financierasolandino.db.ConexionDB;
import com.financierasolandino.model.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteDAO implements DAO<Cliente, String> {
    private static final String SQL_VERIFICAR_CLIENTE = "SELECT idCliente FROM clientes WHERE idCliente = ?";
    private static final String SQL_INSERTAR_CLIENTE = "INSERT INTO clientes (idCliente, nombre, direccion, telefono, correoElectronico) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_OBTENER_CLIENTE = "SELECT * FROM clientes WHERE idCliente = ?";
    private static final String SQL_ACTUALIZAR_CLIENTE = "UPDATE clientes SET direccion = ?, telefono = ?, correoElectronico = ? WHERE idCliente = ?";
    private static final String SQL_LISTAR_CLIENTES = "SELECT * FROM clientes";

    /**
     * Verifica si un cliente existe en la base de datos según su ID.
     * @param idCliente El identificador único del cliente.
     * @return true si el cliente existe, false en caso contrario.
     * @throws ClienteDAOException Si ocurre un error al consultar la base de datos.
     */
    @Override
    public boolean verificarExistenciaCliente(String idCliente) throws ClienteDAOException {
        if (idCliente == null || idCliente.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del cliente no puede ser nulo o vacío");
        }
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_VERIFICAR_CLIENTE)) {
            stmt.setString(1, idCliente);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al verificar la existencia del cliente con DNI: " + idCliente, e);
        }
    }

    /**
     * Registra un nuevo cliente en la base de datos.
     * @param cliente El cliente a registrar.
     * @return true si el registro fue exitoso.
     * @throws ClienteDAOException Si el cliente ya existe o hay un error en la base de datos.
     */
    @Override
    public boolean registrarCliente(Cliente cliente) throws ClienteDAOException {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser nulo");
        }
        if (cliente.getIdCliente() == null || cliente.getIdCliente().trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI del cliente no puede ser nulo o vacío");
        }
        if (verificarExistenciaCliente(cliente.getIdCliente())) {
            throw new ClienteDAOException("El cliente con ID " + cliente.getIdCliente() + " ya está registrado.");
        }
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR_CLIENTE)) {
            stmt.setString(1, cliente.getIdCliente());
            stmt.setString(2, cliente.getNombre());
            stmt.setString(3, cliente.getDireccion());
            stmt.setString(4, cliente.getTelefono());
            stmt.setString(5, cliente.getCorreoElectronico());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al registrar el cliente con DNI: " + cliente.getIdCliente(), e);
        }
    }

    /**
     * Obtiene un cliente de la base de datos según su ID.
     * @param idCliente El identificador único del cliente.
     * @return Un Optional con el cliente si existe, o vacío si no.
     * @throws ClienteDAOException Si ocurre un error al consultar la base de datos.
     */
    @Override
    public Optional<Cliente> obtenerCliente(String idCliente) throws ClienteDAOException {
        if (idCliente == null || idCliente.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del cliente no puede ser nulo o vacío");
        }
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_OBTENER_CLIENTE)) {
            stmt.setString(1, idCliente);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new Cliente(
                        rs.getString("idCliente"),
                        rs.getString("nombre"),
                        rs.getString("direccion"),
                        rs.getString("telefono"),
                        rs.getString("correoElectronico")
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al obtener el cliente con DNI: " + idCliente, e);
        }
    }

    /**
     * Consulta los datos de un cliente según su ID y los devuelve para visualización.
     * @param idCliente El identificador único del cliente.
     * @return Un Optional con el cliente si existe, o vacío si no.
     * @throws ClienteDAOException Si ocurre un error al consultar la base de datos.
     */
    public Optional<Cliente> consultarCliente(String idCliente) throws ClienteDAOException {
        return obtenerCliente(idCliente);
    }

    /**
     * Edita los datos de contacto (dirección, teléfono, correo electrónico) de un cliente.
     * @param cliente El cliente con los datos actualizados.
     * @return true si la edición fue exitosa, false si el cliente no existe.
     * @throws ClienteDAOException Si ocurre un error al actualizar la base de datos.
     */
    public boolean editarCliente(Cliente cliente) throws ClienteDAOException {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser nulo");
        }
        if (cliente.getIdCliente() == null || cliente.getIdCliente().trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del cliente no puede ser nulo o vacío");
        }
        if (!verificarExistenciaCliente(cliente.getIdCliente())) {
            return false;
        }
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_ACTUALIZAR_CLIENTE)) {
            stmt.setString(1, cliente.getDireccion());
            stmt.setString(2, cliente.getTelefono());
            stmt.setString(3, cliente.getCorreoElectronico());
            stmt.setString(4, cliente.getIdCliente());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al editar el cliente con DNI: " + cliente.getIdCliente(), e);
        }
    }

    /**
     * Devuelve una lista con todos los clientes registrados en la base de datos.
     * @return Una lista de objetos Cliente.
     * @throws ClienteDAOException Si ocurre un error al consultar la base de datos.
     */
    public List<Cliente> listarClientes() throws ClienteDAOException {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_LISTAR_CLIENTES);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                clientes.add(new Cliente(
                        rs.getString("idCliente"),
                        rs.getString("nombre"),
                        rs.getString("direccion"),
                        rs.getString("telefono"),
                        rs.getString("correoElectronico")
                ));
            }
            return clientes;
        } catch (SQLException e) {
            throw new ClienteDAOException("❌ Error al listar los clientes", e);
        }
    }
}