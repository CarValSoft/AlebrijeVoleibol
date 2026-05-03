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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF6EC6FF))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAlebrije(Offset(200f, 600f), 150f, Color(0xFFEF476F), 0f, true, 0f, Offset.Zero, 0f)
            drawAlebrijeBall(Offset(350f, 300f), 95f, 1.5f, 0.5f)
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
            Screen.MENU -> MainMenu(onOnline = { mode = GameMode.ONLINE; screen = Screen.MATCHMAKING }, onOffline = { mode = GameMode.OFFLINE; screen = Screen.DIFFICULTY })
            Screen.MATCHMAKING -> MatchmakingScreen(onMatched = { screen = Screen.COURT })
            Screen.DIFFICULTY -> DifficultyScreen(onSelected = { difficulty = it; screen = Screen.COURT })
            Screen.COURT -> CourtScreen(mode = mode, difficulty = difficulty, onFinish = { me, enemy -> playerScore = me; enemyScore = enemy; screen = Screen.RESULT })
            Screen.RESULT -> ResultScreen(playerScore = playerScore, enemyScore = enemyScore, onHome = { screen = Screen.MENU })
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
private fun CourtScreen(mode: GameMode, difficulty: String, onFinish: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val haptics = remember { HapticManager(context) }
    var playerScore by remember { mutableIntStateOf(0) }
    var enemyScore by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    var ballPos by remember { mutableStateOf(Offset(0f, 0f)) }
    var ballVel by remember { mutableStateOf(Offset(0f, 0f)) }
    var ballRotX by remember { mutableFloatStateOf(0f) }
    var ballRotY by remember { mutableFloatStateOf(0f) }
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
    var netVibration by remember { mutableFloatStateOf(0f) }
    var audiencePulse by remember { mutableFloatStateOf(0f) }

    if (playerScore >= 15 || enemyScore >= 15) {
        DisposableEffect(Unit) {
            if (playerScore >= 15) soundManager.play("winner") else soundManager.play("looser")
            onFinish(playerScore, enemyScore)
            onDispose { }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF6EC6FF))) {
        val width = constraints.maxWidth.toFloat(); val height = constraints.maxHeight.toFloat()
        val floorY = height * 0.9f; val netWidth = 15f; val netHeight = 150f
        val ballRadius = 95f; val playerSize = 140f

        LaunchedEffect(width) {
            repeat(6) { clouds.add(Cloud(Random.nextFloat()*width, Random.nextFloat()*height*0.25f, Random.nextFloat()*15f+10f, Random.nextFloat()*20f+40f, Random.nextInt(3))) }
            palms.add(Palm(width*0.05f, floorY, 220f, -8f, 0))
            palms.add(Palm(width*0.95f, floorY, 200f, 6f, 1))
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
                    val gravity = 2500f; val baseMoveSpeed = 1815f; val jumpImpulse = -1400f

                    audiencePulse = (audiencePulse + dt * 2f) % (Math.PI.toFloat() * 2f)

                    if (serveWaitTimer > 0) {
                        serveWaitTimer -= dt
                        if (serveWaitTimer <= 0 && serveState == 0) { serveState = 1; ballVel = Offset(Random.nextFloat()*140f-70f, -400f) }
                    }

                    if (serveState > 0 || serveWaitTimer <= 0) {
                        ballVel = ballVel.copy(y = ballVel.y + gravity * dt); ballPos += ballVel * dt
                        ballRotX += ballVel.x * dt * 0.06f; ballRotY += ballVel.y * dt * 0.03f
                    }

                    if (ballPos.x < ballRadius) { ballPos = ballPos.copy(x = ballRadius); ballVel = ballVel.copy(x = -ballVel.x * 0.8f) }
                    else if (ballPos.x > width - ballRadius) { ballPos = ballPos.copy(x = width - ballRadius); ballVel = ballVel.copy(x = -ballVel.x * 0.8f) }
                    if (ballPos.y < ballRadius) { ballPos = ballPos.copy(y = ballRadius); ballVel = ballVel.copy(y = -ballVel.y * 0.8f) }

                    if (ballPos.y > floorY - ballRadius) {
                        if (ballPos.x < width / 2) enemyScore++ else playerScore++
                        soundManager.play("suelo"); haptics.vibrate(100); spawnParticles(particles, ballPos, Color.White)
                        val winnerLeft = ballPos.x > width / 2; ballPos = if (winnerLeft) Offset(width * 0.25f, 200f) else Offset(width * 0.75f, 200f)
                        ballVel = Offset.Zero; ballRotX = 0f; ballRotY = 0f; serveWaitTimer = 2.5f; serveState = 0
                    }

                    val netRectLeft = width / 2 - netWidth / 2; val netRectRight = width / 2 + netWidth / 2; val netRectTop = floorY - netHeight
                    if (ballPos.x + ballRadius > netRectLeft && ballPos.x - ballRadius < netRectRight && ballPos.y + ballRadius > netRectTop) {
                        netVibration = 1.0f
                        if (ballPos.y > netRectTop + 10f) { ballVel = ballVel.copy(x = -ballVel.x * 0.5f); ballPos = ballPos.copy(x = if (ballPos.x < width / 2) netRectLeft - ballRadius else netRectRight + ballRadius) }
                        else { ballVel = ballVel.copy(y = -ballVel.y * 0.8f); ballPos = ballPos.copy(y = netRectTop - ballRadius) }
                    }

                    if (p1SlideTimer > 0) p1SlideTimer -= dt
                    val moveSpeed = if (p1SlideTimer > 0) baseMoveSpeed * 1.3f else baseMoveSpeed
                    val targetP1VelX = p1MoveVector.x * moveSpeed
                    p1Vel = p1Vel.copy(x = p1Vel.x + (targetP1VelX - p1Vel.x) * 12f * dt)
                    if (p1MoveVector.y < -0.3f && p1Pos.y >= floorY - playerSize/2 - 5f) p1Vel = p1Vel.copy(y = jumpImpulse)
                    p1Vel = p1Vel.copy(y = p1Vel.y + gravity * dt); p1Pos += p1Vel * dt
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

                    val hitRange = ballRadius + playerSize/2 + 100f
                    fun checkCollision(pPos: Offset, isP1: Boolean, pVel: Offset) {
                        val dx = ballPos.x - pPos.x; val dy = ballPos.y - pPos.y; val dist = sqrt(dx*dx + dy*dy)
                        if (dist < hitRange) {
                            val strike = if (isP1) p1StrikeVector else Offset((width*0.2f - ballPos.x)*0.05f, (floorY*0.5f - ballPos.y)*0.05f)
                            val force = strike.getDistance(); val isS = force > 30f
                            if (isP1 && isS) { p1StrikeAnim = 1.0f; p1StrikeDir = strike; strikeForceIndicator = (force / 500f).coerceIn(0f, 1f) }
                            val vImp = if (isS) -1000f else -600f; val fFact = if (isS) 1.0f else 0.5f
                            ballVel = Offset((dx * 12f + strike.x * 6f + pVel.x * 0.6f) * fFact, (vImp + strike.y * 6f + pVel.y * 0.4f) * fFact)
                            soundManager.play(if (isS) "golpeo" else "golpe"); spawnParticles(particles, ballPos, if (isP1) Color(0xFFEF476F) else Color(0xFF2EC4B6))
                            ballPos = pPos + Offset(dx/dist, dy/dist) * hitRange // Force ejection to avoid sticking
                        }
                    }
                    checkCollision(p1Pos, true, p1Vel); checkCollision(p2Pos, false, p2Vel)

                    particles.forEach { it.update(dt) }; particles.removeAll { it.life <= 0 }
                    clouds.forEach { it.update(dt, width) }
                    if (strikeForceIndicator > 0f) strikeForceIndicator -= dt * 0.5f
                    if (netVibration > 0f) netVibration -= dt * 5f
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            clouds.forEach { drawCloud(Offset(it.x, it.y), it.size, it.variant) }
            palms.forEach { drawPalm(Offset(it.x, it.y), it.height, it.tilt, it.variant) }
            
            // Audience Bleachers
            drawBleachers(width, height, floorY, audiencePulse)

            drawRect(Color(0xFFDEB887), Offset(0f, floorY), androidx.compose.ui.geometry.Size(width, height - floorY)) // Darker original
            drawRect(Color(0xFFDEB887).copy(alpha = 0.6f), Offset(width * 0.1f, floorY), androidx.compose.ui.geometry.Size(width * 0.8f, height - floorY)) // Lighter original
            drawLine(Color.White, Offset(0f, floorY + 5f), Offset(width, floorY + 5f), 5f)
            drawLine(Color.White, Offset(width*0.1f, floorY), Offset(width*0.1f, height), 5f)
            drawLine(Color.White, Offset(width*0.9f, floorY), Offset(width*0.9f, height), 5f)
            val vibX = sin(netVibration * 30f) * 10f * netVibration
            drawRect(Color.White, Offset(width/2 - netWidth/2 + vibX, floorY - netHeight), androidx.compose.ui.geometry.Size(netWidth, netHeight))
            if (strikeForceIndicator > 0.01f) {
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Yellow, Color.Red)), Offset(width/2 - netWidth/2 + vibX, floorY - netHeight * strikeForceIndicator), androidx.compose.ui.geometry.Size(netWidth, netHeight * strikeForceIndicator))
            }
            if (strikeTrail.isNotEmpty()) {
                val path = Path(); strikeTrail.forEachIndexed { i, o -> if (i==0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
                drawPath(path, Color(0x66FFD166), style = Stroke(12f, cap = StrokeCap.Round))
            }
            drawAlebrijeBall(ballPos, ballRadius, ballRotX, ballRotY)
            drawAlebrije(p1Pos, playerSize, Color(0xFFEF476F), p1WalkCycle, p1Vel.x > 0, p1StrikeAnim, p1StrikeDir, p1SlideTimer)
            drawAlebrije(p2Pos, playerSize, Color(0xFF2EC4B6), p2WalkCycle, p2Vel.x > 0, 0f, Offset.Zero, 0f)
            particles.forEach { drawCircle(it.color.copy(it.life), it.size, it.pos) }
        }

        Box(modifier = Modifier.fillMaxSize().zIndex(10f)) {
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp).background(Color(0xFFE5D5B0), RoundedCornerShape(14.dp)).padding(3.dp).background(Color(0xFF282828), RoundedCornerShape(11.dp)).padding(horizontal = 28.dp, vertical = 10.dp)) {
                Text("${playerScore} - ${enemyScore}", fontSize = 36.sp, color = Color(0xFFFFD166), fontWeight = FontWeight.Black)
            }
            CuteButton(text = "⚙", onClick = { showSettings = !showSettings }, modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(60.dp, 42.dp).zIndex(999f), padding = PaddingValues(horizontal = 5.dp, vertical = 2.dp))
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

