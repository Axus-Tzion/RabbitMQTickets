package gui;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import model.Ticket;
import rabbit.RabbitManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TicketSenderGUI {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Generador de Tickets");
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Panel de entrada
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        JComboBox<String> tipoCombo = new JComboBox<>(new String[]{"hardware", "software"});
        JTextArea descripcionArea = new JTextArea(5, 20);
        JButton enviarBtn = new JButton("Enviar Ticket");

        inputPanel.add(new JLabel("Tipo de Ticket:"));
        inputPanel.add(tipoCombo);
        inputPanel.add(new JLabel("Descripción:"));
        inputPanel.add(new JScrollPane(descripcionArea));
        inputPanel.add(new JLabel(""));
        inputPanel.add(enviarBtn);

        // Área de estado
        JTextArea estadoArea = new JTextArea();
        estadoArea.setEditable(false);
        estadoArea.setBackground(frame.getBackground());

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(estadoArea), BorderLayout.CENTER);

        // Manejador de envío
        enviarBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String tipo = (String) tipoCombo.getSelectedItem();
                String descripcion = descripcionArea.getText();

                if (descripcion.trim().isEmpty()) {
                    estadoArea.setText("Error: La descripción no puede estar vacía");
                    return;
                }

                try (Connection connection = RabbitManager.getConnection();
                     Channel channel = connection.createChannel()) {

                    // Crear y enviar ticket
                    Ticket ticket = new Ticket(tipo, descripcion);
                    Gson gson = new Gson();
                    String mensaje = gson.toJson(ticket);

                    // Enviar al exchange direct (para técnicos)
                    channel.basicPublish(RabbitManager.EXCHANGE_DIRECT, tipo, null, mensaje.getBytes());

                    // Enviar también al fanout (para auditoría)
                    channel.basicPublish(RabbitManager.EXCHANGE_FANOUT, "", null, mensaje.getBytes());

                    estadoArea.setText(String.format(
                            "Ticket enviado con éxito!\nID: %s\nTipo: %s\nDescripción: %s",
                            ticket.getId(), ticket.getTipo(), ticket.getDescripcion()
                    ));

                    descripcionArea.setText("");

                } catch (Exception ex) {
                    estadoArea.setText("Error al enviar ticket: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        frame.setVisible(true);
    }
}