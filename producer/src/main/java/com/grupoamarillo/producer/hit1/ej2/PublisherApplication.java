package com.grupoamarillo.producer.hit1.ej2;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PublisherApplication {
    //Todos los subscribers se conectan al mismo exchange
    public static final String EXCHANGE = "tp3.ej2.blocks.fanout";
    
    //Delay entre mensajes para que pueda ser mas visual
    @Value("${publisher.delay-ms:1500}")
    private long delayMs;


    public static void main(String[] args) {
        SpringApplication.run(PublisherApplication.class, args);
    }

    @Bean 
    FanoutExchange blocksExchange(){
        return new FanoutExchange(EXCHANGE, true, false);
    }

    //Se ejecuta automaticamente al iniciar la aplicacion
    @Bean
    CommandLineRunner publishEvents(RabbitTemplate rabbitTemplate, ConfigurableApplicationContext ctx){
        //Publica 6 eventos con delay entre ellos
        return args -> {
            for (int i = 0; i <=5; i++){
                String event = "Nuevo bloque minado: bloque #" + i;
                rabbitTemplate.convertAndSend(EXCHANGE, "", event);
                System.out.println("Publicado: " + event);
                Thread.sleep(delayMs);
            }
            int code = SpringApplication.exit(ctx, () -> 0);
            System.exit(code);
        };
    }
}

