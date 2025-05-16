package gui;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import model.Ticket;
import rabbit.RabbitManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import com.formdev.flatlaf.FlatLightLaf;

public class TicketReceiverGUI {
    private static final String TECNICO_ID = "🧑‍🔧 Técnico-" + System.currentTimeMillis();
    private static Ticket ticketActual = null;
    private static Channel channel;
    private static String consumerTag;
    private static long deliveryTag = -1;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("🛠 Sistema de Tickets - Técnico");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 420);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        // Componentes
        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"Hardware", "Software"});
        JButton conectarBtn = new JButton("🔌 Conectar");
        JTextArea ticketArea = new JTextArea();
        JButton resolverBtn = new JButton("✅ Marcar como Resuelto");
        resolverBtn.setEnabled(false);

        // Panel superior
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder("🎛 Configuración"));
        topPanel.add(new JLabel("Especialidad:"));
        topPanel.add(tipoCombo);
        topPanel.add(conectarBtn);

        ticketArea.setEditable(false);
        ticketArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        ticketArea.setLineWrap(true);
        ticketArea.setWrapStyleWord(true);
        ticketArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(ticketArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("📋 Detalles del Ticket"));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.add(resolverBtn);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        conectarBtn.addActionListener(e -> {
            String tipo = (String) tipoCombo.getSelectedItem();
            try {
                Connection connection = RabbitManager.getConnection();
                channel = connection.createChannel();
                channel.basicQos(1);

                String queueName = tipo.equals("Hardware") ? RabbitManager.COLA_HARDWARE : RabbitManager.COLA_SOFTWARE;

                DeliverCallback deliverCallback = (tag, delivery) -> {
                    deliveryTag = delivery.getEnvelope().getDeliveryTag();
                    String mensaje = new String(delivery.getBody(), "UTF-8");
                    Gson gson = new Gson();
                    ticketActual = gson.fromJson(mensaje, Ticket.class);
                    ticketActual.setTecnicoAsignado(TECNICO_ID);

                    SwingUtilities.invokeLater(() -> {
                        ticketArea.setText("🆔 ID: " + ticketActual.getId() + "\n"
                                + "📄 Tipo: " + ticketActual.getTipo() + "\n"
                                + "📝 Descripción:\n" + ticketActual.getDescripcion());
                        resolverBtn.setEnabled(true);
                    });
                };

                consumerTag = channel.basicConsume(queueName, false, deliverCallback, ct -> {});

                tipoCombo.setEnabled(false);
                conectarBtn.setEnabled(false);
                ticketArea.setText("✅ Conectado como técnico de *" + tipo + "*.\n⏳ Esperando tickets...");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "❌ Error al conectar: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        resolverBtn.addActionListener(e -> {
            if (ticketActual != null && deliveryTag != -1) {
                try {
                    ticketActual.setSolucionado(true);
                    Gson gson = new Gson();
                    String mensaje = gson.toJson(ticketActual);
                    channel.basicPublish(RabbitManager.EXCHANGE_FANOUT, "", null, mensaje.getBytes());
                    channel.basicAck(deliveryTag, false);

                    SwingUtilities.invokeLater(() -> {
                        ticketArea.setText("🎉 Ticket resuelto.\n⏳ Esperando nuevo ticket...");
                        resolverBtn.setEnabled(false);
                    });

                    ticketActual = null;
                    deliveryTag = -1;

                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "❌ Error al procesar ticket: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        frame.setVisible(true);
    }
}
