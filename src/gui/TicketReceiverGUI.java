package gui;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import model.Ticket;
import rabbit.RabbitManager;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class TicketReceiverGUI {
    private static final String TECNICO_ID = "Tecnico-" + System.currentTimeMillis();
    private static Ticket ticketActual = null;
    private static Channel channel;
    private static String consumerTag;
    private static long deliveryTag = -1;

    public static void main(String[] args) {
        // Configuración de la interfaz
        JFrame frame = new JFrame("Sistema de Tickets - Técnico");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        // Componentes
        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"hardware", "software"});
        JButton conectarBtn = new JButton("Conectar");
        JTextArea ticketArea = new JTextArea();
        JButton resolverBtn = new JButton("Marcar como Resuelto");
        resolverBtn.setEnabled(false);

        // Panel superior
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Especialidad:"));
        topPanel.add(tipoCombo);
        topPanel.add(conectarBtn);

        // Área central
        ticketArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(ticketArea);

        // Agregar componentes
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(resolverBtn, BorderLayout.SOUTH);

        // Manejador de conexión
        conectarBtn.addActionListener(e -> {
            String tipo = (String) tipoCombo.getSelectedItem();
            try {
                Connection connection = RabbitManager.getConnection();
                channel = connection.createChannel();

                // Configurar QoS para controlar flujo (1 mensaje a la vez)
                channel.basicQos(1);

                String queueName = tipo.equals("hardware") ? RabbitManager.COLA_HARDWARE : RabbitManager.COLA_SOFTWARE;

                DeliverCallback deliverCallback = (tag, delivery) -> {
                    deliveryTag = delivery.getEnvelope().getDeliveryTag();
                    String mensaje = new String(delivery.getBody(), "UTF-8");
                    Gson gson = new Gson();
                    ticketActual = gson.fromJson(mensaje, Ticket.class);
                    ticketActual.setTecnicoAsignado(TECNICO_ID);

                    SwingUtilities.invokeLater(() -> {
                        ticketArea.setText("=== TICKET ASIGNADO ===\n" +
                                "ID: " + ticketActual.getId() + "\n" +
                                "Tipo: " + ticketActual.getTipo() + "\n" +
                                "Descripción: " + ticketActual.getDescripcion());
                        resolverBtn.setEnabled(true);
                    });
                };

                // Consumir mensajes (autoAck = false)
                consumerTag = channel.basicConsume(queueName, false, deliverCallback, ct -> {});

                tipoCombo.setEnabled(false);
                conectarBtn.setEnabled(false);
                ticketArea.setText("Conectado como técnico de " + tipo + "\nEsperando tickets...");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error al conectar: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // Manejador de resolución
        resolverBtn.addActionListener(e -> {
            if (ticketActual != null && deliveryTag != -1) {
                try {
                    // 1. Marcar como resuelto
                    ticketActual.setSolucionado(true);

                    // 2. Enviar a cola de audit
                    Gson gson = new Gson();
                    String mensaje = gson.toJson(ticketActual);
                    channel.basicPublish(RabbitManager.EXCHANGE_FANOUT, "", null, mensaje.getBytes());

                    // 3. Confirmar procesamiento
                    channel.basicAck(deliveryTag, false);

                    // 4. Resetear UI
                    SwingUtilities.invokeLater(() -> {
                        ticketArea.setText("Ticket marcado como resuelto.\nEsperando nuevo ticket...");
                        resolverBtn.setEnabled(false);
                    });

                    // 5. Resetear variables
                    ticketActual = null;
                    deliveryTag = -1;

                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error al procesar ticket: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        frame.setVisible(true);
    }
}