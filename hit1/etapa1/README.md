# Hit #1 — El operador de Sobel (“un equipo”)

## Etapa 1 — Centralizado
Centralizado. Desarrollen un proceso centralizado que tome una imagen, aplique la máscara y genere un nuevo archivo con el resultado. Ámbito: una sola laptop / equipo.

## ejecución
~~~bash
curl -X POST -F "file=@/path/to/image.jpg" http://localhost:8080/image/sobel -o output.jpg
~~~
>[!CAUTION]
> Reemplaza `/path/to/image.jpg` con la ruta real de tu imagen. Es necesario el `@` antes del nombre o ruta del archivo para indicar que se trata de un archivo a subir.
Tambien en la URL se puede usar `http://localhost:8080/` se provee una interfaz web para cargar la imagen y obtener el resultado.