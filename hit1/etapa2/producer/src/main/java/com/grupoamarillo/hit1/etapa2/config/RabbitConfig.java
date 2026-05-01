package com.grupoamarillo.hit1.etapa2.config;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@EnableAutoConfiguration
@Configuration
public class RabbitConfig {
    @Bean public Queue workQueue() { return  QueueBuilder.durable("sobel.work_queue").build(); }
    @Bean public Queue resultQueue() { return  QueueBuilder.durable("sobel.result_queue").build(); }
    @Bean public DirectExchange exchange() { return new DirectExchange("image_exchange"); }
    
    @Bean
    public Binding bindWork(Queue workQueue, DirectExchange exchange) {
        return BindingBuilder.bind(workQueue).to(exchange).with("to_worker");
    }

    @Bean
    public Binding bindResult(Queue resultQueue, DirectExchange exchange) {
        return BindingBuilder.bind(resultQueue).to(exchange).with("to_aggregator");
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
}