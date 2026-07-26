package com.alebrije.voleibol

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import android.media.MediaPlayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AlebrijeApp() }
    }
}

private enum class Screen { MENU, MATCHMAKING, DIFFICULTY, COURT, RESULT }
private enum class GameMode { ONLINE, OFFLINE }

@Preview(showBackground = true)
@Composable
fun AlebrijePreview() {
    val ballTexture = ImageBitmap.imageResource(id = R.drawable.ball)
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF6EC6FF))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAlebrije(Offset(200f, 600f), 150f, Color(0xFFEF476F), 0f, true, 0f, Offset.Zero, 0f)
            drawAlebrijeBall(Offset(350f, 300f), 95f, 0f, ballTexture)
        }
    }
}

@Composable
private fun AlebrijeApp() {
    var screen by remember { mutableStateOf(Screen.MENU) }
    var mode by remember { mutableStateOf(GameMode.OFFLINE) }
    var difficulty by remember { mutableStateOf("fácil") }
    var playerScore by remember { mutableIntStateOf(0) }
    var enemyScore by remember { mutableIntStateOf(0) }
    var isMusicMuted by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Gestión global de audio por ciclo de vida
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> soundManager.pauseAll()
                Lifecycle.Event.ON_RESUME -> {
                    if (!isMusicMuted && screen != Screen.COURT) {
                        soundManager.resumeAll()
                    } else {
                        // Si estamos en cancha o silenciado, solo resumimos efectos (SoundPool)
                        soundManager.resumeEffectsOnly()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isMusicMuted, screen) {
        if (isMusicMuted || screen == Screen.COURT) {
            soundManager.stopMusic()
        } else {
            soundManager.startMusic()
        }
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                Screen.MENU -> MainMenu(
                    onOnline = { mode = GameMode.ONLINE; screen = Screen.MATCHMAKING },
                    onOffline = { mode = GameMode.OFFLINE; screen = Screen.DIFFICULTY }
                )
                Screen.MATCHMAKING -> MatchmakingScreen(onMatched = { screen = Screen.COURT })
                Screen.DIFFICULTY -> DifficultyScreen(onSelected = { difficulty = it; screen = Screen.COURT })
                Screen.COURT -> CourtScreen(
                    mode = mode,
                    difficulty = difficulty,
                    onFinish = { me, enemy -> playerScore = me; enemyScore = enemy; screen = Screen.RESULT },
                    soundManager = soundManager
                )
                Screen.RESULT -> ResultScreen(playerScore = playerScore, enemyScore = enemyScore, onHome = { screen = Screen.MENU })
            }

            if (screen != Screen.COURT) {
                CuteButton(
                    text = if (isMusicMuted) "🔇" else "🔊",
                    onClick = { isMusicMuted = !isMusicMuted },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(60.dp, 56.dp),
                    padding = PaddingValues(0.dp)
                )
            }
        }
    }
}

@Composable
private fun MainMenu(onOnline: () -> Unit, onOffline: () -> Unit) {
    ScreenContainer {
        Text("Alebrije Vóleibol", fontSize = 40.sp, color = Color(0xFFFFD166), fontWeight = FontWeight.ExtraBold)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            CuteButton(text = "Online", onClick = onOnline)
            CuteButton(text = "Offline", onClick = onOffline)
        }
        CuteButton(text = "Google Sign-In", onClick = {})
    }
}

@Composable
private fun MatchmakingScreen(onMatched: () -> Unit) {
    ScreenContainer {
        Text("Emparejando alebrijes...", color = Color.White, fontSize = 30.sp)
        Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0x44111A3D))) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Avatar("Tú")
                Text("VS", color = Color(0xFFFF595E), fontSize = 34.sp, fontWeight = FontWeight.Black)
                Avatar("Rival")
            }
        }
        CuteButton(text = "Entrar a cancha", onClick = onMatched)
    }
}

@Composable
private fun DifficultyScreen(onSelected: (String) -> Unit) {
    ScreenContainer {
        Text("Dificultad", color = Color.White, fontSize = 30.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CuteButton("Fácil") { onSelected("fácil") }
            CuteButton("Medio") { onSelected("medio") }
            CuteButton("Difícil") { onSelected("difícil") }
        }
    }
}

