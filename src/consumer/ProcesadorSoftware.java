package consumer;

import com.rabbitmq.client.*;
import rabbit.RabbitManager;

public class ProcesadorSoftware {
    public static void main(String[] args) throws Exception {
        Connection connection = RabbitManager.getConnection();
        Channel channel = connection.createChannel();

        channel.basicQos(1);

        System.out.println("[Software] Esperando tickets...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String mensaje = new String(delivery.getBody(), "UTF-8");
            System.out.println("[Software] Ticket recibido (pero no confirmado aún): " + mensaje);

            try {
                Thread.sleep(5000);
                System.out.println("[Software] Procesamiento completado, confirmando...");
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                System.err.println("[Software] Error al procesar: " + e.getMessage());
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
        };

        channel.basicConsume(RabbitManager.COLA_SOFTWARE, false, deliverCallback, consumerTag -> {});
    }
}