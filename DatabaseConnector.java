import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {

    // URL de conexión a la base de datos SQL Server
    private static final String JDBC_URL = "jdbc:sqlserver://localhost:1433;databaseName=WordleGameDB;encrypt=false;trustServerCertificate=true;";
   
    private static final String USERNAME = "sa5";
    private static final String PASSWORD = "1234";

    // Método para obtener la conexión a la base de datos
    public static Connection getConnection() throws SQLException {
        try {
            // Cargar el driver JDBC para SQL Server
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC no encontrado.");
            e.printStackTrace();
            throw new SQLException("No se pudo cargar el driver JDBC.");
        }
        // Establecer la conexión
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }

    // Método principal para probar la conexión
    public static void main(String[] args) {
        try {
            // Obtener la conexión
            Connection connection = getConnection();
            System.out.println("Conexión establecida con éxito.");
            // Cerrar la conexión
            connection.close();
        } catch (SQLException e) {
            System.err.println("Error al conectar a la base de datos.");
            e.printStackTrace();
        }
    }
}
