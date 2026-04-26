package com.grupoamarillo.producer;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;


@SpringBootConfiguration
@EnableAutoConfiguration
public class ProducerApplication {

	public static final String QUEUE = "tp3.ej1.tasks";

    @Value("${producer.delay-ms:1000}")
    private long delayMs;

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }

    @Bean
    Queue tasksQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    CommandLineRunner sendTenTasks(RabbitTemplate rabbitTemplate, ConfigurableApplicationContext ctx) {
        return args -> {
            for (int i = 1; i <= 10; i++) {
                String body = "Tarea " + i;
                rabbitTemplate.convertAndSend("", QUEUE, body);
                System.out.println("Enviada: " + body);
                Thread.sleep(delayMs);
            }
            int code = SpringApplication.exit(ctx, () -> 0);
            System.exit(code);
        };
    }

}
