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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlebrijeApp()
        }
    }
}

private enum class Screen {
    MENU, MATCHMAKING, DIFFICULTY, COURT, RESULT
}

private enum class GameMode {
    ONLINE, OFFLINE
}

@Preview(showBackground = true)
@Composable
fun AlebrijePreview() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF6EC6FF))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAlebrije(Offset(200f, 400f), 150f, Color(0xFFEF476F), 0f, true, 0f, Offset.Zero)
            drawAlebrije(Offset(500f, 400f), 150f, Color(0xFF2EC4B6), 0.5f, false, 0f, Offset.Zero)
            drawAlebrijeBall(Offset(350f, 200f), 110f, 1.5f)
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

    MaterialTheme {
        when (screen) {
            Screen.MENU -> MainMenu(
                onOnline = {
                    mode = GameMode.ONLINE
                    screen = Screen.MATCHMAKING
                },
                onOffline = {
                    mode = GameMode.OFFLINE
                    screen = Screen.DIFFICULTY
                }
            )

            Screen.MATCHMAKING -> MatchmakingScreen(onMatched = { screen = Screen.COURT })
            Screen.DIFFICULTY -> DifficultyScreen(onSelected = {
                difficulty = it
                screen = Screen.COURT
            })

            Screen.COURT -> CourtScreen(
                mode = mode,
                difficulty = difficulty,
                onFinish = { me, enemy ->
                    playerScore = me
                    enemyScore = enemy
                    screen = Screen.RESULT
                }
            )

            Screen.RESULT -> ResultScreen(
                playerScore = playerScore,
                enemyScore = enemyScore,
                onHome = { screen = Screen.MENU }
            )
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
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
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
private fun CourtScreen(mode: GameMode, difficulty: String, onFinish: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val haptics = remember { HapticManager(context) }
    
    var playerScore by remember { mutableIntStateOf(0) }
    var enemyScore by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    
    var ballPos by remember { mutableStateOf(Offset(0f, 0f)) }
    var ballVel by remember { mutableStateOf(Offset(0f, 0f)) }
    var ballRotation by remember { mutableFloatStateOf(0f) }
    var serveWaitTimer by remember { mutableFloatStateOf(0f) }
    var serveState by remember { mutableIntStateOf(0) }
    
    var p1Pos by remember { mutableStateOf(Offset(0f, 0f)) }
    var p1Vel by remember { mutableStateOf(Offset(0f, 0f)) }
    var p1WalkCycle by remember { mutableFloatStateOf(0f) }
    var p1StrikeAnim by remember { mutableFloatStateOf(0f) }
    var p1StrikeDir by remember { mutableStateOf(Offset.Zero) }
    
    var p2Pos by remember { mutableStateOf(Offset(0f, 0f)) }
    var p2Vel by remember { mutableStateOf(Offset(0f, 0f)) }
    var p2WalkCycle by remember { mutableFloatStateOf(0f) }
    
    var p1MoveVector by remember { mutableStateOf(Offset.Zero) }
    var p1StrikeVector by remember { mutableStateOf(Offset.Zero) }
    val strikeTrail = remember { mutableStateListOf<Offset>() }
    var strikeForceIndicator by remember { mutableFloatStateOf(0f) }
    
    val clouds = remember { mutableStateListOf<Cloud>() }
    val particles = remember { mutableStateListOf<Particle>() }

    if (playerScore >= 15 || enemyScore >= 15) {
        DisposableEffect(Unit) {
            if (playerScore >= 15) soundManager.play("winner") else soundManager.play("looser")
            onFinish(playerScore, enemyScore)
            onDispose { }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF6EC6FF))) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val floorY = height * 0.9f
        val netWidth = 15f
        val netHeight = 150f
        val ballRadius = 110f
        val playerSize = 140f
        
        LaunchedEffect(width) {
            repeat(5) { clouds.add(Cloud(Random.nextFloat() * width, Random.nextFloat() * height * 0.3f, Random.nextFloat() * 30f + 10f)) }
        }

        LaunchedEffect(width, height) {
            val serveLeft = Random.nextBoolean()
            ballPos = if (serveLeft) Offset(width * 0.25f, 200f) else Offset(width * 0.75f, 200f)
            ballVel = Offset.Zero
            serveWaitTimer = 2.5f
            serveState = 0
            p1Pos = Offset(width * 0.25f, floorY - playerSize/2)
            p2Pos = Offset(width * 0.75f, floorY - playerSize/2)

            var lastFrameTime = withFrameNanos { it }
            while (true) {
                withFrameNanos { frameTime ->
                    val dt = (frameTime - lastFrameTime) / 1_000_000_000f
                    lastFrameTime = frameTime
                    val gravity = 2500f
                    val moveSpeed = 1650f
                    val jumpImpulse = -1400f

                    if (serveWaitTimer > 0) {
                        serveWaitTimer -= dt
                        if (serveWaitTimer <= 0 && serveState == 0) {
                            serveState = 1
                            ballVel = Offset(Random.nextFloat() * 120f - 60f, -400f)
                        }
                    }

                    if (serveState > 0 || serveWaitTimer <= 0) {
                        ballVel = ballVel.copy(y = ballVel.y + gravity * dt)
                        ballPos += ballVel * dt
                        ballRotation += ballVel.x * dt * 0.05f
                    }

                    if (ballPos.x < ballRadius) { ballPos = ballPos.copy(x = ballRadius); ballVel = ballVel.copy(x = -ballVel.x * 0.8f) }
                    else if (ballPos.x > width - ballRadius) { ballPos = ballPos.copy(x = width - ballRadius); ballVel = ballVel.copy(x = -ballVel.x * 0.8f) }
                    if (ballPos.y < ballRadius) { ballPos = ballPos.copy(y = ballRadius); ballVel = ballVel.copy(y = -ballVel.y * 0.8f) }

                    if (ballPos.y > floorY - ballRadius) {
                        if (ballPos.x < width / 2) enemyScore++ else playerScore++
                        soundManager.play("suelo")
                        haptics.vibrate(100)
                        spawnParticles(particles, ballPos, Color.White)
                        val winnerLeft = ballPos.x > width / 2
                        ballPos = if (winnerLeft) Offset(width * 0.25f, 200f) else Offset(width * 0.75f, 200f)
                        ballVel = Offset.Zero; ballRotation = 0f; serveWaitTimer = 2.5f; serveState = 0
                    }

                    val netRectLeft = width / 2 - netWidth / 2
                    val netRectRight = width / 2 + netWidth / 2
                    val netRectTop = floorY - netHeight
                    if (ballPos.x + ballRadius > netRectLeft && ballPos.x - ballRadius < netRectRight && ballPos.y + ballRadius > netRectTop) {
                        if (ballPos.y > netRectTop + 10f) {
                            ballVel = ballVel.copy(x = -ballVel.x * 0.5f)
                            ballPos = ballPos.copy(x = if (ballPos.x < width / 2) netRectLeft - ballRadius else netRectRight + ballRadius)
                        } else {
                            ballVel = ballVel.copy(y = -ballVel.y * 0.8f)
                            ballPos = ballPos.copy(y = netRectTop - ballRadius)
                        }
                    }

                    val targetP1VelX = p1MoveVector.x * moveSpeed
                    p1Vel = p1Vel.copy(x = p1Vel.x + (targetP1VelX - p1Vel.x) * 12f * dt)
                    if (p1MoveVector.y < -0.3f && p1Pos.y >= floorY - playerSize/2 - 5f) p1Vel = p1Vel.copy(y = jumpImpulse)
                    p1Vel = p1Vel.copy(y = p1Vel.y + gravity * dt)
                    p1Pos += p1Vel * dt
                    if (p1Pos.y > floorY - playerSize/2) { p1Pos = p1Pos.copy(y = floorY - playerSize/2); p1Vel = p1Vel.copy(y = 0f) }
                    p1Pos = p1Pos.copy(x = p1Pos.x.coerceIn(playerSize/2, width/2 - netWidth/2 - playerSize/2))
                    if (abs(p1Vel.x) > 10f && p1Pos.y >= floorY - playerSize/2 - 1f) p1WalkCycle += abs(p1Vel.x) * dt * 0.05f
                    if (p1StrikeAnim > 0f) p1StrikeAnim -= dt * 4f

                    val aiSpeed = when(difficulty.lowercase()) { "fácil" -> 450f; "medio" -> 800f; else -> 1250f }
                    var predictedX = ballPos.x
                    if (ballVel.y > 0) { val timeToLand = (floorY - ballPos.y) / (ballVel.y + 1f); predictedX = ballPos.x + ballVel.x * timeToLand }
                    predictedX = predictedX.coerceIn(width/2 + netWidth/2 + playerSize/2, width - playerSize/2)
                    val aiTargetX = if (ballPos.x > width * 0.4f) predictedX else width * 0.75f
                    val aiDiffX = aiTargetX - p2Pos.x
                    val targetP2VelX = (if (abs(aiDiffX) > 20f) aiDiffX.coerceIn(-1f, 1f) else 0f) * aiSpeed
                    p2Vel = p2Vel.copy(x = p2Vel.x + (targetP2VelX - p2Vel.x) * 10f * dt)
                    if (ballPos.x > width/2 && ballPos.y < floorY - 400f && abs(ballPos.x - p2Pos.x) < 150f && p2Pos.y >= floorY - playerSize/2 - 5f) {
                        if (Random.nextFloat() < (if (difficulty == "difícil") 0.15f else 0.05f)) p2Vel = p2Vel.copy(y = jumpImpulse)
                    }
                    p2Vel = p2Vel.copy(y = p2Vel.y + gravity * dt); p2Pos += p2Vel * dt
                    if (p2Pos.y > floorY - playerSize/2) { p2Pos = p2Pos.copy(y = floorY - playerSize/2); p2Vel = p2Vel.copy(y = 0f) }
                    p2Pos = p2Pos.copy(x = p2Pos.x.coerceIn(width/2 + netWidth/2 + playerSize/2, width - playerSize/2))
                    if (abs(p2Vel.x) > 10f && p2Pos.y >= floorY - playerSize/2 - 1f) p2WalkCycle += abs(p2Vel.x) * dt * 0.05f

                    val hitRange = ballRadius + playerSize/2 + 50f
                    fun checkCollision(pPos: Offset, isP1: Boolean, pVel: Offset) {
                        val dx = ballPos.x - pPos.x; val dy = ballPos.y - pPos.y; val dist = sqrt(dx*dx + dy*dy)
                        if (dist < hitRange) {
                            val strike = if (isP1) p1StrikeVector else Offset((width*0.2f - ballPos.x)*0.05f, (floorY*0.5f - ballPos.y)*0.05f)
                            val force = strike.getDistance(); val isS = force > 20f
                            if (isP1 && isS) { p1StrikeAnim = 1.0f; p1StrikeDir = strike; strikeForceIndicator = (force / 500f).coerceIn(0f, 1f) }
                            val vImp = if (isS) -1000f else -600f; val fFact = if (isS) 1.0f else 0.5f
                            ballVel = Offset((dx * 10f + strike.x * 6f + pVel.x * 0.5f) * fFact, (vImp + strike.y * 6f + pVel.y * 0.3f) * fFact)
                            soundManager.play(if (isS) "golpeo" else "golpe")
                            spawnParticles(particles, ballPos, if (isP1) Color(0xFFEF476F) else Color(0xFF2EC4B6))
                            ballPos += Offset(dx/dist, dy/dist) * (hitRange - dist)
                        }
                    }
                    checkCollision(p1Pos, true, p1Vel); checkCollision(p2Pos, false, p2Vel)

                    particles.forEach { it.update(dt) }; particles.removeAll { it.life <= 0 }
                    clouds.forEach { it.update(dt, width) }
                    if (strikeForceIndicator > 0f) strikeForceIndicator -= dt * 0.5f
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            clouds.forEach { drawCircle(Color.White.copy(0.6f), 60f, Offset(it.x, it.y)) }
            drawPalm(Offset(width * 0.08f, floorY), 220f)
            drawPalm(Offset(width * 0.92f, floorY), 190f)
            drawRect(Color(0xFFDEB887), Offset(0f, floorY), androidx.compose.ui.geometry.Size(width, height - floorY))
            drawRect(Color(0xFFC19A6B), Offset(width * 0.1f, floorY), androidx.compose.ui.geometry.Size(width * 0.8f, height - floorY))
            drawLine(Color.White, Offset(0f, floorY + 5f), Offset(width, floorY + 5f), 5f)
            drawLine(Color.White, Offset(width*0.1f, floorY), Offset(width*0.1f, height), 5f)
            drawLine(Color.White, Offset(width*0.9f, floorY), Offset(width*0.9f, height), 5f)
            drawRect(Color.White, Offset(width/2 - netWidth/2, floorY - netHeight), androidx.compose.ui.geometry.Size(netWidth, netHeight))
            if (strikeForceIndicator > 0.01f) {
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Yellow, Color.Red)), Offset(width/2 - netWidth/2, floorY - netHeight * strikeForceIndicator), androidx.compose.ui.geometry.Size(netWidth, netHeight * strikeForceIndicator))
            }
            if (strikeTrail.isNotEmpty()) {
                val path = Path(); strikeTrail.forEachIndexed { i, o -> if (i==0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
                drawPath(path, Color(0x66FFD166), style = Stroke(12f, cap = StrokeCap.Round))
            }
            drawAlebrijeBall(ballPos, ballRadius, ballRotation)
            drawAlebrije(p1Pos, playerSize, Color(0xFFEF476F), p1WalkCycle, p1Vel.x > 0, p1StrikeAnim, p1StrikeDir)
            drawAlebrije(p2Pos, playerSize, Color(0xFF2EC4B6), p2WalkCycle, p2Vel.x > 0, 0f, Offset.Zero)
            particles.forEach { drawCircle(it.color.copy(it.life), it.size, it.pos) }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp).background(Color(0xFFE5D5B0), RoundedCornerShape(14.dp)).padding(3.dp).background(Color(0xFF282828), RoundedCornerShape(11.dp)).padding(horizontal = 28.dp, vertical = 10.dp)) {
                Text("${playerScore} - ${enemyScore}", fontSize = 36.sp, color = Color(0xFFFFD166), fontWeight = FontWeight.Black)
            }
            CuteButton(text = "⚙", onClick = { showSettings = !showSettings }, modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(60.dp, 42.dp), padding = PaddingValues(horizontal = 5.dp, vertical = 2.dp))
        }

        if (showSettings) {
            Column(modifier = Modifier.padding(20.dp).align(Alignment.Center).clip(RoundedCornerShape(16.dp)).background(Color(0xDD1F2041)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CuteButton("Rendirme") { enemyScore = 15 }; CuteButton("Cerrar") { showSettings = false }
            }
        }

        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { if (it.x > size.width/2) strikeTrail.clear() },
                onDrag = { change, drag -> 
                    change.consume()
                    if (change.position.x < size.width/2) {
                        p1MoveVector = Offset((drag.x * 0.15f).coerceIn(-1.1f, 1.1f), (drag.y * 0.15f).coerceIn(-1.1f, 1.1f))
                    } else {
                        p1StrikeVector += drag; if (strikeTrail.size > 15) strikeTrail.removeAt(0); strikeTrail.add(change.position)
                    }
                },
                onDragEnd = { p1MoveVector = Offset.Zero; p1StrikeVector = Offset.Zero; strikeTrail.clear() }
            )
        })
    }
}

