# 🐇 RabbitMQ - Patrones de Mensajería

Proyecto de Sistemas Distribuidos y Programación Paralela - TP3

## 📋 Ejercicio 1: Message Queue (Point-to-Point)

### 🎯 Objetivo
Implementar un patrón de cola de mensajes donde múltiples consumidores procesan tareas de forma balanceada.

### 🏗️ Arquitectura

```
┌─────────────┐    ┌─────────────────┐    ┌─────────────┐
│  Producer   │───▶│     Queue       │───▶│  Consumer 1 │
│ (1 instancia)│    │   "tp3.ej1.tasks" │    │             │
└─────────────┘    └─────────────────┘    ├─────────────┤
                                         │  Consumer 2 │
                                         │             │
                                         └─────────────┘
```

### ⚙️ Configuración
- **Exchange**: Direct (default)
- **Queue**: Durable, no autodelete
- **Acknowledgment**: Manual
- **Prefetch**: 1 mensaje por consumer

### 🔄 Comportamiento Round-Robin

```
Mensaje 1 → Consumer 1
Mensaje 2 → Consumer 2  
Mensaje 3 → Consumer 1
Mensaje 4 → Consumer 2
```

### 📊 Propiedades de la Queue
```properties
Nombre: tp3.ej1.tasks
Durable: ✓ Sí
Auto-delete: ✗ No
Exclusive: ✗ No
Prefetch: 1 mensaje/consumer
Ack: Manual (MANUAL)
```

### 🧪 Casos de Prueba
1. **1 Consumer**: Todos los mensajes van a un solo consumer
2. **2 Consumers**: Distribución round-robin automática
3. **Producer primero**: Mensajes se acumulan en la queue

---

## 📢 Ejercicio 2: Pub-Sub Fan-out (Event Bus)

### 🎯 Objetivo  
Implementar un patrón de publicación-suscripción donde múltiples subscribers reciben todos los eventos.

### 🏗️ Arquitectura

```
┌─────────────┐    ┌─────────────────────┐    ┌─────────────┐
│  Publisher  │───▶│  Fanout Exchange    │───▶│ Subscriber 1│
│             │    │ "tp3.ej2.blocks.fanout"│    │ (Nodo 1)    │
└─────────────┘    └─────────────────────┘    ├─────────────┤
                         │                    │ Subscriber 2│
                         │                    │ (Nodo 2)    │
                         │                    ├─────────────┤
                         └───────────────────▶│ Subscriber 3│
                                              │ (Nodo 3)    │
                                              └─────────────┘
```

### ⚙️ Configuración
- **Exchange**: Fanout (broadcast)
- **Queues**: Anónimas, temporales
- **Routing Key**: Se ignora en fanout
- **Persistencia**: Exchange durable

### 🔄 Comportamiento Broadcast

```
Evento "Bloque #1" → Todos los subscribers lo reciben
Evento "Bloque #2" → Todos los subscribers lo reciben  
Evento "Bloque #3" → Todos los subscribers lo reciben
```

### 📊 Propiedades del Exchange
```properties
Nombre: tp3.ej2.blocks.fanout
Tipo: Fanout
Durable: ✓ Sí
Auto-delete: ✗ No
Routing: Se ignora (broadcast)
```

### 🧪 Caso de Uso: Blockchain
- **Eventos**: "Nuevo bloque minado"
- **Subscribers**: 3 nodos de red
- **Resultado**: Todos los nodos se mantienen sincronizados

---

## 💥 Ejercicio 3: Dead Letter Queue (DLQ)

### 🎯 Objetivo
Implementar una cola principal con **Dead Letter Exchange** para redirigir automáticamente mensajes fallidos a una **Dead Letter Queue**.

### 🏗️ Arquitectura

```
┌─────────────┐    ┌──────────────────────┐    ┌────────────────────┐
│  Producer   │───▶│  Main Exchange       │───▶│ Main Queue         │
└─────────────┘    │  tp3.ej3.main.exchange│    │ tp3.ej3.main.queue │
                   └──────────────────────┘    └─────────┬──────────┘
                                                         │
                             ack (error=false)           │ nack(requeue=false, error=true)
                                                         │
                                                         ▼
                                           ┌─────────────────────────┐
                                           │ DLX                     │
                                           │ tp3.ej3.dlx.exchange    │
                                           └───────────┬─────────────┘
                                                       ▼
                                           ┌─────────────────────────┐
                                           │ DLQ                     │
                                           │ tp3.ej3.dlq.queue       │
                                           └───────────┬─────────────┘
                                                       ▼
                                               DLQ Consumer
```

### ⚙️ Configuración
- **Main Queue**: durable + `x-dead-letter-exchange` + `x-dead-letter-routing-key`
- **DLX**: `tp3.ej3.dlx.exchange`
- **DLQ**: `tp3.ej3.dlq.queue`
- **Ack Mode**: manual (`AcknowledgeMode.MANUAL`)

### 🔄 Comportamiento
- Si el consumidor principal recibe `{"error": false}`: procesa y hace `ack`.
- Si recibe `{"error": true}`: hace `basicNack(..., requeue=false)`.
- RabbitMQ mueve ese mensaje al DLX y termina en la DLQ.
- El segundo consumidor (DLQ Consumer) lee e imprime los mensajes fallidos.

---

## 🚀 Cómo Ejecutar

### Prerrequisitos
- Java 17+
- RabbitMQ ejecutándose en localhost:5672
- Maven

