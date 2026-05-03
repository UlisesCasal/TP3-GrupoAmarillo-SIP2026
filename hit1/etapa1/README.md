# Hit #1 — El operador de Sobel (“un equipo”)

## Etapa 1 — Centralizado
Centralizado. Desarrollen un proceso centralizado que tome una imagen, aplique la máscara y genere un nuevo archivo con el resultado. Ámbito: una sola laptop / equipo.


## Ejecución
~~~bash
# Se proporciona un script para generar las imagenes de esta etapa
sh ./build-image.sh
~~~

Una vez generada la imagen, levantamos el contenedor.
~~~bash
docker run -p 8080:8080 tp3-hit1-etapa1 
~~~

Podemos enviar la imagen a procesar utilizando curl:
~~~bash
curl -X POST -F "file=@/path/to/image.jpg" http://localhost:8080/image/sobel -o output.jpg
~~~

>[!CAUTION]
> Reemplaza `/path/to/image.jpg` con la ruta real de tu imagen. Es necesario el `@` antes del nombre o ruta del archivo para indicar que se trata de un archivo a subir.

Se proporciona en `http://localhost:8080/` una interfaz web simple para cargar la imagen y obtener el resultado.