@Composable
private fun CourtScreen(mode: GameMode, difficulty: String, onFinish: (Int, Int) -> Unit, soundManager: SoundManager) {
    val context = LocalContext.current
    val haptics = remember { HapticManager(context) }
    val cloudAtlas = ImageBitmap.imageResource(id = R.drawable.cloud_atla)
    val ballTexture = ImageBitmap.imageResource(id = R.drawable.ball)
    
    var playerScore by remember { mutableIntStateOf(0) }
    var enemyScore by remember { mutableIntStateOf(0) }
    var p1Touches by remember { mutableIntStateOf(0) }
    var p2Touches by remember { mutableIntStateOf(0) }
    var lastPointWinner by remember { mutableIntStateOf(0) } // 1: P1, 2: P2
    var scoreEffectTimer by remember { mutableFloatStateOf(0f) }
    var showSettings by remember { mutableStateOf(false) }

    var ballPos by remember { mutableStateOf(Offset(0f, 0f)) }
    var ballVel by remember { mutableStateOf(Offset(0f, 0f)) }
    var ballRot by remember { mutableFloatStateOf(0f) }
    var ballSpin by remember { mutableFloatStateOf(0f) }
    var serveWaitTimer by remember { mutableFloatStateOf(0f) }
    var serveState by remember { mutableIntStateOf(0) }

    var p1Pos by remember { mutableStateOf(Offset(0f, 0f)) }
    var p1Vel by remember { mutableStateOf(Offset(0f, 0f)) }
    var p1WalkCycle by remember { mutableFloatStateOf(0f) }
    var p1StrikeAnim by remember { mutableFloatStateOf(0f) }
    var p1StrikeDir by remember { mutableStateOf(Offset.Zero) }
    var p1SlideTimer by remember { mutableFloatStateOf(0f) }

    var p2Pos by remember { mutableStateOf(Offset(0f, 0f)) }
    var p2Vel by remember { mutableStateOf(Offset(0f, 0f)) }
    var p2WalkCycle by remember { mutableFloatStateOf(0f) }

    var p1MoveVector by remember { mutableStateOf(Offset.Zero) }
    var p1StrikeVector by remember { mutableStateOf(Offset.Zero) }
    val strikeTrail = remember { mutableStateListOf<Offset>() }
    var strikeForceIndicator by remember { mutableFloatStateOf(0f) }

    val clouds = remember { mutableStateListOf<Cloud>() }
    val palms = remember { mutableStateListOf<Palm>() }
    val particles = remember { mutableStateListOf<Particle>() }
    val confetti = remember { mutableStateListOf<Particle>() }
    var netVibration by remember { mutableFloatStateOf(0f) }
    var audiencePulse by remember { mutableFloatStateOf(0f) }

    if (playerScore >= 3 || enemyScore >= 3) {
        DisposableEffect(Unit) {
            if (playerScore >= 3) soundManager.play("winner") else soundManager.play("looser")
            onFinish(playerScore, enemyScore)
            onDispose { }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF6EC6FF))) {
        val width = constraints.maxWidth.toFloat(); val height = constraints.maxHeight.toFloat()
        val floorY = height * 0.9f; val netWidth = 15f; val netHeight = 150f
        val ballRadius = 95f; val playerSize = 140f
        val beachHeight = 200f // Altura de la arena mayor que la red (150f)

        LaunchedEffect(width) {
            clouds.clear() // Forzamos limpieza para mostrar el nuevo atlas pixel art
            repeat(8) { clouds.add(Cloud(Random.nextFloat()*width, Random.nextFloat()*height*0.22f, Random.nextFloat()*10f+5f, Random.nextFloat()*15f+20f, it % 8)) }
            if (palms.isEmpty()) {
                palms.add(Palm(width*0.05f, floorY - beachHeight, 220f, -8f, 0))
                palms.add(Palm(width*0.95f, floorY - beachHeight, 200f, 6f, 1))
            }
        }

        LaunchedEffect(width, height) {
            val serveLeft = Random.nextBoolean()
            ballPos = if (serveLeft) Offset(width * 0.25f, 200f) else Offset(width * 0.75f, 200f)
            ballVel = Offset.Zero; serveWaitTimer = 2.5f; serveState = 0
            p1Pos = Offset(width * 0.25f, floorY - playerSize/2); p2Pos = Offset(width * 0.75f, floorY - playerSize/2)

            var lastFrameTime = withFrameNanos { it }
            while (true) {
                withFrameNanos { frameTime ->
                    val dt = (frameTime - lastFrameTime) / 1_000_000_000f; lastFrameTime = frameTime
                    // Gravedad Asimétrica: Pesadez natural
                    val gravityRising = 5200f
                    val gravityFalling = 8800f
                    val baseMoveSpeed = 1550f 
                    val jumpImpulse = -1800f 

                    audiencePulse += dt * 2f
                    if (scoreEffectTimer > 0f) scoreEffectTimer -= dt

                    if (serveWaitTimer > 0) {
                        serveWaitTimer -= dt
                        if (serveWaitTimer <= 0 && serveState == 0) { 
                            serveState = 1; 
                            ballVel = Offset(Random.nextFloat()*70f-35f, -600f) // 140->70, 1200->600 (50% más lento)
                            soundManager.play("saque")
                            p1Touches = 0; p2Touches = 0
                        }
                    }

                    val ballOnLeftPrev = ballPos.x < width / 2
                    if (serveState > 0 || serveWaitTimer <= 0) {
                        val bGrav = if (ballVel.y < 0) gravityRising else gravityFalling
                        ballVel = ballVel.copy(y = ballVel.y + bGrav * dt); ballPos += ballVel * dt
                        
                        // Rotación natural: fricción y acumulación
                        ballSpin -= ballSpin * 0.5f * dt 
                        ballRot += ballSpin * dt
                    }
                    val ballOnLeftNow = ballPos.x < width / 2
                    if (ballOnLeftPrev != ballOnLeftNow) {
                        if (ballOnLeftNow) p2Touches = 0 else p1Touches = 0
                    }

                    if (ballPos.x < ballRadius) { ballPos = ballPos.copy(x = ballRadius); ballVel = ballVel.copy(x = -ballVel.x * 0.8f) }
                    else if (ballPos.x > width - ballRadius) { ballPos = ballPos.copy(x = width - ballRadius); ballVel = ballVel.copy(x = -ballVel.x * 0.8f) }
                    if (ballPos.y < ballRadius) { ballPos = ballPos.copy(y = ballRadius); ballVel = ballVel.copy(y = -ballVel.y * 0.8f) }

                    if (ballPos.y > floorY - ballRadius) {
                        if (ballPos.x < width / 2) {
                            enemyScore++; lastPointWinner = 2; soundManager.play("loos")
                            spawnConfetti(confetti, width * 0.55f, width * 0.9f, floorY - 100f)
                        } else {
                            playerScore++; lastPointWinner = 1; soundManager.play("win")
                            spawnConfetti(confetti, width * 0.1f, width * 0.45f, floorY - 100f)
                        }
                        scoreEffectTimer = 1.0f
                        p1Touches = 0; p2Touches = 0
                        soundManager.play("suelo"); haptics.vibrate(100); spawnParticles(particles, ballPos, Color.White)
                        val winnerLeft = ballPos.x > width / 2; ballPos = if (winnerLeft) Offset(width * 0.25f, 200f) else Offset(width * 0.75f, 200f)
                        ballVel = Offset.Zero; ballRot = 0f; ballSpin = 0f; serveWaitTimer = 2.5f; serveState = 0
                    }

                    val netRectLeft = width / 2 - netWidth / 2; val netRectRight = width / 2 + netWidth / 2; val netRectTop = floorY - netHeight
                    if (ballPos.x + ballRadius > netRectLeft && ballPos.x - ballRadius < netRectRight && ballPos.y + ballRadius > netRectTop) {
                        netVibration = 1.0f
                        soundManager.play("red")
                        if (ballPos.y > netRectTop + 10f) { ballVel = ballVel.copy(x = -ballVel.x * 0.5f); ballPos = ballPos.copy(x = if (ballPos.x < width / 2) netRectLeft - ballRadius else netRectRight + ballRadius) }
                        else { ballVel = ballVel.copy(y = -ballVel.y * 0.8f); ballPos = ballPos.copy(y = netRectTop - ballRadius) }
                    }

                    if (p1SlideTimer > 0) p1SlideTimer -= dt
                    val moveSpeed = if (p1SlideTimer > 0) baseMoveSpeed * 3.0f else baseMoveSpeed 
                    val targetP1VelX = p1MoveVector.x * moveSpeed
                    p1Vel = p1Vel.copy(x = p1Vel.x + (targetP1VelX - p1Vel.x) * 12f * dt)
                    if (p1MoveVector.y < -0.3f && p1Pos.y >= floorY - playerSize/2 - 5f) p1Vel = p1Vel.copy(y = jumpImpulse)
                    
                    val p1Grav = if (p1Vel.y < 0) gravityRising else gravityFalling
                    p1Vel = p1Vel.copy(y = p1Vel.y + p1Grav * dt); p1Pos += p1Vel * dt
                    
                    if (p1Pos.y > floorY - playerSize/2) { p1Pos = p1Pos.copy(y = floorY - playerSize/2); p1Vel = p1Vel.copy(y = 0f) }
                    p1Pos = p1Pos.copy(x = p1Pos.x.coerceIn(playerSize/2, width/2 - netWidth/2 - playerSize/2))
                    if (abs(p1Vel.x) > 10f && p1Pos.y >= floorY - playerSize/2 - 1f) p1WalkCycle += abs(p1Vel.x) * dt * 0.05f
                    if (p1StrikeAnim > 0f) p1StrikeAnim -= dt * 4f

                    val aiSpeed = when(difficulty.lowercase()) { "fácil" -> 540f; "medio" -> 960f; else -> 1500f }
                    var predictedX = ballPos.x
                    if (ballVel.y > 0) { val timeToLand = (floorY - ballPos.y) / (ballVel.y + 1f); predictedX = ballPos.x + ballVel.x * timeToLand }
                    
                    // IA con barridas salvadoras
                    val aiCanSlide = p2Pos.y >= floorY - playerSize/2 - 5f
                    if (ballPos.x > width / 2 && aiCanSlide && abs(ballPos.x - p2Pos.x) > 200f && abs(ballPos.x - p2Pos.x) < 500f && ballPos.y > floorY - 300f) {
                        if (Random.nextFloat() < 0.05f) {
                            p2Vel = p2Vel.copy(x = (ballPos.x - p2Pos.x).coerceIn(-1f, 1f) * baseMoveSpeed * 3.5f)
                            soundManager.play("barrida")
                        }
                    }

                    predictedX = predictedX.coerceIn(width/2 + netWidth/2 + playerSize/2, width - playerSize/2)
                    val aiTargetX = if (ballPos.x > width * 0.4f) predictedX else width * 0.75f
                    val aiDiffX = aiTargetX - p2Pos.x
                    val targetP2VelX = (if (abs(aiDiffX) > 20f) aiDiffX.coerceIn(-1f, 1f) else 0f) * aiSpeed
                    p2Vel = p2Vel.copy(x = p2Vel.x + (targetP2VelX - p2Vel.x) * 10f * dt)
                    if (ballPos.x > width/2 && ballPos.y < floorY - 400f && abs(ballPos.x - p2Pos.x) < 150f && p2Pos.y >= floorY - playerSize/2 - 5f) {
                        if (Random.nextFloat() < (if (difficulty == "difícil") 0.15f else 0.05f)) p2Vel = p2Vel.copy(y = jumpImpulse)
                    }
                    val p2Grav = if (p2Vel.y < 0) gravityRising else gravityFalling
                    p2Vel = p2Vel.copy(y = p2Vel.y + p2Grav * dt); p2Pos += p2Vel * dt
                    if (p2Pos.y > floorY - playerSize/2) { p2Pos = p2Pos.copy(y = floorY - playerSize/2); p2Vel = p2Vel.copy(y = 0f) }
                    p2Pos = p2Pos.copy(x = p2Pos.x.coerceIn(width/2 + netWidth/2 + playerSize/2, width - playerSize/2))
                    if (abs(p2Vel.x) > 10f && p2Pos.y >= floorY - playerSize/2 - 1f) p2WalkCycle += abs(p2Vel.x) * dt * 0.05f

                    val hitRange = ballRadius + playerSize/2 + 10f 
                    fun checkCollision(pPos: Offset, isP1: Boolean, pVel: Offset) {
                        val dx = ballPos.x - pPos.x; val dy = ballPos.y - pPos.y; val dist = sqrt(dx*dx + dy*dy)
                        if (dist < hitRange) {
                            if (!isP1 && p2Touches >= 4) return 
                            val strike = if (isP1) p1StrikeVector else {
                                val remaining = 4 - p2Touches
                                val aggressiveness = when(remaining) {
                                    1 -> 1.5f 
                                    2 -> 1.2f 
                                    else -> 1.0f
                                }
                                val targetX = width * Random.nextFloat() * 0.45f 
                                Offset((targetX - ballPos.x) * aggressiveness, (floorY * 0.15f - ballPos.y) * aggressiveness)
                            }
                            val force = strike.getDistance(); val isS = if (isP1) force > 30f else force > 10f
                            if (isP1 && isS) { p1StrikeAnim = 1.0f; p1StrikeDir = strike; strikeForceIndicator = (force / 400f).coerceIn(0f, 1f) }
                            
                            // Velocidades reducidas para mayor realismo
                            val vImp = if (isS) -1350f else -850f
                            val fFact = if (isP1) (if (isS) 0.65f else 0.35f) else (if (isS) 0.9f else 0.7f)

                            ballVel = Offset((dx * 4f + strike.x * 1.5f + pVel.x * 0.4f) * fFact, (vImp + strike.y * 1.5f + pVel.y * 0.2f) * fFact)
                            
                            // Inyectar giro reactivo basado en el impacto
                            ballSpin = (strike.x * 5f + dx * 10f).coerceIn(-1500f, 1500f)

                            soundManager.play(if (isS) "golpeo" else "golpe"); spawnParticles(particles, ballPos, if (isP1) Color(0xFFEF476F) else Color(0xFF2EC4B6))
                            if (isP1) p1Touches++ else p2Touches++
                            
                            val normal = Offset(dx/dist, dy/dist)
                            ballPos = pPos + normal * (hitRange + 5f) 
                            if (ballVel.y > -150f) ballVel = ballVel.copy(y = -350f)
                        }
                    }
                    checkCollision(p1Pos, true, p1Vel); checkCollision(p2Pos, false, p2Vel)

                    particles.forEach { it.update(dt) }; particles.removeAll { it.life <= 0 }
                    confetti.forEach { it.update(dt) }; confetti.removeAll { it.life <= 0 }
                    clouds.forEach { it.update(dt, width) }
                    if (strikeForceIndicator > 0f) strikeForceIndicator -= dt * 0.5f
                    if (netVibration > 0f) netVibration -= dt * 5f
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            // Sky Gradient Texture
            drawRect(Brush.verticalGradient(listOf(Color(0xFF6EC6FF), Color(0xFFB3E5FC))), Offset.Zero, size)
            for(i in 0..20) {
                val y = i * (canvasHeight / 20f)
                drawLine(Color.White.copy(0.05f), Offset(0f, y), Offset(canvasWidth, y), 1f)
            }

            drawActiveSea(width, height, floorY - beachHeight, audiencePulse)
            
            // Arena/Playa amplia que divide el mar de la cancha
            drawTexturedRect(Color(0xFFF5DEB3), Offset(0f, floorY - beachHeight), androidx.compose.ui.geometry.Size(width, beachHeight), 99)

            // Audience Bleachers y Palmas encima de la arena
            drawBleachers(width, height, floorY - beachHeight, audiencePulse)
            palms.forEach { drawPalm(Offset(it.x, it.y), it.height, it.tilt, it.variant) }

            // Suelo de la cancha (Cancha completa beige claro)
            val canchaColor = Color(0xFFF5F5DC) // Beige claro (Beige)
            drawTexturedRect(canchaColor, Offset(0f, floorY), androidx.compose.ui.geometry.Size(width, height - floorY), 20)

            // Líneas de la cancha
            drawLine(Color.White, Offset(0f, floorY + 5f), Offset(width, floorY + 5f), 5f) // Línea base
            drawLine(Color.White, Offset(width/2f, floorY), Offset(width/2f, height), 5f) // Línea Central (bajo la red)

            val vibX = sin(netVibration * 30f) * 10f * netVibration
            val netTop = floorY - netHeight
            drawRect(Color(0xFF555555), Offset(width/2 - netWidth/2 + vibX, netTop), androidx.compose.ui.geometry.Size(netWidth, netHeight))
            
            // 3 Líneas verticales desde el tope de la red hasta el fondo (Dibujadas después para estar encima)
            val lineX = width/2 + vibX
            val bottomY = size.height
            val dashColor = Color(0xFF777777) // Un poco más claro que la red para que se noten encima
            // Round Dot
            drawLine(
                color = dashColor,
                start = Offset(lineX - 3f, netTop),
                end = Offset(lineX - 3f, bottomY),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 10f), 0f)
            )
            // Square Dot
            drawLine(
                color = dashColor,
                start = Offset(lineX, netTop),
                end = Offset(lineX, bottomY),
                strokeWidth = 2f,
                cap = StrokeCap.Butt,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 10f), 0f)
            )
            // Dash Dot
            drawLine(
                color = dashColor,
                start = Offset(lineX + 3f, netTop),
                end = Offset(lineX + 3f, bottomY),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 5f, 2f, 5f), 0f)
            )
            
            if (strikeForceIndicator > 0.01f) {
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Yellow, Color.Red)), Offset(width/2 - netWidth/2 + vibX, floorY - netHeight * strikeForceIndicator), androidx.compose.ui.geometry.Size(netWidth, netHeight * strikeForceIndicator))
            }
            if (strikeTrail.isNotEmpty()) {
                val path = Path(); strikeTrail.forEachIndexed { i, o -> if (i==0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
                drawPath(path, Color(0x66FFD166), style = Stroke(12f, cap = StrokeCap.Round))
            }
            drawAlebrije(p1Pos, playerSize, Color(0xFFEF476F), p1WalkCycle, p1Vel.x > 0, p1StrikeAnim, p1StrikeDir, p1SlideTimer)
            drawAlebrije(p2Pos, playerSize, Color(0xFF2EC4B6), p2WalkCycle, p2Vel.x > 0, 0f, Offset.Zero, 0f)
            particles.forEach { drawCircle(it.color.copy(it.life), it.size, it.pos) }
            confetti.forEach { drawRect(it.color.copy(it.life.coerceIn(0f, 1f)), it.pos, androidx.compose.ui.geometry.Size(12f, 12f)) }
            clouds.forEach { drawCloud(Offset(it.x, it.y), it.size, it.variant, cloudAtlas) }
        }

        Box(modifier = Modifier.fillMaxSize().zIndex(10f)) {
            val destelloColor = if (scoreEffectTimer > 0f) {
                if (lastPointWinner == 1) Color.Green.copy(scoreEffectTimer * 0.4f)
                else Color.Red.copy(scoreEffectTimer * 0.4f)
            } else Color.Transparent

            Box(modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp)
                .background(destelloColor, RoundedCornerShape(14.dp))
                .padding(6.dp)
                .background(Color(0xFFE5D5B0), RoundedCornerShape(14.dp))
                .padding(3.dp)
                .background(Color(0xFF282828), RoundedCornerShape(11.dp))
                .padding(horizontal = 28.dp, vertical = 10.dp)
            ) {
                Text("${playerScore} - ${enemyScore}", fontSize = 36.sp, color = Color(0xFFFFD166), fontWeight = FontWeight.Black)
            }
            CuteButton(text = "⚙", onClick = { showSettings = !showSettings }, modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(60.dp, 42.dp).zIndex(999f), padding = PaddingValues(horizontal = 5.dp, vertical = 2.dp))
        }

        Canvas(modifier = Modifier.fillMaxSize().zIndex(20f)) {
            drawAlebrijeBall(ballPos, ballRadius, ballRot, ballTexture)
        }

        if (showSettings) {
            Column(modifier = Modifier.padding(20.dp).align(Alignment.Center).clip(RoundedCornerShape(16.dp)).background(Color(0xDD1F2041)).padding(12.dp).zIndex(1000f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CuteButton("Rendirme") { enemyScore = 15 }; CuteButton("Cerrar") { showSettings = false }
            }
        }

        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            awaitEachGesture {
                val activeTouches = mutableMapOf<PointerId, Offset>()
                while (true) {
                    val event = awaitPointerEvent()
                    // Consumir el evento para intentar evitar que el sistema lo interprete como gestos de navegación
                    event.changes.forEach { it.consume() }
                    
                    event.changes.forEach { pointer ->
                        if (pointer.pressed) {
                            val pos = pointer.position
                            val lastPos = activeTouches[pointer.id] ?: pos
                            val drag = pos - lastPos
                            if (pos.x < size.width / 2) {
                                // Slide check
                                if (abs(drag.x) > 40f && p1SlideTimer <= 0f && p1Pos.y >= floorY - playerSize/2 - 10f) {
                                    p1SlideTimer = 0.5f
                                }
                                p1MoveVector = Offset((drag.x * 0.15f).coerceIn(-1.1f, 1.1f), (drag.y * 0.15f).coerceIn(-1.1f, 1.1f))
                            } else {
                                if (drag.getDistance() > 2f) {
                                    p1StrikeVector += drag
                                    if (strikeTrail.size > 15) strikeTrail.removeAt(0); strikeTrail.add(pos)
                                }
                            }
                            activeTouches[pointer.id] = pos
                        } else {
                            if (pointer.position.x < size.width / 2) p1MoveVector = Offset.Zero
                            else { p1StrikeVector = Offset.Zero; strikeTrail.clear() }
                            activeTouches.remove(pointer.id)
                        }
                    }
                    if (event.changes.none { it.pressed }) break
                }
            }
        })
    }
}

