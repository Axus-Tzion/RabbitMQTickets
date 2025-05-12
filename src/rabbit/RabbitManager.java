package rabbit;
import com.rabbitmq.client.*;

public class RabbitManager {
    public static final String HOST = "localhost";
    public static final String EXCHANGE_DIRECT = "tickets_direct";
    public static final String EXCHANGE_FANOUT = "tickets_fanout";
    public static final String COLA_HARDWARE = "hardware_cola";
    public static final String COLA_SOFTWARE = "software_cola";
    public static final String COLA_AUDIT = "audit_cola";

    public static Connection getConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        return factory.newConnection();
    }

    public static void setup() throws Exception {
        try (Connection connection = getConnection(); Channel channel = connection.createChannel()) {
            // Configuración de exchanges (duraderos)
            channel.exchangeDeclare(EXCHANGE_DIRECT, BuiltinExchangeType.DIRECT, true);
            channel.exchangeDeclare(EXCHANGE_FANOUT, BuiltinExchangeType.FANOUT, true);

            // Configuración de colas (duraderas)
            channel.queueDeclare(COLA_HARDWARE, true, false, false, null);
            channel.queueDeclare(COLA_SOFTWARE, true, false, false, null);
            channel.queueDeclare(COLA_AUDIT, true, false, false, null);

            // Bindings
            channel.queueBind(COLA_HARDWARE, EXCHANGE_DIRECT, "hardware");
            channel.queueBind(COLA_SOFTWARE, EXCHANGE_DIRECT, "software");
            channel.queueBind(COLA_AUDIT, EXCHANGE_FANOUT, "");
        }
    }
}