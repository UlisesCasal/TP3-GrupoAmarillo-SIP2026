# Hit #1 — El operador de Sobel (“un equipo”)
## Etapa 3 — Tolerante a fallos
 Mejoren la aplicación de la Etapa 2 para que, en caso de que un worker (proceso distribuido al que se le asignó parte de la imagen a procesar) se caiga y no responda, el proceso principal detecte la situación y reasigne el cálculo a otro worker.


### Decisionse
    - Los consumidores tiene un delay de 2 segundos en enviar el mensaje de confirmación para permitir simular un congelamiento y detener el al mismo.
    -  Se definio que si se encola un mismo mensaje 4 veces, se descartara el mismo a una DLQ, para evitar mensajes toxicos.

### Ejecución
~~~ bash
sh build-image.sh
~~~

~~~ bash
docker compose -f compose.hi1.3.yml up -d
~~~
Se levantaron 4 workers, una rabbitmq, una joinner (para compaginar y poner a dispocision la imagen) y un producer (que divide y encola la imagen original).

~~~bash
# parts cantidad de partes en las que se divide la imagen, mensajes que generará el producer
curl -X POST -F "file=@/path/to/image.jpg" -F "parts=10" http://localhost:8080/image/sobel
~~~
La imagen se puede ver en `http://localhost:8088/images/sobel/result_{ID}.jpg`, donde `{ID}` es un identificador único generado para cada imagen procesada.