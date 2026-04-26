package com.grupoamarillo.ej3dlq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@SpringBootApplication
public class Ej3DlqApplication {

    // Exchange/cola principal donde llegan los mensajes "normales" de trabajo.
    public static final String MAIN_EXCHANGE = "tp3.ej3.main.exchange";
    public static final String MAIN_QUEUE = "tp3.ej3.main.queue";
    public static final String MAIN_ROUTING_KEY = "tp3.ej3.main";

    // Dead Letter Exchange y Dead Letter Queue para redirigir mensajes fallidos.
    public static final String DLX_EXCHANGE = "tp3.ej3.dlx.exchange";
    public static final String DLQ_QUEUE = "tp3.ej3.dlq.queue";
    public static final String DLQ_ROUTING_KEY = "tp3.ej3.dlq";

    @Value("${producer.delay-ms:700}")
    private long delayMs;

    public static void main(String[] args) {
        SpringApplication.run(Ej3DlqApplication.class, args);
    }

    // Exchange principal tipo direct.
    @Bean
    DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE, true, false);
    }

    // Dead Letter Exchange tipo direct.
    @Bean
    DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    Queue mainQueue() {
        // La cola principal define DLX + routing key de DLQ.
        // Si el consumidor hace nack con requeue=false, RabbitMQ moverá
        // automáticamente el mensaje a este DLX.
        return QueueBuilder.durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue dlqQueue() {
        // Cola de mensajes fallidos.
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    Binding mainBinding(Queue mainQueue, DirectExchange mainExchange) {
        // Enlace de la cola principal al exchange principal.
        return BindingBuilder.bind(mainQueue).to(mainExchange).with(MAIN_ROUTING_KEY);
    }

    @Bean
    Binding dlqBinding(Queue dlqQueue, DirectExchange dlxExchange) {
        // Enlace de la DLQ al DLX.
        return BindingBuilder.bind(dlqQueue).to(dlxExchange).with(DLQ_ROUTING_KEY);
    }

    @Bean
    SimpleRabbitListenerContainerFactory manualAckFactory(ConnectionFactory connectionFactory) {
        // Ack manual para decidir explícitamente:
        // - ack si el procesamiento fue correcto
        // - nack si queremos mandar el mensaje a DLQ
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        return factory;
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    CommandLineRunner sendMessages(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        // Publicador de prueba: alterna mensajes OK y fallidos (error=true).
        // Esto permite verificar visualmente que los fallidos terminen en DLQ.
        return args -> {
            for (int i = 1; i <= 6; i++) {
                boolean hasError = i % 2 == 0;
                Map<String, Object> payload = Map.of(
                        "id", i,
                        "task", "Procesar item " + i,
                        "error", hasError
                );

                String json = objectMapper.writeValueAsString(payload);
                rabbitTemplate.convertAndSend(MAIN_EXCHANGE, MAIN_ROUTING_KEY, json);
                System.out.println("Enviado a cola principal: " + json);
                Thread.sleep(delayMs);
            }
        };
    }

    @Component
    static class MainQueueConsumer {
        private final ObjectMapper objectMapper;

        MainQueueConsumer(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @RabbitListener(queues = MAIN_QUEUE, containerFactory = "manualAckFactory")
        public void consumeMain(Message message, Channel channel) throws Exception {
            long tag = message.getMessageProperties().getDeliveryTag();
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(body, new TypeReference<>() {});

            boolean hasError = Boolean.TRUE.equals(payload.get("error"));
            if (hasError) {
                // Rechazo explícito sin reencolar (requeue=false):
                // RabbitMQ enruta el mensaje al DLX configurado en la cola.
                System.out.println("MAIN CONSUMER -> mensaje con error, se rechaza y va a DLQ: " + body);
                channel.basicNack(tag, false, false);
                return;
            }

            // Mensaje válido: se confirma para eliminarlo de la cola principal.
            System.out.println("MAIN CONSUMER -> procesado OK: " + body);
            channel.basicAck(tag, false);
        }
    }

    @Component
    static class DlqConsumer {
        @RabbitListener(queues = DLQ_QUEUE)
        public void consumeDlq(String message) {
            // Segundo consumidor: procesa/inspecciona mensajes fallidos.
            System.out.println("DLQ CONSUMER -> mensaje fallido recibido: " + message);
        }
    }
}
