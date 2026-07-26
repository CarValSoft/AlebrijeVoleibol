# Nubes Procedurales Estilo Pixel Art

Se ha implementado un sistema de generación de nubes que imita las 8 variantes del atlas de referencia proporcionado, utilizando una técnica de cúmulos de círculos con capas de sombra.

## Cambios Realizados

### [MainActivity.kt](file:///E:/dev/android_studio/AlebrijeVoleibol/v1/app/src/main/java/com/alebrije/voleibol/MainActivity.kt)

#### Sistema de Cúmulos (Atlas Procedural)
Se ha reescrito la función `drawCloud` para utilizar configuraciones de cúmulos basadas en la imagen de referencia:
- **8 Variantes Únicas:** Cada nube ahora tiene una estructura específica (piramidal, torre, doble pico, asimétrica, etc.).
- **Sombreado Dinámico:** Se dibuja una capa inferior en tono azulado suave (`0xFFDDE7F2`) con un ligero desplazamiento y escala mayor, creando el efecto de profundidad y base plana visto en el pixel art original.
- **Transparencia Suave:** Se ajustó la opacidad al 85% para que las nubes se integren de forma natural con el degradado del cielo.

#### Variedad en el Cielo
- Se incrementó el número de nubes iniciales a 7.
- Se amplió el generador aleatorio para incluir las 8 variantes (`Random.nextInt(8)`).
- Se ajustaron los rangos de tamaño y posición vertical para mayor dinamismo visual.

## Verificación

- [x] **Compilación:** El proyecto compila y se ejecuta sin problemas.
- [x] **Estética:** Las nubes ahora muestran formas complejas y variadas en lugar de círculos simples, respetando el estilo artístico del atlas.

> [!TIP]
> Si deseas nubes más compactas o dispersas, puedes ajustar el factor de escala `size` en la inicialización o los offsets relativos en la lógica de cada variante dentro de `drawCloud`.
