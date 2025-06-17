package com.financierasolandino.model;

public class Cliente {
    private String idCliente;
    private String nombre;
    private String direccion;
    private String telefono;
    private String correoElectronico;

    public Cliente(String idCliente, String nombre, String direccion, String telefono, String correoElectronico) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.correoElectronico = correoElectronico;
    }

    // Getters
    public String getIdCliente() { return idCliente; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getTelefono() { return telefono; }
    public String getCorreoElectronico() { return correoElectronico; }
}