private class Cloud(var x: Float, var y: Float, val speed: Float) {
    fun update(dt: Float, width: Float) { x += speed * dt; if (x > width + 120f) x = -120f }
}

private fun DrawScope.drawPalm(pos: Offset, h: Float) {
    drawLine(Color(0xFF8B4513), pos, pos.copy(y = pos.y - h), 16f, StrokeCap.Round)
    for (i in 0..3) {
        val rad = Math.toRadians((i * 45 - 70).toDouble())
        drawLine(Color(0xFF228B22), pos.copy(y = pos.y - h), Offset(pos.x + cos(rad).toFloat() * 90f, pos.y - h + sin(rad).toFloat() * 70f), 10f, StrokeCap.Round)
    }
}

private fun DrawScope.drawAlebrije(pos: Offset, size: Float, color: Color, walk: Float, faceR: Boolean, sAnim: Float, sDir: Offset) {
    withTransform({ translate(pos.x, pos.y); scale(if (faceR) 1f else -1f, 1f, Offset.Zero) }) {
        val shake = if (sAnim > 0) sin(sAnim * 25f) * 6f else 0f
        translate(shake, 0f) {
            drawCircle(color, size * 0.4f, Offset.Zero)
            drawCircle(color, size * 0.25f, Offset(0f, -size * 0.52f))
            drawCircle(Color.White, size * 0.05f, Offset(size * 0.1f, -size * 0.55f))
            drawCircle(Color.Black, size * 0.02f, Offset(size * 0.12f, -size * 0.55f))
        }
        val legS = sin(walk) * size * 0.22f
        drawLine(color, Offset(-size * 0.1f, size * 0.3f), Offset(-size * 0.1f + legS, size * 0.52f), size * 0.12f, StrokeCap.Round)
        drawLine(color, Offset(size * 0.1f, size * 0.3f), Offset(size * 0.1f - legS, size * 0.52f), size * 0.12f, StrokeCap.Round)
        val hOff = if (sAnim > 0) Offset(sDir.x.coerceIn(-60f, 60f), sDir.y.coerceIn(-60f, 60f)) * sAnim else Offset.Zero
        drawLine(color, Offset(size * 0.32f, 0f), Offset(size * 0.55f + hOff.x, -size * 0.25f + hOff.y), size * 0.1f, StrokeCap.Round)
    }
}