private class Cloud(var x: Float, var y: Float, val speed: Float, val size: Float, val variant: Int) {
    fun update(dt: Float, width: Float) { x += speed * dt; if (x > width + size * 3) x = -size * 3 }
}
private class Palm(val x: Float, val y: Float, val height: Float, val tilt: Float, val variant: Int)

private fun DrawScope.drawPalm(pos: Offset, h: Float, tilt: Float, variant: Int) {
    val leafCount = 5 + variant
    withTransform({ translate(pos.x, pos.y); rotate(tilt, Offset.Zero) }) {
        drawLine(Color(0xFF5D2E0A), Offset.Zero, Offset(0f, -h), 22f, StrokeCap.Round)
        for (i in 0 until leafCount) {
            val rad = Math.toRadians((i * (180f/leafCount) - 135f + (variant * 15f)).toDouble())
            drawLine(if (variant == 0) Color(0xFF228B22) else Color(0xFF1B6A1B), Offset(0f, -h), Offset(cos(rad).toFloat()*(90f+i*10f), -h+sin(rad).toFloat()*(70f+i*5f)), 14f, StrokeCap.Round)
        }
    }
}


private fun DrawScope.drawCloud(pos: Offset, size: Float, variant: Int) {
    val color = Color.White.copy(0.75f)
    drawCircle(color, size, pos); drawCircle(color, size * 0.85f, pos + Offset(-size * 0.75f, size * 0.25f))
    drawCircle(color, size * 0.85f, pos + Offset(size * 0.75f, size * 0.25f)); if (variant > 0) drawCircle(color, size * 0.65f, pos + Offset(0f, -size * 0.45f))
}

