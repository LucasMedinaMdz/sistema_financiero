# 🧾 Aplicación de Consola - Sistema Financiero Sol Andino

Esta es una aplicación de consola en Java que se conecta a una base de datos MySQL.  
La configuración de la conexión a la base de datos se realiza mediante un archivo de propiedades externo.

---

## 🔧 Requisitos

- Java 21 o superior (puedes descargarlo de [oracle.com](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html))
- Maven (para gestión de dependencias y compilación)
- MySQL instalado y en ejecución
- MySQL Workbench (opcional, para administrar la base de datos visualmente)
- IntelliJ IDEA (recomendado, aunque puedes usar cualquier IDE)

---

## ⚙️ Configuración

1. Ve a la carpeta `src/main/resources/`.

2. Verás un archivo llamado:  
   `config.properties.ejemplo`

3. **Renombra** este archivo a: **"config.properties"**

   Para hacerlo en IntelliJ IDEA:
   - Haz clic derecho sobre `config.properties.ejemplo` → **Refactor** → **Rename** 
   - O presiona **Shift + F6** y cambiar el nombre
   - Destildar las opciones *Search for references* y *Search comments and strings* → **Refactor**

5. Abre `config.properties` y completa tus datos de conexión:

   ```properties
   db.url=jdbc:mysql://localhost:3306/sistema_financiero?useSSL=false&serverTimezone=UTC
   db.usuario=TU_USUARIO
   db.contrasena=TU_CONTRASENA
   ```
---

## 📥 Importar la Base de Datos

Este proyecto incluye un archivo SQL con la estructura de la base de datos y datos mínimos de prueba para que puedas ejecutarlo directamente sin configuraciones manuales.

### 📂 Ubicación del archivo

El archivo SQL se encuentra en el siguiente link: https://drive.google.com/file/d/1UtAF7o70kEAY_gnrq-9X9Nz4bV3a8g8p/view

### 🧰 Cómo importarlo

1. Abre **MySQL Workbench**.
2. Conéctate a tu servidor.
3. En **MySQL Workbench** → Haga clic en File → Open SQL Script → Seleccione el archivo **sistema_financiero.sql**
4. Ejecuta el script completo. Esto:
   - Creará la base de datos `sistema_financiero`
   - Generará todas las tablas necesarias
   - Insertará algunos datos de prueba

🔎 Asegúrate de que el nombre de la base de datos (`sistema_financiero`) coincida con el valor configurado en tu archivo `config.properties`.

> 💡 Si prefieres iniciar con la base vacía, puedes eliminar las sentencias `INSERT` del archivo SQL antes de ejecutarlo.

---

## 📦 Estructura del Proyecto

```plaintext
src/
 └── main/
     ├── java/
     │    └── ConexionDB.java
     └── resources/
          └── config.properties
```

---

## 📁 .gitignore

El archivo `config.properties` está ignorado por Git para evitar subir credenciales sensibles.  
Esto se logra con la siguiente línea en `.gitignore`:

```gitignore
src/main/resources/config.properties
```

> 🔐 **Importante:** Asegúrate de **no subir el archivo `config.properties` a ningún repositorio público**, ya que contiene credenciales sensibles como tu usuario y contraseña de la base de datos.  
> Este archivo ya está incluido en `.gitignore` para ayudarte a evitarlo.