private fun DrawScope.drawTexturedRect(color: Color, offset: Offset, size: androidx.compose.ui.geometry.Size, seed: Int = 0) {
    drawRect(color, offset, size)
    val random = Random(seed)
    val lineCount = (size.height / 4f).toInt().coerceIn(5, 100)
    for (i in 0 until lineCount) {
        val y = offset.y + (size.height / lineCount) * i
        val thickness = random.nextFloat() * 2f + 1f
        val alpha = random.nextFloat() * 0.15f + 0.05f
        val lColor = if (color.luminance() > 0.5f) Color.Black.copy(alpha) else Color.White.copy(alpha)
        drawLine(lColor, Offset(offset.x, y), Offset(offset.x + size.width, y + (random.nextFloat() - 0.5f) * 2f), thickness)
    }
}

private fun DrawScope.drawActiveSea(w: Float, h: Float, floorY: Float, time: Float) {
    val seaColor = Color(0xFF008B99)
    val seaTop = floorY - 100f // Ajustado para arena mayor
    
    // Base Sea with texture
    drawTexturedRect(seaColor, Offset(0f, seaTop), androidx.compose.ui.geometry.Size(w, h - seaTop), 42)
    
    // Waves
    for (i in 0..2) {
        val waveAlpha = 0.3f - i * 0.1f
        val waveColor = Color.White.copy(alpha = waveAlpha)
        val path = Path()
        path.moveTo(0f, seaTop + i * 15f)
        for (x in 0..w.toInt() step 20) {
            val waveHeight = sin(x * 0.01f + time * 0.5f + i) * 15f // Velocidad neutralizada (2f -> 0.5f)
            path.lineTo(x.toFloat(), seaTop + waveHeight + i * 15f)
        }
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        drawPath(path, waveColor)
        
        // Sea strokes like the image
        for (j in 0..5) {
            val strokeY = seaTop + 30f + j * 40f + sin(time * 0.5f + j) * 10f
            val strokeWidth = 100f + sin(time * 0.2f + j) * 50f
            val strokeX = (time * 25f + j * 200f) % (w + 200f) - 100f // Velocidad neutralizada (100f -> 25f)
            drawLine(Color.White.copy(0.1f), Offset(strokeX, strokeY), Offset(strokeX + strokeWidth, strokeY), 2f, StrokeCap.Round)
        }
    }
}

