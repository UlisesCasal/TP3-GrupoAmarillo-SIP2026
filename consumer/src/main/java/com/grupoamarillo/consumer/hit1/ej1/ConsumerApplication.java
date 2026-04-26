package com.grupoamarillo.consumer.hit1.ej1;

// ─────────────────────────────────────────────────────────────────────────────
// Importaciones necesarias para trabajar con RabbitMQ y Spring AMQP
// ─────────────────────────────────────────────────────────────────────────────

// Canal de comunicación con RabbitMQ. Permite operaciones como acknowledge
// (basicAck) para confirmar que un mensaje fue procesado correctamente.
import com.rabbitmq.client.Channel;

// Modo de acknowledgement manual: el consumidor decide cuándo confirmar
// que procesó el mensaje, en lugar de que RabbitMQ lo haga automáticamente.
import org.springframework.amqp.core.AcknowledgeMode;

// Representa el mensaje crudo que llega desde la cola,，包含 sus propiedades
// (delivery tag, headers, etc.) y el body (bytes del contenido).
import org.springframework.amqp.core.Message;

// Cola durable donde se acumulan los mensajes pending de ser consumidos.
// Se define como Bean para que Spring la declare en RabbitMQ al iniciar.
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

// Anotación que marca un método como listener de una cola. Cuando llega un
// mensaje, Spring invoca automáticamente este método y le pasa el mensaje.
import org.springframework.amqp.rabbit.annotation.RabbitListener;

// Factoría de conexiones RabbitMQ. Spring Boot auto-configura una conexión
// al broker (localhost:5672) basándose en application.properties.
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

// Permite configurar cómo el listener consume mensajes: modo de ack, prefetch,
// cantidad de hilos concurrentes, etc. Se define como Bean custom.
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;

// Permite inyectar valores de configuración externalizados (application.properties
// o argumentos de línea de comando). Acá se usa para identificar cada instancia
// del consumidor cuando se corren múltiples procesos.
import org.springframework.beans.factory.annotation.Value;

// Punto de entrada de Spring Boot. Esta anotación habilita la configuración
// automática de AMQP, escaneo de componentes, etc.
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Marca un método como fuente de un Bean managed por Spring (cola, factory, etc.).
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

// Constante para especificar el charset UTF-8 al convertir el body del mensaje
// de bytes a String, evitando así problemas con caracteres especiales.
import java.nio.charset.StandardCharsets;

// ─────────────────────────────────────────────────────────────────────────────
// Anotación principal: indica que esta clase es una aplicación Spring Boot
// con soporte automático para AMQP (RabbitMQ).
// ─────────────────────────────────────────────────────────────────────────────
@SpringBootApplication
public class ConsumerApplication {

    // Nombre de la cola. Todos los productores y consumidores usan la misma
    // para comunicarse. "tp3.ej1.tasks" es la convención de nombres del TP3.
    public static final String QUEUE = "tp3.ej1.tasks";

    // ─────────────────────────────────────────────────────────────────────────
    // main: punto de entrada. SpringApplication.run(...) inicia el contexto
    // de Spring, detecta los Beans definidos abajo y comienza a escuchar la cola.
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bean Queue: declara la cola "tp3.ej1.tasks" en RabbitMQ.
    // - durable=true → la cola sobrevive a un reinicio del broker.
    // - QueueBuilder.fluent API chaining.
    // Spring lo crea al arrancar y RabbitMQ lo aloja si no existe.
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    Queue tasksQueue(){
        return QueueBuilder.durable(QUEUE).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bean SimpleRabbitListenerContainerFactory: configura cómo este consumidor
    // consume mensajes de la cola.
    //
    // - AcknowledgeMode.MANUAL: el consumidor debe llamar channel.basicAck()
    //   manualmente para confirmar que processó el mensaje. Esto garantiza
    //   que si el consumidor cae antes de ack-ear, RabbitMQ re-entrega el
    //   mensaje a otro consumer (garantía de "al menos uno").
    //
    // - prefetchCount=1: RabbitMQ envía como máximo 1 mensaje no-ack'd a la
    //   vez por consumer. Esto habilita el balanceo round-robin entre varios
    //   consumidores (ej: 2 consumers → cada uno recibe ~5 mensajes).
    //
    // - concurrentConsumers=1: un solo hilo de consumo por instancia.
    //   Se puede aumentar para procesar varios mensajes en paralelo dentro
    //   del mismo proceso.
    // ─────────────────────────────────────────────────────────────────────────
    @Bean
    SimpleRabbitListenerContainerFactory manualAckFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setConcurrentConsumers(1);
        return factory;
    }

    // Se define el listener como un bean separado para evitar la referencia
    // circular entre la clase de configuración principal y el containerFactory.
    @Component
    static class TaskConsumer {

        // Identificador de instancia del consumidor. Se pasa por línea de
        // comandos (--consumer.id=1, --consumer.id=2...) para distinguir qué
        // proceso atendió cada mensaje.
        @Value("${consumer.id:1}")
        private String consumerId;

        // @RabbitListener: marca este método como handler de la cola.
        // Cada vez que llega un mensaje a "tp3.ej1.tasks", Spring invoca
        // onMessage() usando la configuración de ack manual definida arriba.
        @RabbitListener(queues = QUEUE, containerFactory = "manualAckFactory")
        public void onMessage(Message message, Channel channel) throws Exception {
            long tag = message.getMessageProperties().getDeliveryTag();
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            System.out.println("Consumer [" + consumerId + " ] recibio: " + body);
            channel.basicAck(tag, false);
        }
    }
}
