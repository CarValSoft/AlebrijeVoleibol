# Control de Rotación Natural y Reactiva de la Pelota

Este plan aborda la solicitud del usuario de reducir la velocidad de rotación de la pelota, haciéndola más natural y reactiva a los impactos recibidos.

## Cambios Propuestos

### [app]

#### [MODIFY] [MainActivity.kt](file:///E:/dev/android_studio/AlebrijeVoleibol/v1/app/src/main/java/com/alebrije/voleibol/MainActivity.kt)

1.  **Rediseño de la Acumulación de Rotación**:
    *   Sustituir la actualización constante basada en velocidad por una variable de "spin" (giro) que se activa en el impacto.
    *   `ballRotAccum`: Nueva variable para acumular el ángulo total.
    *   `ballSpin`: Velocidad de rotación actual.

2.  **Lógica en el bucle de juego**:
    *   Reducir drásticamente los multiplicadores de rotación.
    *   Implementar una fricción de rotación (freno natural) para que el giro se detenga gradualmente si no recibe nuevos impactos.
    *   Ajustar la rotación para que dependa principalmente de la dirección del golpe (`strike`) y no tanto de la velocidad de traslación.

3.  **Ajuste en `drawAlebrijeBall`**:
    *   Eliminar los multiplicadores internos (`* 50f`, `* 10f`) que amplificaban demasiado el ángulo.
    *   Usar directamente el ángulo acumulado en grados.

## Plan de Verificación

### Manual Verification
1.  **Impacto**: Golpear la pelota y observar que comienza a girar de forma coherente con la fuerza del golpe.
2.  **Decaimiento**: Verificar que la rotación disminuye gradualmente mientras la pelota vuela sin ser tocada.
3.  **Límites**: Asegurar que la rotación nunca alcance velocidades visualmente irreales ("borrosas").