private class Cloud(var x: Float, var y: Float, val speed: Float, val size: Float, val variant: Int) {
    fun update(dt: Float, width: Float) { x += speed * dt; if (x > width + size * 3) x = -size * 3 }
}
private class Palm(val x: Float, val y: Float, val height: Float, val tilt: Float, val variant: Int)

private fun DrawScope.drawPalm(pos: Offset, h: Float, tilt: Float, variant: Int) {
    val leafCount = 5 + variant
    withTransform({ 
        translate(pos.x, pos.y)
        rotate(tilt, Offset.Zero) 
    }) {
        drawLine(Color(0xFF5D2E0A), Offset.Zero, Offset(0f, -h), 22f, StrokeCap.Round)
        for (i in 0 until leafCount) {
            val rad = Math.toRadians((i * (180f/leafCount) - 135f + (variant * 15f)).toDouble())
            drawLine(if (variant == 0) Color(0xFF228B22) else Color(0xFF1B6A1B), Offset(0f, -h), Offset(cos(rad).toFloat()*(90f+i*10f), -h+sin(rad).toFloat()*(70f+i*5f)), 14f, StrokeCap.Round)
        }
    }
}


private fun DrawScope.drawCloud(pos: Offset, size: Float, variant: Int, atlas: ImageBitmap) {
    val cols = 4; val rows = 2
    val spriteW = atlas.width / cols; val spriteH = atlas.height / rows
    val col = variant % cols; val row = (variant / cols) % rows
    
    val srcOffset = IntOffset(col * spriteW, row * spriteH)
    val srcSize = IntSize(spriteW, spriteH)
    
    val ratio = spriteH.toFloat() / spriteW.toFloat()
    val destW = size * 4.5f 
    val destH = destW * ratio
    
    drawImage(
        image = atlas,
        srcOffset = srcOffset,
        srcSize = srcSize,
        dstOffset = IntOffset((pos.x - destW/2).toInt(), (pos.y - destH/2).toInt()),
        dstSize = IntSize(destW.toInt(), destH.toInt())
    )
}