private fun DrawScope.drawBleachers(w: Float, h: Float, floorY: Float, pulse: Float) {
    val bW = w * 0.5f; val bH = 120f; val bX = w / 2 - bW / 2; val bY = floorY - bH
    drawRect(Color(0xFF5A5A5A), Offset(bX, bY), androidx.compose.ui.geometry.Size(bW, bH))
    for (row in 0..2) {
        val rowY = bY + row * 40f
        for (i in 0..10) {
            val audienceX = bX + 20f + i * (bW - 40f) / 10f
            val jump = sin(pulse + i + row) * 10f
            drawCircle(Color(Random(i).nextInt()), 12f, Offset(audienceX, rowY + jump))
        }
    }
}

private fun DrawScope.drawAlebrije(pos: Offset, size: Float, color: Color, walk: Float, faceR: Boolean, sAnim: Float, sDir: Offset, slide: Float) {
    withTransform({ translate(pos.x, pos.y); scale(if (faceR) 1f else -1f, 1f, Offset.Zero) }) {
        val sF = sDir.getDistance().coerceIn(0f, 120f); val bT = if (sAnim > 0) (sDir.x / 8f) * sAnim else if (slide > 0) 30f else 0f
        withTransform({ rotate(bT, Offset.Zero) }) {
            val shake = if (sAnim > 0) sin(sAnim * 35f) * 10f else 0f
            translate(shake, 0f) { drawCircle(color, size * 0.43f, Offset.Zero); drawCircle(color, size * 0.27f, Offset(0f, -size * 0.55f)); drawCircle(Color.White, size * 0.05f, Offset(size * 0.1f, -size * 0.58f)); drawCircle(Color.Black, size * 0.02f, Offset(size * 0.12f, -size * 0.58f)) }
        }
        val legS = if (sAnim > 0) sin(sAnim * 25f) * size * 0.2f else if (slide > 0) size * 0.4f else sin(walk) * size * 0.25f
        val legY = if (slide > 0) -size * 0.2f else if (sAnim > 0.5f) -size * 0.1f * sAnim else 0f
        drawLine(color, Offset(-size * 0.12f, size * 0.3f), Offset(-size * 0.12f + legS, size * 0.55f + legY), size * 0.15f, StrokeCap.Round)
        drawLine(color, Offset(size * 0.12f, size * 0.3f), Offset(size * 0.12f - legS, size * 0.55f + legY), size * 0.15f, StrokeCap.Round)
        
        // 1st Hand
        val h1Off = if (sAnim > 0) Offset(sDir.x.coerceIn(-120f, 120f), sDir.y.coerceIn(-120f, 120f)) * sAnim else Offset.Zero
        val aB1 = Offset(size * 0.35f, -size * 0.12f); val aE1 = Offset(size * 0.65f + h1Off.x, -size * 0.35f + h1Off.y)
        val elb1 = (aB1 + aE1) / 2f + Offset(0f, -sF * 0.25f * sAnim)
        drawLine(color, aB1, elb1, size * 0.14f, StrokeCap.Round); drawLine(color, elb1, aE1, size * 0.14f, StrokeCap.Round); drawCircle(color, size * 0.09f, aE1)
        
        // 2nd Hand (Default down, reacts to movement/slide)
        val h2Swing = if (slide > 0) size * 0.3f else sin(walk + 1f) * size * 0.1f
        val aB2 = Offset(-size * 0.35f, -size * 0.12f); val aE2 = Offset(-size * 0.45f - h2Swing, size * 0.3f)
        drawLine(color, aB2, aE2, size * 0.12f, StrokeCap.Round); drawCircle(color, size * 0.08f, aE2)
    }
}

