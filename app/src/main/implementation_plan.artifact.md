# Capas de Visualización y Reemplazo de Pelota PNG

Este plan aborda la reestructuración de las capas de dibujo para que la pelota aparezca por encima de todos los elementos (marcador y nubes) y sustituye la representación procedural de la pelota por una textura PNG.

## Cambios Propuestos

### [app]

#### [MODIFY] [MainActivity.kt](file:///E:/dev/android_studio/AlebrijeVoleibol/v1/app/src/main/java/com/alebrije/voleibol/MainActivity.kt)

1.  **Carga de la nueva pelota**:
    *   Cargar `R.drawable.ball` como `ImageBitmap` en `CourtScreen` usando `remember`.

2.  **Reestructuración de Capas (Z-Index)**:
    *   Actualmente el marcador está en un `Box` con `zIndex(10f)` sobre el `Canvas`.
    *   Para que la pelota esté encima del marcador, se dividirá el dibujo en dos `Canvas`:
        *   **Canvas 1 (Fondo/Juego)**: Nubes, mar, arena, red, jugadores, partículas.
        *   **Box Intermedio**: Marcador (`zIndex(10f)`).
        *   **Canvas 2 (Pelota)**: Solo el dibujo de la pelota (`zIndex(20f)`).

3.  **Rediseño de `drawAlebrijeBall`**:
    *   Sustituir el dibujo de círculos y gajos por `drawImage`.
    *   Aplicar rotación (`rotate`) sobre el centro de la pelota.
    *   Ajustar el destino para que coincida exactamente con el `ballRadius` actual, manteniendo la zona de golpeo circular intacta (la lógica de colisión no cambia, solo el renderizado).

4.  **Optimización**:
    *   Mantener la transparencia original de `ball.png` al usar `drawImage` sin filtros de color.

## Plan de Verificación

### Manual Verification
1.  **Capas**: Lanzar la pelota hacia arriba y verificar que pase visualmente por "encima" del marcador de puntuación.
2.  **Gráficos**: Confirmar que la nueva textura de la pelota se ve nítida y conserva sus bordes transparentes.
3.  **Animación**: Verificar que la pelota sigue rotando en el aire de forma fluida.
4.  **Física**: Comprobar que los rebotes siguen siendo precisos y circulares.
