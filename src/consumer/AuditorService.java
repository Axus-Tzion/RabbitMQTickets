package consumer;

import com.rabbitmq.client.*;
import rabbit.RabbitManager;

public class AuditorService {
    public static void main(String[] args) throws Exception {
        Connection connection = RabbitManager.getConnection();
        Channel channel = connection.createChannel();

        System.out.println("[Auditoría] Esperando tickets...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String mensaje = new String(delivery.getBody(), "UTF-8");
            System.out.println("[Auditoría] Registrando ticket: " + mensaje);

            // Pequeño retraso para visualización en RabbitMQ
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Aquí iría el código para guardar en BD
        };

        channel.basicConsume(RabbitManager.COLA_AUDIT, true, deliverCallback, consumerTag -> {});
    }
}