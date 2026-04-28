# Informe de Patrones de Mensajería (TP3 - Ejercicio 5)

Este informe detalla los patrones de mensajería implementados en el trabajo práctico, incluyendo sus diagramas de arquitectura, diferencias clave y escenarios recomendados de uso.

---

## 1. Patrón Point-to-Point (Cola de Mensajes)
Es el modelo más simple donde un productor envía mensajes a una cola y uno o más consumidores los procesan.

### Diagrama de Arquitectura
```
┌─────────────┐    ┌─────────────────┐    ┌─────────────┐
│  Producer   │───▶│     Queue       │───▶│  Consumer 1 │
│             │    │ "tp3.ej1.tasks" │    │             │
└─────────────┘    └─────────────────┘    ├─────────────┤
                                          │  Consumer 2 │
                                          │             │
                                          └─────────────┘
```
*Nota: Si hay múltiples consumidores, cada mensaje es procesado por solo uno de ellos (competición).*

### Escenarios de uso
- Distribución de carga de trabajo (Worker Queues).
- Procesamiento asíncrono de tareas pesadas.
- Desacoplamiento de microservicios donde solo un servicio debe reaccionar al evento.

---

## 2. Patrón Publish-Subscribe (Fanout)
El mensaje enviado por el publicador se replica en todas las colas vinculadas al Exchange de tipo Fanout.

### Diagrama de Arquitectura
```
┌─────────────┐    ┌─────────────────────┐    ┌─────────────┐
│  Publisher  │───▶│  Fanout Exchange    │───▶│ Subscriber 1│
│             │    │                     │    │             │
└─────────────┘    └─────────────────────┘    ├─────────────┤
                         │                    │ Subscriber 2│
                         │                    │             │
                         │                    ├─────────────┤
                         └───────────────────▶│ Subscriber 3│
                                              │             │
                                              └─────────────┘
```

### Escenarios de uso
- Notificaciones masivas (Push notifications).
- Actualización de caché en múltiples nodos.
- Registro de logs por diferentes servicios (ej. un servicio guarda en BD y otro en un archivo).

---

## 3. Dead Letter Queue (DLQ)
Permite manejar mensajes que no pudieron ser procesados exitosamente, moviéndolos a una cola especial para su posterior análisis o intervención manual.

### Diagrama de Arquitectura
```
┌─────────────┐    ┌──────────────────────┐    ┌────────────────────┐
│  Producer   │───▶│  Main Exchange       │───▶│ Main Queue         │
└─────────────┘    └──────────────────────┘    └─────────┬──────────┘
                                                         │
                             ack (error=false)           │ nack(requeue=false)
                                                         │
                                                         ▼
                                           ┌─────────────────────────┐
                                           │ DLX                     │
                                           └───────────┬─────────────┘
                                                       ▼
                                           ┌─────────────────────────┐
                                           │ DLQ                     │
                                           └───────────┬─────────────┘
                                                       ▼
                                               DLQ Consumer
```

### Escenarios de uso
- Manejo de errores de negocio (datos mal formados).
- Depuración de fallos en producción sin perder información.
- Auditoría de mensajes fallidos.

---

## 4. Reliable Event Bus (REB) con Retry Pattern
Implementa una lógica de reintentos exponenciales. Si un mensaje falla, se mueve a colas de espera con TTL (Time-To-Live) antes de volver a la cola principal.

### Diagrama de Arquitectura
```
┌──────────┐      ┌─────────────┐      ┌───────────────┐
│ Producer │─────▶│ Main Queue  │◀─────┤ Retry Queues  │
└──────────┘      └──────┬──────┘      │ (1s, 2s, 4s, 8s)│
                         │             └───────▲───────┘
                 Error? (Retry)                │
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

### Escenarios de uso
- Integración con APIs externas inestables (fallos temporales de red).
- Sistemas donde el orden de procesamiento puede ser flexible ante errores.
- Transacciones que pueden fallar por bloqueos temporales de base de datos.

---

## Diferencias entre Patrones

| Característica | Point-to-Point | Pub/Sub (Fanout) | DLQ | REB (Retry) |
| :--- | :--- | :--- | :--- | :--- |
| **Destinatarios** | Un solo consumidor por mensaje | Múltiples consumidores | Un consumidor (o admin) | Un consumidor (reintentos) |
| **Persistencia** | Generalmente durable | Temporales/Durable | Alta (para auditoría) | Alta (resiliencia) |
| **Complejidad** | Baja | Media | Media | Alta |
| **Manejo de Errores** | Básico (Requeue) | Básico | Redirección a DLQ | Reintentos progresivos |

---

## Conclusiones sobre Escenarios de Uso

- **Usar Point-to-Point** cuando el objetivo es la escalabilidad horizontal y la distribución de tareas.
- **Usar Pub/Sub** cuando varios componentes independientes necesitan reaccionar al mismo evento de forma aislada.
- **Usar DLQ** en cualquier sistema crítico para evitar la pérdida de datos cuando el procesamiento falla de forma definitiva.
- **Usar REB/Retry** cuando se espera que los errores sean transitorios y un reintento automático pueda solucionar el problema sin intervención humana.
