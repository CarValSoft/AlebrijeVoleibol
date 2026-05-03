# AlebrijeVoleibol

Prototipo nativo (Kotlin + Jetpack Compose) de un juego de voleibol temático de alebrijes en modo horizontal obligatorio.

## Incluye en esta base

- Bloqueo de orientación en `landscape` desde `AndroidManifest`.
- Flujo de pantallas:
  - Menú principal con botones **Online** y **Offline**.
  - Pantalla de emparejamiento (mock) para online con VS.
  - Pantalla de selección de dificultad (fácil, medio, difícil).
  - Cancha con marcador al mejor de 15 puntos.
  - Pantalla de resultado (victoria/derrota).
- Touch controllers circulares transparentes:
  - Control de movimiento.
  - Control de golpeo por gesto (drag vectorial).
- Botón de ajustes en esquina superior izquierda:
  - Rendirse.
  - Invertir controles (toggle).
- Estilo visual minimalista/cute con paleta alebrije.
- Base en SVG/vector drawables para recursos UI.

## Pendiente para juego completo

1. Física avanzada (gravedad realista, colisiones red/suelo/personaje, remates precisos).
2. Integración de audio por eventos:
   - Narración inicial.
   - Música de fondo.
   - Música final ganador/perdedor.
   - SFX: salto, golpe, red, suelo, silbato, etc.
3. Integración real de Google Sign-In y backend (Firebase recomendado).
4. Networking multijugador en tiempo real (WebSocket/Firebase Realtime Database/Firestore + sincronización).
5. Arte SVG completo de personajes, UI y objetos temáticos alebrije.

## Ejecutar

```bash
./gradlew assembleDebug
```

> Si aún no tienes wrapper, genera uno con `gradle wrapper` o abre el proyecto en Android Studio y sincroniza.
