# Integración de Textura de Pelota y Capas de Visualización

Se ha sustituido la pelota procedural por una textura PNG real y se ha reestructurado el sistema de capas para garantizar que la pelota siempre sea el elemento más visible en pantalla, situándose por encima del marcador y las nubes.

## Cambios Realizados

### [MainActivity.kt](file:///E:/dev/android_studio/AlebrijeVoleibol/v1/app/src/main/java/com/alebrije/voleibol/MainActivity.kt)

#### Nueva Textura de Pelota (PNG)
- **Carga de Activo:** Se integra el archivo `ball.png` utilizando `ImageBitmap.imageResource`.
- **Renderizado Fiel:** La función `drawAlebrijeBall` ahora utiliza `drawImage` para mostrar la textura original con toda su transparencia y detalle.
- **Mantenimiento de Animación:** Se conserva la lógica de rotación (`ballRotX`, `ballRotY`) aplicada sobre la textura, manteniendo la sensación de giro en el aire.
- **Física Intacta:** A pesar de que la imagen es cuadrada, el dibujo se ajusta exactamente al radio de colisión circular (`ballRadius`), asegurando que los golpes sigan siendo precisos y coherentes con el motor físico.

#### Reestructuración de Capas (Z-Index)
Para cumplir con el requisito de que la pelota se vea por encima de todo:
1.  **Canvas de Juego (Fondo):** Contiene nubes, jugadores y escenario.
2.  **Marcador (Capa Intermedia):** Se mantiene con `zIndex(10f)`.
3.  **Canvas de Pelota (Capa Superior):** Se ha creado un lienzo independiente con **`zIndex(20f)`** dedicado exclusivamente a la pelota. Esto garantiza que visualmente pase "por delante" del marcador y de las nubes en cualquier trayectoria.

## Verificación

- [x] **Visibilidad:** La pelota ahora aparece sobrepuesta al marcador de puntuación cuando cruza su área.
- [x] **Estética:** Se eliminó el dibujo vectorial de gajos, mostrando la imagen real `ball.png`.
- [x] **Compilación:** El proyecto compila y se ejecuta correctamente (`assembleDebug` exitoso).
- [x] **Preview:** Se actualizó la vista previa de Android Studio para soportar el nuevo parámetro de textura.

> [!TIP]
> Al separar la pelota en su propio Canvas superior, el juego gana una sensación de profundidad mucho más clara, destacando el objeto principal de la acción.
