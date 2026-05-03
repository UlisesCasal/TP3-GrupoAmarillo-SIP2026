package  com.grupoamarillo.hit1.etapa3.config;
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

    public static final String SOBEL_WORK_QUEUE="sobel.work_queue";
    public static final String SOBEL_RESULT_QUEUE="sobel.result_queue";

    public static final String SOBEL_DLX="sobel.dlx";
    public static final String SOBEL_DLQ="sobel.dlq";

    @Bean public DirectExchange dlqExchange() {return new DirectExchange(SOBEL_DLX);}
    @Bean public Queue dqlQueue() { return  QueueBuilder.durable(SOBEL_DLQ).build();}
    
   
   @Bean public Queue workQueue() { return  QueueBuilder.durable(SOBEL_WORK_QUEUE)
                .withArgument("x-queue-type", "quorum")
                .withArgument("x-dead-letter-exchange", SOBEL_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq.sobel")
                .withArgument("x-delivery-limit", 4)
                .build(); }

    @Bean public Queue resultQueue() { return new Queue(SOBEL_RESULT_QUEUE); }
    @Bean public DirectExchange exchange() { return new DirectExchange("image_exchange"); }
    
    
    @Bean
    public Binding bindWork(Queue workQueue, DirectExchange exchange) {
        return BindingBuilder.bind(workQueue).to(exchange).with("to_worker");
    }

    @Bean
    public Binding bindResult(Queue resultQueue, DirectExchange exchange) {
        return BindingBuilder.bind(resultQueue).to(exchange).with("to_aggregator");
    }

    @Bean Binding bindDLQ(Queue dqlQueue, DirectExchange dlqExchange){
        return  BindingBuilder.bind(dqlQueue).to(dlqExchange).with("dlq.sobel");
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