private fun DrawScope.drawAlebrijeBall(pos: Offset, radius: Float, rotX: Float, rotY: Float) {
    drawCircle(Color(0xFF3AB7BF), radius, pos); drawCircle(Color(0xFFFFD166), radius * 0.84f, pos)
    val bPath = Path().apply { addOval(androidx.compose.ui.geometry.Rect(pos.x - radius, pos.y - radius, pos.x + radius, pos.y + radius)) }
    withTransform({ clipPath(bPath) }) {
        val path = Path(); val wW = radius * 0.65f; val wA = radius * 0.18f
        val offX = (rotX * radius * 2.2f) % (wW * 2f); val offY = (rotY * radius * 1.5f)
        for (i in -4..4) { val sX = pos.x - radius + i * wW + offX; path.moveTo(sX, pos.y + offY); path.quadraticTo(sX+wW*0.25f, pos.y-wA+offY, sX+wW*0.5f, pos.y+offY); path.quadraticTo(sX+wW*0.75f, pos.y+wA+offY, sX+wW, pos.y+offY) }
        drawPath(path, Color(0xFFEF476F), style = Stroke(radius * 0.18f, cap = StrokeCap.Round))
    }
    drawCircle(Color.White, radius * 0.13f, Offset(pos.x - radius * 0.3f + (sin(rotX)*radius*0.12f), pos.y - radius * 0.3f + (cos(rotY)*radius*0.08f)))
}

@Composable
private fun CuteButton(text: String, modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(0.dp), onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF476F), contentColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = modifier.height(56.dp).width(180.dp), contentPadding = padding) { Text(text, fontWeight = FontWeight.Bold) }
}

private class SoundManager(context: Context) {
    private val soundPool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).build()
    private val sounds = mutableMapOf<String, Int>()
    init { listOf("suelo", "red", "golpe", "toque", "barrida", "golpeo", "win", "loos", "winner", "looser", "bg-music").forEach { name -> val resId = context.resources.getIdentifier(name, "raw", context.packageName); if (resId != 0) sounds[name] = soundPool.load(context, resId, 1) } }
    fun play(name: String) { sounds[name]?.let { soundPool.play(it, 1f, 1f, 0, 0, 1f) } }
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
