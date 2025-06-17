package com.financierasolandino.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionDB {
    private static final String ARCHIVO_PROPIEDADES = "config.properties";
    private static final String URL = "db.url";
    private static final String USUARIO = "db.usuario";
    private static final String CONTRASENA = "db.contrasena";
    private static final Properties propiedades = new Properties();

    static {
        try (InputStream input = ConexionDB.class.getClassLoader().getResourceAsStream(ARCHIVO_PROPIEDADES)) {
            if (input == null) {
                System.err.println("⚠️  No se encontró el archivo de configuración: '" + ARCHIVO_PROPIEDADES + "'");
                System.err.println("💡  Asegúrate de haber copiado y renombrado 'config.properties.ejemplo' como 'config.properties' en la carpeta 'resources'.");
                System.exit(1);  // Termina la ejecución para evitar stack trace confuso
            }
            propiedades.load(input);
        } catch (IOException e) {
            System.err.println("❌ Error al cargar el archivo de propiedades: " + ARCHIVO_PROPIEDADES);
            e.printStackTrace();
            System.exit(1);
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ No se encontró el driver JDBC de MySQL");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(
                propiedades.getProperty(URL),
                propiedades.getProperty(USUARIO),
                propiedades.getProperty(CONTRASENA)
        );
    }
}