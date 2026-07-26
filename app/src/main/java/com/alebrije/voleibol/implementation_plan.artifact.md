# Implementación de Atlas de Nubes Pixel Art Procedural

El usuario reporta que aún no ve las nubes del atlas. Este plan asegura que el estado se refresque correctamente y que el estilo visual sea idéntico al Pixel Art de la imagen de referencia (formas angulares, bases planas y sombreado sólido).

## User Review Required

> [!IMPORTANT]
> - Se forzará la limpieza de la lista de nubes (`clouds.clear()`) para garantizar que los modelos anteriores desaparezcan.
> - El dibujo cambiará de círculos a **rectángulos redondeados** (`drawRoundRect`) para capturar la esencia "bloque" del Pixel Art.
> - Se han mapeado las 8 siluetas exactas de la imagen proporcionada.

## Proposed Changes

### [app]

#### [MODIFY] [MainActivity.kt](file:///E:/dev/android_studio/AlebrijeVoleibol/v1/app/src/main/java/com/alebrije/voleibol/MainActivity.kt)

1.  **Forzar Refresco de Estado**:
    *   En el `LaunchedEffect(width)` de `CourtScreen`, se llamará a `clouds.clear()` antes de añadir las nuevas nubes. Esto soluciona el problema de ver nubes "viejas" si la composición no se reinició por completo.

2.  **Rediseño de `drawCloud` (Estilo Atlas Pixel Art)**:
    *   Sustituir `drawCircle` por `drawRoundRect` con un radio de esquina pequeño (~15% del tamaño).
    *   Implementar las 8 variantes con coordenadas de rectángulos que imitan las siluetas de la imagen:
        - **Var 0:** 3 bloques (Pequeña).
        - **Var 1:** 5 bloques (Ancha).
        - **Var 2:** 3 bloques con domo central alto (Puffy).
        - **Var 3:** Estructura compleja de 6 bloques (Grande).
        - **Var 4:** Perfil bajo (Flat).
        - **Var 5:** 2 picos asimétricos.
        - **Var 6:** Torre central (High).
        - **Var 7:** Estructura escalonada (Long).
    *   **Sombreado**: La sombra será un rectángulo sólido en la base de cada cúmulo, no un desplazamiento suave, para mantener el estilo "Atlas".

## Verification Plan

### Manual Verification
1.  **Comprobación de Reinicio**: Entrar y salir del juego (o reiniciar la app) y confirmar que la primera nube que aparece ya tiene el nuevo estilo de bloques.
2.  **Comparativa Visual**: Comparar las siluetas en pantalla con la imagen del atlas. Deben ser reconocibles las 8 variantes.
3.  **Sombreado**: Confirmar que la base de las nubes tiene el tono azulado/grisáceo característico del atlas.
