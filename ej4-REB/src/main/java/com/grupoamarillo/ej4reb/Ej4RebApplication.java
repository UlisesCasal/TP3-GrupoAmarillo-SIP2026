package com.grupoamarillo.ej4reb;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Random;

/*  
Intento 1 (original) → FAIL → retry 1s
Intento 2            → FAIL → retry 2s
Intento 3            → FAIL → retry 4s
Intento 4            → FAIL → retry 8s
Intento 5            → FAIL → DLQ  ← "después de 4 reintentos fallidos"
*/

@SpringBootApplication
public class Ej4RebApplication {

    public static final String MAIN_EXCHANGE = "tp3.ej4.main.exchange";
    public static final String MAIN_QUEUE = "tp3.ej4.main.queue";
    public static final String MAIN_ROUTING_KEY = "tp3.ej4.main";

    public static final String RETRY_EXCHANGE = "tp3.ej4.retry.exchange";
    public static final String DLX_EXCHANGE = "tp3.ej4.dlx.exchange";
    public static final String DLQ_QUEUE = "tp3.ej4.dlq.queue";

    public static void main(String[] args) {
        SpringApplication.run(Ej4RebApplication.class, args);
    }

    // --- INFRAESTRUCTURA ---

    @Bean DirectExchange mainExchange() { return new DirectExchange(MAIN_EXCHANGE); } // Exchange principal para tareas
    @Bean DirectExchange retryExchange() { return new DirectExchange(RETRY_EXCHANGE); } // Exchange para reintentos con delay
    @Bean DirectExchange dlxExchange() { return new DirectExchange(DLX_EXCHANGE); } // Exchange para mensajes que superan reintentos

    // Cola principal para recibir tareas

    @Bean
    Queue mainQueue() {
        return QueueBuilder.durable(MAIN_QUEUE).build();
    }

    // Binding entre la cola principal y su exchange
    @Bean
    Binding mainBinding() {
        return BindingBuilder.bind(mainQueue()).to(mainExchange()).with(MAIN_ROUTING_KEY);
    }

    // Cola de Dead Letter para mensajes que superan los reintentos
    @Bean
    Queue dlq() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    // Binding entre la cola de Dead Letter y su exchange
    @Bean
    Binding dlqBinding() {
        return BindingBuilder.bind(dlq()).to(dlxExchange()).with("dlq.key");
    }

    // --- COLAS DE ESPERA (RETRY QUEUES) CON TTL ---

    private Queue createRetryQueue(String name, int ttlMs) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", MAIN_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_ROUTING_KEY)
                .withArgument("x-message-ttl", ttlMs)
                .build();
    }

    @Bean Queue retry1s() { return createRetryQueue("tp3.ej4.retry.1s", 1000); }
    @Bean Queue retry2s() { return createRetryQueue("tp3.ej4.retry.2s", 2000); }
    @Bean Queue retry4s() { return createRetryQueue("tp3.ej4.retry.4s", 4000); }
    @Bean Queue retry8s() { return createRetryQueue("tp3.ej4.retry.8s", 8000); }

    @Bean Binding r1Bind() { return BindingBuilder.bind(retry1s()).to(retryExchange()).with("retry.1s"); }
    @Bean Binding r2Bind() { return BindingBuilder.bind(retry2s()).to(retryExchange()).with("retry.2s"); }
    @Bean Binding r4Bind() { return BindingBuilder.bind(retry4s()).to(retryExchange()).with("retry.4s"); }
    @Bean Binding r8Bind() { return BindingBuilder.bind(retry8s()).to(retryExchange()).with("retry.8s"); }

    // --- PRODUCTOR: 20 MENSAJES ---
    @Bean
    CommandLineRunner producer(RabbitTemplate rabbitTemplate) {
        return args -> {
            System.out.println("\n[PROD] Enviando 20 tareas a la cola principal...");
            for (int i = 1; i <= 20; i++) {
                rabbitTemplate.convertAndSend(MAIN_EXCHANGE, MAIN_ROUTING_KEY, "Tarea #" + i);
            }
        };
    }

    // --- CONSUMIDORES ---
    @Component
    static class RebConsumer {
        private final RabbitTemplate rabbitTemplate;
        private final Random random = new Random();
        private final int[] delays = {1, 2, 4, 8};

        RebConsumer(RabbitTemplate rabbitTemplate) {
            this.rabbitTemplate = rabbitTemplate;
        }

        // Escuchamos la cola principal
        @RabbitListener(queues = MAIN_QUEUE, ackMode = "MANUAL")
        public void consume(Message message, Channel channel) throws Exception {
            long deliveryTag = message.getMessageProperties().getDeliveryTag();
            
            // Simulamos procesamiento con posible error aleatorio
            try {
                String body = new String(message.getBody());
                Integer retryCount = (Integer) message.getMessageProperties().getHeaders().getOrDefault("x-retry-count", 0);

                System.out.println("[CONS] Recibido (Intento #" + (retryCount + 1) + "): " + body);

                // Simulamos un 50% de probabilidad de fallo
                if (random.nextBoolean()) {
                    System.out.println("  [!] ERROR en " + body);
                    
                    // Si no hemos superado el número de reintentos, lo reencolamos con el delay correspondiente
                    if (retryCount < delays.length) {
                        int delay = delays[retryCount];
                        String routingKey = "retry." + delay + "s";
                        
                        System.out.println("  [RETRY] Reencolando con espera de " + delay + "s...");

                        MessageProperties props = new MessageProperties();
                        props.setHeader("x-retry-count", retryCount + 1);
                        Message retryMsg = new Message(message.getBody(), props);

                        rabbitTemplate.send(RETRY_EXCHANGE, routingKey, retryMsg);
                    } else {
                        System.out.println("  [DLQ] Max reintentos alcanzado para " + body);
                        rabbitTemplate.convertAndSend(DLX_EXCHANGE, "dlq.key", body);
                    }
                } else {
                    System.out.println("  [OK] Procesado exitosamente: " + body);
                }
            } finally {
                // Siempre confirmamos el mensaje de la cola actual
                channel.basicAck(deliveryTag, false);
            }
        }

        // Escuchamos la DLQ para ver los mensajes que fallaron después de los reintentos
        @RabbitListener(queues = DLQ_QUEUE)
        public void dlqListener(String message) {
            System.err.println("\n>>> [DLQ RECIBIDO] Mensaje fallido final: " + message + " <<<\n");
        }
    }
}