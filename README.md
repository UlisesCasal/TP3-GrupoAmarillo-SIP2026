# 🐇 RabbitMQ - Patrones de Mensajería

Proyecto de Sistemas Distribuidos y Programacion Paralela - TP3

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

## 🔁 Ejercicio 4: Retry Exponential Backoff (REB)

### 🎯 Objetivo
Implementar un sistema de reintentos con **espera exponencial** utilizando colas intermedias con TTL.

### 🏗️ Arquitectura

```
┌──────────┐      ┌─────────────┐      ┌───────────────┐
│ Producer │─────▶│ Main Queue  │◀─────┤ Retry Queues  │
└──────────┘      └──────┬──────┘      │ (1s, 2s, 4s, 8s)│
                         │             └───────▲───────┘
                 Error? (50% prob)             │
                         │                     │
             ┌───────────┴───────────┐         │
             │     REB Consumer      │─────────┘
             └───────────┬───────────┘
                         │
               Max retries reached?
                         │
                         ▼
             ┌───────────────────────┐
             │         DLQ           │
             └───────────────────────┘
```

### ⚙️ Configuración
- **Main Queue**: Recibe mensajes originales y reintentos expirados.
- **Retry Queues**: 4 colas con TTL progresivo (1s, 2s, 4s, 8s).
- **TTL & DLX**: Cada cola de retry redirige al `MAIN_EXCHANGE` al expirar.
- **Header `x-retry-count`**: Controla el número de intentos realizados.

### 🔄 Comportamiento Exponential Backoff
- **Intento 1**: Si falla, espera **1s**.
- **Intento 2**: Si falla, espera **2s**.
- **Intento 3**: Si falla, espera **4s**.
- **Intento 4**: Si falla, espera **8s**.
- **Fallo Final**: Tras el 4to reintento (5to intento total), el mensaje se mueve a la **DLQ**.

### 🧪 Resultados Observados (Logs)
```text
[CONS] Recibido (Intento #1): Tarea #1
  [!] ERROR en Tarea #1
  [RETRY] Reencolando con espera de 1s...
[CONS] Recibido (Intento #2): Tarea #1
  [RETRY] Reencolando con espera de 2s...
[CONS] Recibido (Intento #3): Tarea #1
  [OK] Procesado exitosamente: Tarea #1
```

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

### Ejercicio 4: Exponential Backoff (REB)
```bash
# Terminal 1 - Ejecutar Ej4 (proyecto aislado)
cd ej4-REB && mvn clean spring-boot:run
```

---

## 📊 Comparativa de Patrones

| Característica | Point-to-Point | Pub-Sub Fan-out | DLQ | Retry Backoff |
|----------------|----------------|-----------------|-----|---------------|
| **Destinatarios** | 1 consumer | Todos los subscribers | Mensajes fallidos | 1 consumer (con reintentos) |
| **Queue** | Compartida, durable | Anónimas, temporales | Principal + DLQ | Principal + Retry Queues |
| **Escalabilidad** | Horizontal | Horizontal | Control de fallos | Resiliencia temporal |
| **Estrategia** | Round-Robin | Broadcast | Redirección simple | Exponential Backoff |
| **Caso de uso** | Carga de trabajo | Eventos/Notificaciones | Trazabilidad | Fallos transitorios (APIs/DB) |

---

## 🎯 Aprendizajes Clave

### Ejercicio 1 (Point-to-Point)
- ✅ Load balancing automático con round-robin.
- ✅ Acknowledgement manual para procesamiento confiable.

### Ejercicio 2 (Pub-Sub Fan-out)
- ✅ Broadcast automático a todos los subscribers.
- ✅ Exchange fanout ignora routing keys.

### Ejercicio 3 (DLQ)
- ✅ Mensajes con error no se pierden.
- ✅ Redirección automática mediante configuración de exchange.

### Ejercicio 4 (Retry Exponential Backoff)
- ✅ Implementación de resiliencia sin plugins externos.
- ✅ Control de carga mediante esperas progresivas.
- ✅ Uso creativo de TTL y DLX para simular "delay".

---

## 🔍 Observaciones en RabbitMQ UI

### Para Ejercicio 4
- Ver el exchange `tp3.ej4.retry.exchange`.
- Observar las colas `tp3.ej4.retry.1s`, `2s`, `4s`, `8s`.
- Verificar cómo los mensajes "saltan" de las colas de retry de regreso a la principal al expirar su TTL.
- Ver los mensajes finales en `tp3.ej4.dlq.queue` tras agotar los reintentos.