private fun DrawScope.drawAlebrijeBall(pos: Offset, radius: Float, rot: Float) {
    drawCircle(Color(0xFF3AB7BF), radius, pos)
    drawCircle(Color(0xFFFFD166), radius * 0.84f, pos)
    val bPath = Path().apply { addOval(androidx.compose.ui.geometry.Rect(pos.x - radius, pos.y - radius, pos.x + radius, pos.y + radius)) }
    withTransform({ clipPath(bPath) }) {
        val path = Path(); val wW = radius * 0.65f; val wA = radius * 0.18f; val off = (rot * radius * 2.2f) % (wW * 2f)
        for (i in -4..4) {
            val sX = pos.x - radius + i * wW + off
            path.moveTo(sX, pos.y)
            path.quadraticTo(sX + wW * 0.25f, pos.y - wA, sX + wW * 0.5f, pos.y)
            path.quadraticTo(sX + wW * 0.75f, pos.y + wA, sX + wW, pos.y)
        }
        drawPath(path, Color(0xFFEF476F), style = Stroke(radius * 0.18f, cap = StrokeCap.Round))
    }
    val sX = pos.x - radius * 0.3f + (sin(rot) * radius * 0.12f); val sY = pos.y - radius * 0.3f + (cos(rot) * radius * 0.08f)
    drawCircle(Color.White, radius * 0.13f, Offset(sX, sY))
}

