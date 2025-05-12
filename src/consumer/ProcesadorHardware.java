package consumer;

import com.rabbitmq.client.*;
import rabbit.RabbitManager;

public class ProcesadorHardware {
    public static void main(String[] args) throws Exception {
        Connection connection = RabbitManager.getConnection();
        Channel channel = connection.createChannel();

        // Configurar prefetch count (1 mensaje a la vez)
        channel.basicQos(1);

        System.out.println("[Hardware] Esperando tickets...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String mensaje = new String(delivery.getBody(), "UTF-8");
            System.out.println("[Hardware] Ticket recibido (pero no confirmado aún): " + mensaje);

            // Simular procesamiento (5 segundos para que puedas verlo en RabbitMQ)
            try {
                Thread.sleep(5000);
                System.out.println("[Hardware] Procesamiento completado, confirmando...");
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                System.err.println("[Hardware] Error al procesar: " + e.getMessage());
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
        };

        // Auto-ack en false para control manual
        channel.basicConsume(RabbitManager.COLA_HARDWARE, false, deliverCallback, consumerTag -> {});
    }
}