private fun DrawScope.drawBleachers(w: Float, h: Float, floorY: Float, pulse: Float) {
    val bW = w * 0.35f; val bH = 100f
    val bleacherColor = Color.White
    
    // Dos gradas blancas con bordes grises
    listOf(w * 0.1f, w * 0.55f).forEachIndexed { idx, bX ->
        val borderColor = if (idx == 0) Color(0xFFD3D3D3) else Color(0xFFA9A9A9)
        val rect = androidx.compose.ui.geometry.Rect(bX, floorY - 50f, bX + bW, floorY - 50f + bH)
        
        drawRect(bleacherColor, rect.topLeft, rect.size)
        drawRect(borderColor, rect.topLeft, rect.size, style = Stroke(4f))
        
        // Público de alebrijes complejos
        for (row in 0..1) {
            val rowY = floorY - 50f + row * 40f
            for (i in 0..6) {
                val audienceX = bX + 30f + i * (bW - 60f) / 6f
                val phase = i * 0.5f + row * 1.2f + idx * 2.5f
                val jump = sin(pulse * (1.2f + (i % 3) * 0.2f) + phase) * (10f + (row * 5f))
                val randomSeed = (i + row * 10 + idx * 100).toLong()
                val alebrijeColor = Color(Random(randomSeed).nextInt()).copy(alpha = 1f)
                
                // Dibujar alebrijes (Cabeza, cuerpo, pies, manos)
                drawAlebrije(
                    pos = Offset(audienceX, rowY + jump),
                    size = 40f,
                    color = alebrijeColor,
                    walk = pulse + i,
                    faceR = i % 2 == 0,
                    sAnim = 0f,
                    sDir = Offset.Zero,
                    slide = 0f,
                    cheer = pulse * 2f + i // Mayor frecuencia para alentar
                )
            }
        }
    }
}

