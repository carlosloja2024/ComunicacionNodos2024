import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class Cliente extends JFrame {
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private int idJuego;
    private int puntaje;

    private JTextField usuarioField;
    private JPasswordField contrasenaField;
    private JTextArea preguntaArea;
    private JRadioButton[] respuestasButtons;
    private ButtonGroup respuestasGroup;
    private JLabel puntajeLabel;
    private JButton enviarButton, guardarSalirButton, salirSinGuardarButton;

    private JRadioButton nuevoJuegoButton;
    private JRadioButton juegoGuardadoButton;
    private JList<String> sesionesList;
    private DefaultListModel<String> sesionesListModel;

    public Cliente() {
        // Configuración de la interfaz gráfica
        setTitle("Cliente Wordle");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel de autenticación
        JPanel loginPanel = new JPanel(new GridLayout(5, 2));
        usuarioField = new JTextField();
        contrasenaField = new JPasswordField();

        nuevoJuegoButton = new JRadioButton("Nuevo Juego");
        juegoGuardadoButton = new JRadioButton("Juego Guardado");
        ButtonGroup tipoJuegoGroup = new ButtonGroup();
        tipoJuegoGroup.add(nuevoJuegoButton);
        tipoJuegoGroup.add(juegoGuardadoButton);

        nuevoJuegoButton.setSelected(true);

        JButton loginButton = new JButton("Iniciar Sesión");
        loginButton.addActionListener(e -> autenticarUsuario());

        loginPanel.add(new JLabel("Usuario:"));
        loginPanel.add(usuarioField);
        loginPanel.add(new JLabel("Contraseña:"));
        loginPanel.add(contrasenaField);
        loginPanel.add(nuevoJuegoButton);
        loginPanel.add(juegoGuardadoButton);
        loginPanel.add(new JLabel(""));
        loginPanel.add(loginButton);

        // Panel de preguntas y respuestas
        JPanel preguntaPanel = new JPanel(new BorderLayout());
        preguntaArea = new JTextArea(4, 20);
        preguntaArea.setEditable(false);
        preguntaPanel.add(new JScrollPane(preguntaArea), BorderLayout.NORTH);

        JPanel respuestasPanel = new JPanel(new GridLayout(7, 1));
        respuestasButtons = new JRadioButton[4];
        respuestasGroup = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            respuestasButtons[i] = new JRadioButton();
            respuestasGroup.add(respuestasButtons[i]);
            respuestasPanel.add(respuestasButtons[i]);
        }
        preguntaPanel.add(respuestasPanel, BorderLayout.CENTER);

        // Panel de puntaje y botones
        JPanel controlPanel = new JPanel(new GridLayout(2, 1));
        puntajeLabel = new JLabel("Puntaje: 0");
        enviarButton = new JButton("Enviar Respuesta");
        enviarButton.addActionListener(e -> enviarRespuesta());
        guardarSalirButton = new JButton("Guardar y Salir");
        guardarSalirButton.addActionListener(e -> guardarYSalir());
        salirSinGuardarButton = new JButton("Salir sin Guardar");
        salirSinGuardarButton.addActionListener(e -> salirSinGuardar());

        JPanel botonesPanel = new JPanel(new GridLayout(1, 3));
        botonesPanel.add(enviarButton);
        botonesPanel.add(guardarSalirButton);
        botonesPanel.add(salirSinGuardarButton);

        controlPanel.add(puntajeLabel);
        controlPanel.add(botonesPanel);

        // Panel de sesiones guardadas
        JPanel sesionesPanel = new JPanel(new BorderLayout());
        sesionesListModel = new DefaultListModel<>();
        sesionesList = new JList<>(sesionesListModel);
        sesionesPanel.add(new JScrollPane(sesionesList), BorderLayout.CENTER);

        // Agregar paneles al frame
        add(loginPanel, BorderLayout.NORTH);
        add(preguntaPanel, BorderLayout.CENTER);
        add(sesionesPanel, BorderLayout.EAST);
        add(controlPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void autenticarUsuario() {
        try {
            socket = new Socket("localhost", 26); // Ajusta la IP si es necesario
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            String usuario = usuarioField.getText();
            String contrasena = new String(contrasenaField.getPassword());

            output.writeUTF(usuario);
            output.writeUTF(contrasena);

            String respuesta = input.readUTF();
            if (respuesta.startsWith("Autenticación exitosa")) {
                idJuego = Integer.parseInt(respuesta.split(": ")[1]);
                JOptionPane.showMessageDialog(this, "Autenticación exitosa. ID del juego: " + idJuego);

                if (juegoGuardadoButton.isSelected()) {
                    cargarSesionesGuardadas(usuario);
                } else {
                    obtenerPregunta();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Autenticación fallida.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void obtenerPregunta() {
        try {
            output.writeUTF("PREGUNTA");
            String pregunta = input.readUTF();
            preguntaArea.setText(pregunta);

            for (int i = 0; i < 4; i++) {
                respuestasButtons[i].setText(input.readUTF());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enviarRespuesta() {
        try {
            for (int i = 0; i < 4; i++) {
                if (respuestasButtons[i].isSelected()) {
                    output.writeUTF("RESPUESTA");
                    output.writeUTF(respuestasButtons[i].getText());
                    String resultado = input.readUTF();
                    JOptionPane.showMessageDialog(this, resultado);

                    if (resultado.startsWith("Respuesta correcta")) {
                        puntaje += 10;
                    }
                    puntajeLabel.setText("Puntaje: " + puntaje);
                    obtenerPregunta();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarSesionesGuardadas(String usuario) {
        try {
            // Limpiar la lista de sesiones guardadas
            sesionesListModel.clear();

            output.writeUTF("SESIONES_GUARDADAS");
            output.writeUTF(usuario);

            // Leer el número de sesiones guardadas
            int numeroSesiones = input.readInt();
            for (int i = 0; i < numeroSesiones; i++) {
                String sesion = input.readUTF();
                sesionesListModel.addElement(sesion);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void guardarYSalir() {
        try {
            output.writeUTF("RETIRARSE");
            cerrarConexion();
            JOptionPane.showMessageDialog(this, "Estado del juego guardado.");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void salirSinGuardar() {
        try {
            output.writeUTF("FINALIZAR");
            cerrarConexion();
            JOptionPane.showMessageDialog(this, "Juego finalizado sin guardar.");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cerrarConexion() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Cliente::new);
    }
}
