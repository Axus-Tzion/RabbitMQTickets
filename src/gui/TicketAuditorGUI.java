package gui;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import model.Ticket;
import rabbit.RabbitManager;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TicketAuditorGUI {
    private static List<Ticket> tickets = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("Auditoría de Tickets");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Componentes
        JTabbedPane tabbedPane = new JTabbedPane();
        JTextArea todosArea = new JTextArea();
        JTextArea pendientesArea = new JTextArea();
        JTextArea resueltosArea = new JTextArea();

        // Configurar áreas
        todosArea.setEditable(false);
        pendientesArea.setEditable(false);
        resueltosArea.setEditable(false);

        // Agregar pestañas
        tabbedPane.addTab("Todos", new JScrollPane(todosArea));
        tabbedPane.addTab("Pendientes", new JScrollPane(pendientesArea));
        tabbedPane.addTab("Resueltos", new JScrollPane(resueltosArea));

        frame.add(tabbedPane);

        // Conexión RabbitMQ
        Connection connection = RabbitManager.getConnection();
        Channel channel = connection.createChannel();

        // Configurar consumidor
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String mensaje = new String(delivery.getBody(), "UTF-8");
            Gson gson = new Gson();
            Ticket nuevoTicket = gson.fromJson(mensaje, Ticket.class);

            // Actualizar lista
            synchronized (tickets) {
                tickets.removeIf(t -> t.getId().equals(nuevoTicket.getId()));
                tickets.add(nuevoTicket);
            }

            // Actualizar UI
            SwingUtilities.invokeLater(() -> actualizarUI(todosArea, pendientesArea, resueltosArea));
        };

        channel.basicConsume(RabbitManager.COLA_AUDIT, true, deliverCallback, consumerTag -> {});

        // Timer para actualización periódica
        Timer timer = new Timer(2000, e -> actualizarUI(todosArea, pendientesArea, resueltosArea));
        timer.start();

        frame.setVisible(true);
    }

    private static void actualizarUI(JTextArea todos, JTextArea pendientes, JTextArea resueltos) {
        synchronized (tickets) {
            // Construir textos
            StringBuilder todosText = new StringBuilder();
            StringBuilder pendientesText = new StringBuilder();
            StringBuilder resueltosText = new StringBuilder();

            for (Ticket t : tickets) {
                String ticketStr = formatTicket(t);
                todosText.append(ticketStr).append("\n\n");

                if (t.isSolucionado()) {
                    resueltosText.append(ticketStr).append("\n\n");
                } else {
                    pendientesText.append(ticketStr).append("\n\n");
                }
            }

            // Actualizar áreas
            todos.setText(todosText.toString());
            pendientes.setText(pendientesText.toString());
            resueltos.setText(resueltosText.toString());
        }
    }

    private static String formatTicket(Ticket t) {
        return String.format(
                "ID: %s\nTipo: %s\nDescripción: %s\nEstado: %s\nTécnico: %s",
                t.getId(),
                t.getTipo(),
                t.getDescripcion(),
                t.isSolucionado() ? "RESUELTO" : "PENDIENTE",
                t.getTecnicoAsignado().isEmpty() ? "No asignado" : t.getTecnicoAsignado()
        );
    }
}