private fun DrawScope.drawAlebrije(pos: Offset, size: Float, color: Color, walk: Float, faceR: Boolean, sAnim: Float, sDir: Offset, slide: Float, cheer: Float = 0f) {
    withTransform({ 
        translate(pos.x, pos.y)
        scale(if (faceR) 1f else -1f, 1f, Offset.Zero) 
    }) {
        val sF = sDir.getDistance().coerceIn(0f, 120f); val bT = if (sAnim > 0) (sDir.x / 8f) * sAnim else if (slide > 0) 30f else 0f
        withTransform({ rotate(bT, Offset.Zero) }) {
            val shake = if (sAnim > 0) sin(sAnim * 35f) * 10f else 0f
            translate(shake, 0f) { 
                drawCircle(color, size * 0.43f, Offset.Zero) // Cuerpo
                // Character Texture
                for(i in 0..5) {
                    val angle = i * 60f
                    val rx = cos(Math.toRadians(angle.toDouble())).toFloat() * size * 0.3f
                    val ry = sin(Math.toRadians(angle.toDouble())).toFloat() * size * 0.3f
                    drawLine(Color.White.copy(0.15f), Offset(rx - 10f, ry), Offset(rx + 10f, ry), 2f)
                }
                drawCircle(color, size * 0.27f, Offset(0f, -size * 0.55f)) // Cabeza
                drawCircle(Color.White, size * 0.05f, Offset(size * 0.1f, -size * 0.58f)) // Ojo
                drawCircle(Color.Black, size * 0.02f, Offset(size * 0.12f, -size * 0.58f)) 
            }
        }
        val legS = if (sAnim > 0) sin(sAnim * 25f) * size * 0.2f else if (slide > 0) size * 0.4f else sin(walk) * size * 0.25f
        val legY = if (slide > 0) -size * 0.2f else if (sAnim > 0.5f) -size * 0.1f * sAnim else 0f
        drawLine(color, Offset(-size * 0.12f, size * 0.3f), Offset(-size * 0.12f + legS, size * 0.55f + legY), size * 0.15f, StrokeCap.Round) // Pies
        drawLine(color, Offset(size * 0.12f, size * 0.3f), Offset(size * 0.12f - legS, size * 0.55f + legY), size * 0.15f, StrokeCap.Round)
        
        // 1st Hand
        val cheerY = if (cheer > 0f) (sin(cheer) * 0.5f + 0.5f) * size * 0.8f else 0f
        val h1Off = if (sAnim > 0) Offset(sDir.x.coerceIn(-120f, 120f), sDir.y.coerceIn(-120f, 120f)) * sAnim else Offset(0f, -cheerY)
        val aB1 = Offset(size * 0.35f, -size * 0.12f); val aE1 = Offset(size * 0.65f + h1Off.x, -size * 0.35f + h1Off.y)
        val elb1 = (aB1 + aE1) / 2f + Offset(0f, -sF * 0.25f * sAnim)
        drawLine(color, aB1, elb1, size * 0.14f, StrokeCap.Round); drawLine(color, elb1, aE1, size * 0.14f, StrokeCap.Round); drawCircle(color, size * 0.09f, aE1)
        
        // 2nd Hand
        val h2Swing = if (slide > 0) size * 0.3f else if (cheer > 0f) 0f else sin(walk + 1f) * size * 0.1f
        val aB2 = Offset(-size * 0.35f, -size * 0.12f)
        val aE2 = if (cheer > 0f) Offset(-size * 0.65f, -size * 0.35f - cheerY) else Offset(-size * 0.45f - h2Swing, size * 0.3f)
        drawLine(color, aB2, aE2, size * 0.12f, StrokeCap.Round); drawCircle(color, size * 0.08f, aE2)
    }
}