@Composable
private fun CuteButton(text: String, modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(0.dp), onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF476F), contentColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = modifier.height(56.dp).width(180.dp), contentPadding = padding) { Text(text, fontWeight = FontWeight.Bold) }
}

private class SoundManager(context: Context) {
    private val soundPool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).build()
    private val sounds = mutableMapOf<String, Int>()
    init {
        listOf("suelo", "red", "golpe", "toque", "barrida", "golpeo", "win", "loos", "winner", "looser", "bg-music").forEach { name ->
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId != 0) sounds[name] = soundPool.load(context, resId, 1)
        }
    }
    fun play(name: String) { sounds[name]?.let { soundPool.play(it, 1f, 1f, 0, 0, 1f) } }
}

private class HapticManager(context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    fun vibrate(duration: Long) { if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)) }
}

private class Particle(var pos: Offset, val color: Color) {
    var vel = Offset(Random.nextFloat() * 200 - 100, Random.nextFloat() * 200 - 100); var life = 1.0f; var size = Random.nextFloat() * 10 + 5
    fun update(dt: Float) { pos += vel * dt; life -= dt * 2f }
}

private fun spawnParticles(list: MutableList<Particle>, pos: Offset, color: Color) { repeat(10) { list.add(Particle(pos, color)) } }

@Composable
private fun ResultScreen(playerScore: Int, enemyScore: Int, onHome: () -> Unit) {
    val victory = playerScore > enemyScore
    ScreenContainer {
        Text(if (victory) "¡Ganaste!" else "Derrota", fontSize = 44.sp, color = if (victory) Color(0xFFFFD166) else Color(0xFFFF595E), fontWeight = FontWeight.ExtraBold)
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(Color(0xFF2EC4B6)))
        Text(name, color = Color.White)
    }
}