### Ejecutar RabbitMQ (Docker)
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### Ejercicio 1: Message Queue
```bash
# Terminal 1 - Consumer 1 (Ej1)
cd consumer && mvn -Dspring-boot.run.main-class=com.grupoamarillo.consumer.hit1.ej1.ConsumerApplication -Dspring-boot.run.arguments=--consumer.id=1 spring-boot:run

# Terminal 2 - Consumer 2 (Ej1)  
cd consumer && mvn -Dspring-boot.run.main-class=com.grupoamarillo.consumer.hit1.ej1.ConsumerApplication -Dspring-boot.run.arguments=--consumer.id=2 spring-boot:run

# Terminal 3 - Producer (Ej1)
cd producer && mvn -Dspring-boot.run.main-class=com.grupoamarillo.producer.ProducerApplication spring-boot:run
```

### Ejercicio 2: Pub-Sub Fan-out
```bash
# Terminal 1 - Subscriber 1 (Nodo 1 - Ej2)
cd consumer && mvn -Dspring-boot.run.main-class=com.grupoamarillo.consumer.hit1.ej2.SubscriberApplication -Dspring-boot.run.arguments=--node.id=1 spring-boot:run

# Terminal 2 - Subscriber 2 (Nodo 2 - Ej2)
cd consumer && mvn -Dspring-boot.run.main-class=com.grupoamarillo.consumer.hit1.ej2.SubscriberApplication -Dspring-boot.run.arguments=--node.id=2 spring-boot:run

# Terminal 3 - Subscriber 3 (Nodo 3 - Ej2) 
cd consumer && mvn -Dspring-boot.run.main-class=com.grupoamarillo.consumer.hit1.ej2.SubscriberApplication -Dspring-boot.run.arguments=--node.id=3 spring-boot:run

# Terminal 4 - Publisher (Ej2)
cd producer && mvn -Dspring-boot.run.main-class=com.grupoamarillo.producer.hit1.ej2.PublisherApplication spring-boot:run
```

### Ejercicio 3: Dead Letter Queue (DLQ)
```bash
# Terminal 1 - Ejecutar Ej3 (proyecto aislado)
cd ej3-dlq && mvn clean spring-boot:run
```

Salida esperada (resumen):
- `MAIN CONSUMER -> procesado OK` para mensajes con `"error": false`
- `MAIN CONSUMER -> mensaje con error, se rechaza y va a DLQ` para `"error": true`
- `DLQ CONSUMER -> mensaje fallido recibido` en la cola de dead letters

---

## 📊 Comparativa de Patrones

| Característica | Point-to-Point | Pub-Sub Fan-out | DLQ |
|----------------|----------------|-----------------|-----|
| **Destinatarios** | 1 consumer por mensaje | Todos los subscribers | Mensajes fallidos |
| **Queue** | Compartida, durable | Anónimas, temporales | Principal + DLQ durable |
| **Acoplamiento** | Producer conoce queue | Publisher no conoce subscribers | Productor/consumidor desacoplados por DLX |
| **Escalabilidad** | Horizontal (más consumers) | Horizontal (más subscribers) | Horizontal en reproceso de fallidos |
| **Caso de uso** | Procesamiento de tareas | Notificaciones/Eventos | Trazabilidad y recuperación de errores |

---

## 🎯 Aprendizajes Clave

### Ejercicio 1 (Point-to-Point)
- ✅ Load balancing automático con round-robin
- ✅ Acknowledgement manual para procesamiento confiable  
- ✅ Prefetch count controla la distribución
- ✅ Queues duraderas para persistencia

### Ejercicio 2 (Pub-Sub Fan-out)
- ✅ Broadcast automático a todos los subscribers
- ✅ Desacoplamiento completo entre componentes
- ✅ Exchange fanout ignora routing keys
- ✅ Colas anónimas para suscripciones temporales

### Ejercicio 3 (DLQ)
- ✅ Mensajes con error no se pierden
- ✅ Redirección automática con DLX
- ✅ Reprocesamiento/control mediante DLQ consumer
- ✅ Patrón estándar usado en industria (RabbitMQ/SQS+DLQ)

---

## 🔍 Observaciones en RabbitMQ UI

### Para Ejercicio 1
- Ver la queue `tp3.ej1.tasks` con mensajes en ready/unacked
- Observar cómo los mensajes se distribuyen entre consumers
- Ver el prefetch count funcionando

### Para Ejercicio 2  
- Ver el exchange fanout `tp3.ej2.blocks.fanout`
- Observar las 3 colas anónimas conectadas
- Verificar que todos los mensajes llegan a todas las colas

### Para Ejercicio 3
- Ver la cola principal `tp3.ej3.main.queue`
- Ver el DLX `tp3.ej3.dlx.exchange`
- Ver la DLQ `tp3.ej3.dlq.queue`
- Verificar que mensajes con `"error": true` terminan en la DLQ

---

## 📝 Configuración de Properties

### Producer (application.properties)
```properties
# Ejercicio 1
spring.rabbitmq.addresses=localhost:5672

# Ejercicio 2  
publisher.delay-ms=1500
```

### Consumer (application.properties)
```properties
# Identificación para ambos ejercicios
consumer.id=1
node.id=1

# Configuración RabbitMQ
spring.rabbitmq.addresses=localhost:5672
spring.rabbitmq.listener.simple.acknowledge-mode=manual
spring.rabbitmq.listener.simple.prefetch=1
```