private fun DrawScope.drawAlebrijeBall(pos: Offset, radius: Float, rotation: Float, texture: ImageBitmap) {
    withTransform({
        rotate(rotation, pos)
    }) {
        drawImage(
            image = texture,
            dstOffset = IntOffset((pos.x - radius).toInt(), (pos.y - radius).toInt()),
            dstSize = IntSize((radius * 2).toInt(), (radius * 2).toInt())
        )
    }
}

@Composable
private fun CuteButton(text: String, modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(0.dp), onClick: () -> Unit) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    Button(
        onClick = {
            soundManager.play("toque")
            onClick()
        }, 
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF476F), contentColor = Color.White), 
        shape = RoundedCornerShape(18.dp), 
        modifier = modifier.height(56.dp).width(180.dp), 
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.matchParentSize().alpha(0.2f)) {
                for(i in 0..5) {
                    val y = i * (size.height / 5f)
                    drawLine(Color.White, Offset(0f, y), Offset(size.width, y), 2f)
                }
            }
            Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(padding))
        }
    }
}

private class SoundManager(context: Context) {
    private val soundPool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).build()
    private val sounds = mutableMapOf<String, Int>()
    private var mediaPlayer: MediaPlayer? = null
    private val ctx = context

    init {
        listOf("suelo", "red", "golpe", "toque", "barrida", "golpeo", "win", "loos", "winner", "looser", "saque").forEach { name ->
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId != 0) sounds[name] = soundPool.load(context, resId, 1)
        }
    }

    fun startMusic() {
        if (mediaPlayer == null) {
            val resId = ctx.resources.getIdentifier("bg_music", "raw", ctx.packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(ctx, resId).apply {
                    isLooping = true
                    setVolume(0.4f, 0.4f)
                    start()
                }
            }
        } else if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
        }
    }

    fun stopMusic() {
        mediaPlayer?.pause()
    }

    fun pauseAll() {
        mediaPlayer?.pause()
        soundPool.autoPause()
    }

    fun resumeAll() {
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
        }
        soundPool.autoResume()
    }

    fun resumeEffectsOnly() {
        soundPool.autoResume()
    }

    fun play(name: String) { sounds[name]?.let { soundPool.play(it, 1f, 1f, 0, 0, 1f) } }

    fun release() {
        soundPool.release()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

private class HapticManager(context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    fun vibrate(duration: Long) { if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)) }
}

