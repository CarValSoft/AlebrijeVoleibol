# Control de Rotación Natural y Reactiva

Se ha rediseñado el sistema de giro de la pelota para eliminar la rotación excesiva y convertirla en un movimiento físico natural, dependiente de los impactos reales durante el juego.

## Cambios Realizados

### [MainActivity.kt](file:///E:/dev/android_studio/AlebrijeVoleibol/v1/app/src/main/java/com/alebrije/voleibol/MainActivity.kt)

#### Nuevo Motor de Spin (Giro)
Se ha sustituido la rotación constante por un modelo dinámico de velocidad angular:
- **`ballSpin`**: Ahora la pelota tiene una "velocidad de giro" propia que se ve afectada por el impacto.
- **Fricción Atmosférica**: Se ha añadido una pérdida gradual de giro en el aire (`ballSpin * 0.5f * dt`), lo que hace que la pelota deje de girar lentamente si no es golpeada, tal como ocurre en la realidad.
- **Giro Reactivo**: En la función `checkCollision`, el giro se inyecta basándose en la dirección del golpe (`strike`) y el punto de contacto (`dx`). Un golpe lateral o descentrado provocará un efecto de rotación coherente.

#### Renderizado Limpio
- Se han eliminado los multiplicadores exagerados (`* 50f`) que causaban que la pelota se viera borrosa o girara a velocidades irreales.
- La función `drawAlebrijeBall` ahora recibe directamente el ángulo acumulado en grados, lo que garantiza un movimiento visualmente suave y nítido.

## Verificación

- [x] **Comportamiento:** La pelota ya no gira por defecto de forma frenética; solo reacciona con un giro moderado al ser golpeada.
- [x] **Estética:** El giro se detiene gradualmente, aportando una sensación de realismo deportivo.
- [x] **Compilación:** El proyecto compila correctamente (`assembleDebug` exitoso).

> [!TIP]
> Intenta golpear la pelota con diferentes partes del cuerpo para observar cómo el "spin" cambia de dirección e intensidad de manera natural.
