public class CheckJDBC {
    public static void main(String[] args) {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            System.out.println("Biblioteca JDBC de SQL Server está disponible.");
        } catch (ClassNotFoundException e) {
            System.out.println("Biblioteca JDBC de SQL Server no está disponible.");
        }
    }
}
