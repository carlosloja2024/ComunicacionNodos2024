import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;


public class Servidor {
    private static final int PORT = 26;
    private static Connection connection;

    public static void main(String[] args) {
        try {
            // Cargar el controlador JDBC
           Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            // Establecer conexión con la base de datos
           // String connectionUrl = "jdbc:sqlserver://192.168.1.5:1433;databaseName=WordleGameDB;user=sa5;password=28199612Ab@;encrypt=false;trustServerCertificate=true";
           String connectionUrl = "jdbc:sqlserver://localhost:1433;databaseName=WordleGameDB;user=sa5;password=1234;encrypt=false;trustServerCertificate=true";
                                  
   
            connection = DriverManager.getConnection(connectionUrl);

            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor escuchando en el puerto " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private DataInputStream input;
        private DataOutputStream output;
        private int idJuego;
        private int idJugador;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                input = new DataInputStream(clientSocket.getInputStream());
                output = new DataOutputStream(clientSocket.getOutputStream());

                String usuario = input.readUTF();
                String contrasena = input.readUTF();

                if (autenticarUsuario(usuario, contrasena)) {
                    idJugador = obtenerIdJugador(usuario);
                    int idMateria = obtenerIdMateriaPorDefecto(); // Obtenemos el id_materia por defecto
                    idJuego = iniciarJuego(idJugador, idMateria);
                    output.writeUTF("Autenticación exitosa. ID del juego: " + idJuego);

                    boolean enJuego = true;
                    while (enJuego) {
                        String accion = input.readUTF();

                        switch (accion) {
                            case "PREGUNTA":
                                enviarPregunta();
                                break;
                            case "RESPUESTA":
                                String respuesta = input.readUTF();
                                procesarRespuesta(respuesta);
                                break;
                            case "RETIRARSE":
                                guardarEstadoJuego();
                                enJuego = false;
                                break;
                            case "FINALIZAR":
                                guardarEstadoJuego();
                                calificarJuego();
                                enJuego = false;
                                break;
                            case "SESIONES_GUARDADAS":
                                String user = input.readUTF();
                                enviarSesionesGuardadas(user);
                                break;
                            default:
                                output.writeUTF("Acción no reconocida.");
                        }
                    }
                } else {
                    output.writeUTF("Autenticación fallida.");
                }

                clientSocket.close();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }

        private boolean autenticarUsuario(String usuario, String contrasena) throws SQLException {
            String query = "SELECT COUNT(*) FROM Jugadores WHERE email = ? AND contraseña = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, usuario);
            statement.setString(2, contrasena);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1) > 0;
        }

        private int obtenerIdJugador(String usuario) throws SQLException {
            String query = "SELECT id_jugador FROM Jugadores WHERE email = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, usuario);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return -1;
        }

        private int obtenerIdMateriaPorDefecto() throws SQLException {
            String query = "SELECT TOP 1 id_materia FROM Materias";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return -1; // En caso de que no haya materias, retornar -1 como error
        }

        private int iniciarJuego(int idJugador, int idMateria) throws SQLException {
            String query = "INSERT INTO Partidas (id_jugador, materia, estado, fechaInicio, id_materia) VALUES (?, 'General', 'En curso', GETDATE(), ?);";
            PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, idJugador);
            statement.setInt(2, idMateria);
            statement.executeUpdate();
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return -1;
        }

        private void enviarPregunta() throws SQLException, IOException {
            String query = "SELECT TOP 1 id_pregunta, texto FROM Preguntas ORDER BY NEWID()";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int idPregunta = resultSet.getInt("id_pregunta");
                String textoPregunta = resultSet.getString("texto");
                output.writeUTF("Pregunta: " + textoPregunta);

                // Enviar opciones de respuesta
                String queryRespuestas = "SELECT texto FROM Respuestas WHERE id_pregunta = ?";
                PreparedStatement statementRespuestas = connection.prepareStatement(queryRespuestas);
                statementRespuestas.setInt(1, idPregunta);
                ResultSet resultSetRespuestas = statementRespuestas.executeQuery();
                while (resultSetRespuestas.next()) {
                    output.writeUTF(resultSetRespuestas.getString("texto"));
                }

                // Guardar la pregunta enviada en el estado del juego
                guardarPreguntaEnJuego(idPregunta);
            }
        }

        private void procesarRespuesta(String respuesta) throws SQLException, IOException {
            // Obtener la última pregunta enviada para el juego actual
            int idPregunta = obtenerUltimaPreguntaEnviada();

            // Verificar si la respuesta es correcta
            String query = "SELECT correcta FROM Respuestas WHERE id_pregunta = ? AND texto = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, idPregunta);
            statement.setString(2, respuesta);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                boolean correcta = resultSet.getBoolean("correcta");
                if (correcta) {
                    // Actualizar puntuación
                    actualizarPuntuacion(10);
                    output.writeUTF("Respuesta correcta. Puntuación +10.");
                } else {
                    output.writeUTF("Respuesta incorrecta.");
                }
            }
        }

        private void actualizarPuntuacion(int puntos) throws SQLException {
            String query = "UPDATE Partidas SET puntuacion = puntuacion + ? WHERE id_partida = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, puntos);
            statement.setInt(2, idJuego);
            statement.executeUpdate();
        }

        private void guardarEstadoJuego() throws SQLException {
            String query = "UPDATE Partidas SET estado = 'Guardado', fechaFin = GETDATE() WHERE id_partida = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, idJuego);
            statement.executeUpdate();
        }

        private void calificarJuego() throws SQLException {
            String query = "UPDATE Partidas SET estado = 'Finalizado', fechaFin = GETDATE() WHERE id_partida = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, idJuego);
            statement.executeUpdate();
        }

        private void guardarPreguntaEnJuego(int idPregunta) throws SQLException {
            String query = "INSERT INTO PartidasPreguntas (id_partida, id_pregunta) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, idJuego);
            statement.setInt(2, idPregunta);
            statement.executeUpdate();
        }

        private int obtenerUltimaPreguntaEnviada() throws SQLException {
            String query = "SELECT TOP 1 id_pregunta FROM PartidasPreguntas WHERE id_partida = ? ORDER BY id DESC";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, idJuego);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id_pregunta");
            }
            return -1;
        }

        private void enviarSesionesGuardadas(String usuario) throws SQLException, IOException {
            int idJugador = obtenerIdJugador(usuario);
            String query = "SELECT id_partida, puntuacion FROM Partidas WHERE id_jugador = ? AND estado = 'Guardado'";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, idJugador);
            ResultSet resultSet = statement.executeQuery();

            List<String> sesiones = new ArrayList<>();
            while (resultSet.next()) {
                int idPartida = resultSet.getInt("id_partida");
                int puntuacion = resultSet.getInt("puntuacion");
                sesiones.add("ID: " + idPartida + ", Puntaje: " + puntuacion);
            }

            output.writeInt(sesiones.size());
            for (String sesion : sesiones) {
                output.writeUTF(sesion);
            }
        }
    }
}
