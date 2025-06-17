package com.financierasolandino.dao;

public class ClienteDAOException extends RuntimeException {
    public ClienteDAOException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClienteDAOException(String message) {
        super(message);
    }
}