private class Particle(var pos: Offset, val color: Color) {
    var vel = Offset(Random.nextFloat()*200-100, Random.nextFloat()*200-100); var life = 1.0f; var size = Random.nextFloat() * 10 + 5
    fun update(dt: Float) { pos += vel * dt; life -= dt * 2f }
}

private fun spawnParticles(list: MutableList<Particle>, pos: Offset, color: Color) { repeat(10) { list.add(Particle(pos, color)) } }

private fun spawnConfetti(list: MutableList<Particle>, xMin: Float, xMax: Float, yBase: Float) {
    val colors = listOf(Color.Yellow, Color.Red, Color.Cyan, Color.Green, Color.Magenta)
    repeat(40) {
        list.add(Particle(
            Offset(Random.nextFloat() * (xMax - xMin) + xMin, yBase),
            colors.random()
        ).apply {
            vel = Offset(Random.nextFloat() * 200f - 100f, -Random.nextFloat() * 400f - 200f)
            life = 2.0f
        })
    }
}

@Composable
private fun ResultScreen(playerScore: Int, enemyScore: Int, onHome: () -> Unit) {
    ScreenContainer {
        Text(if (playerScore > enemyScore) "¡Ganaste!" else "Derrota", fontSize = 44.sp, color = if (playerScore > enemyScore) Color(0xFFFFD166) else Color(0xFFFF595E), fontWeight = FontWeight.ExtraBold)
        Text("Marcador: $playerScore - $enemyScore", color = Color.White, fontSize = 26.sp)
        CuteButton(text = "Volver al menú", onClick = onHome)
    }
}

@Composable
private fun ScreenContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1F2041)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically), content = content)
}

@Composable
private fun Avatar(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(Color(0xFF2EC4B6))); Text(name, color = Color.White) }
}
