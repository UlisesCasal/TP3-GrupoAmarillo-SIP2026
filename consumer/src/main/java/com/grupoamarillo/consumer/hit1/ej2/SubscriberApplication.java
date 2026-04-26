package com.grupoamarillo.consumer.hit1.ej2;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SubscriberApplication {
    // Nombre fijo del exchange fanout que se compartirá entre todos los subscribers
    public static final String EXCHANGE = "tp3.ej2.blocks.fanout";

    public static void main(String[] args) {
        SpringApplication.run(SubscriberApplication.class, args);
    }

    // Crea el exchange fanout con nombre fijo
    // true  -> durable (sobrevive reinicios del broker)
    // false -> no autodelete (no se borra cuando se desconectan los consumers)
    @Bean
    FanoutExchange blocksExchange(){
        return new FanoutExchange(EXCHANGE, true, false);
    }

    // Cola anónima: cada instancia de este subscriber obtiene un nombre único y temporal
    // Ideal para escalar horizontalmente: cada nuevo contenedor/proceso recibe su propia cola
    @Bean
    Queue nodeQueue(){
        return new AnonymousQueue();
    }

    // Enlaza la cola anónima al exchange fanout; de esta forma la cola recibirá
    // una copia de cada mensaje que se publique en el exchange
    @Bean
    Binding binding(Queue nodeQueue, FanoutExchange blocksExchange){
        return BindingBuilder.bind(nodeQueue).to(blocksExchange);
    }

    // Componente interno que escucha los mensajes entrantes
    @Component
    static class BlockSubscriber{

        // Identificador del nodo; se puede sobreescribir con la propiedad 'node.id'
        // Si no se define, el valor por defecto es "1"
        @Value("${node.id:1}")
        private String nodeId;

        // Listener que se activa cada vez que llega un mensaje a la cola anónima
        // #{nodeQueue.name} resuelve el nombre dinámico de la cola en tiempo de arranque
        @RabbitListener(queues = "#{nodeQueue.name}")
        public void onEvent(String message){
            System.out.println("Nodo [" + nodeId + "] recibio: " + message);
        }
    }
}
