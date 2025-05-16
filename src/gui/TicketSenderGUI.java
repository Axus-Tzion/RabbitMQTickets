package gui;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import model.Ticket;
import rabbit.RabbitManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TicketSenderGUI {

    public static void main(String[] args) {
        try {
            // Configurar Look and Feel moderno (FlatLaf)
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (Exception ex) {
            try {
                // Fallback a Nimbus si FlatLaf no está disponible
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Configuración de la ventana principal
        JFrame frame = new JFrame("Generador de Tickets");
        frame.setSize(550, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null); // Centrar ventana

        // Panel principal con márgenes
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);

        // Panel de entrada con diseño mejorado
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Componentes con estilo moderno
        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"hardware", "software"});
        tipoCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JTextArea descripcionArea = new JTextArea(5, 20);
        descripcionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descripcionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JButton enviarBtn = new JButton("Enviar Ticket");
        enviarBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        enviarBtn.setBackground(new Color(70, 130, 180));
        enviarBtn.setForeground(Color.WHITE);
        enviarBtn.setFocusPainted(false);

        // Diseño con GridBagLayout para mejor organización
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Tipo de Ticket:"), gbc);

        gbc.gridx = 1;
        inputPanel.add(tipoCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Descripción:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridheight = 2;
        inputPanel.add(new JScrollPane(descripcionArea), gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        inputPanel.add(enviarBtn, gbc);

        JTextArea estadoArea = new JTextArea();
        estadoArea.setEditable(false);
        estadoArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        estadoArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Estado del envío"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        estadoArea.setBackground(new Color(245, 245, 245));

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(estadoArea), BorderLayout.CENTER);
        frame.add(mainPanel);

        enviarBtn.addActionListener(e -> {
            String tipo = (String) tipoCombo.getSelectedItem();
            String descripcion = descripcionArea.getText().trim();

            if (descripcion.isEmpty()) {
                estadoArea.setText("❌ Error: La descripción no puede estar vacía.");
                return;
            }

            try (Connection connection = RabbitManager.getConnection();
                 Channel channel = connection.createChannel()) {

                Ticket ticket = new Ticket(tipo, descripcion);
                String mensaje = new Gson().toJson(ticket);

                channel.basicPublish(RabbitManager.EXCHANGE_DIRECT, tipo, null, mensaje.getBytes());
                channel.basicPublish(RabbitManager.EXCHANGE_FANOUT, "", null, mensaje.getBytes());

                estadoArea.setText(String.format(
                        "✅ Ticket enviado con éxito!\n\nID: %s\nTipo: %s\nDescripción: %s",
                        ticket.getId(), ticket.getTipo(), ticket.getDescripcion()
                ));

                descripcionArea.setText("");

            } catch (Exception ex) {
                estadoArea.setText("❌ Error al enviar ticket: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        frame.setVisible(true);
    }
}