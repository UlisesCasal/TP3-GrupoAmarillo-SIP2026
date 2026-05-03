# Hit #1 — El operador de Sobel (“un equipo”)
El operador de Sobel [SOB68] es una máscara que, aplicada a una imagen, permite detectar (resaltar) bordes. Es una operación matemática que, aplicada a cada píxel y considerando los píxeles vecinos, obtiene un nuevo valor (color) para ese píxel. Aplicando la operación a cada píxel se obtiene una nueva imagen que resalta los bordes.

Objetivo:

- Input: una imagen.
- Proceso: aplicación del operador de Sobel.
- Output: una imagen filtrada (con los bordes resaltados).

## Etapas 
### 1 — Centralizado. 
Desarrollen un proceso centralizado que tome una imagen, aplique la máscara y genere un nuevo archivo con el resultado. Ámbito: una sola laptop / equipo.


### 2 — Distribuido. 
Desarrollen el mismo proceso de manera distribuida: dividan la imagen en N pedazos y asignen la tarea de aplicar la máscara a N procesos distribuidos (workers). Después unifiquen los resultados. Este es exactamente el patrón Master-Worker (también llamado Granja de Trabajadores) que Foster [FOS95] caracteriza como uno de los esquemas algorítmicos paralelos fundamentales. Ámbito: Docker.
  
  
### 3 — Tolerante a fallos. 
Mejoren la aplicación de la Etapa 2 para que, en caso de que un worker (proceso distribuido al que se le asignó parte de la imagen a procesar) se caiga y no responda, el proceso principal detecte la situación y reasigne el cálculo a otro worker.
