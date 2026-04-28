package com.alebrije.voleibol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

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
    var playerScore by remember { mutableIntStateOf(0) }
    var enemyScore by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var invertControls by remember { mutableStateOf(false) }
    var movementVector by remember { mutableStateOf(Offset.Zero) }
    var strikeVector by remember { mutableStateOf(Offset.Zero) }
    var tutorialHintTime by remember { mutableFloatStateOf(2f) }

    if (playerScore >= 15 || enemyScore >= 15) {
        onFinish(playerScore, enemyScore)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF6EC6FF))) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CuteButton(text = "⚙") { showSettings = !showSettings }
                Text(
                    text = "${playerScore} - ${enemyScore}",
                    fontSize = 32.sp,
                    color = Color(0xFFFF595E),
                    fontWeight = FontWeight.ExtraBold
                )
                Text(text = mode.name, color = Color.White)
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xFF3CB371))) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width / 2f, size.height * 0.12f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )
                }

                Text(
                    text = "Modo ${mode.name.lowercase()} / IA: $difficulty",
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                    color = Color.White
                )
            }
        }

        if (showSettings) {
            Column(
                modifier = Modifier.padding(20.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xDD1F2041)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CuteButton(text = "Rendirme") { enemyScore = 15 }
                CuteButton(text = "Invertir controles") { invertControls = !invertControls }
                CuteButton(text = "+1 punto") { playerScore += 1 }
                CuteButton(text = "+1 rival") { enemyScore += 1 }
            }
        }

        val movementAlignment = if (invertControls) Alignment.CenterEnd else Alignment.CenterStart
        val strikeAlignment = if (invertControls) Alignment.CenterStart else Alignment.CenterEnd

        TouchController(
            modifier = Modifier.align(movementAlignment).padding(24.dp),
            label = "Movimiento",
            onDrag = { movementVector = it }
        )
        TouchController(
            modifier = Modifier.align(strikeAlignment).padding(24.dp),
            label = "Golpeo",
            onDrag = { strikeVector = it }
        )

        if (tutorialHintTime > 0f) {
            val density = LocalDensity.current
            val xOffset = with(density) { 120.dp.toPx() }
            Text(
                text = "👟",
                fontSize = 40.sp,
                modifier = Modifier.align(movementAlignment).offset { IntOffset((if (invertControls) -xOffset else xOffset).roundToInt(), 0) }
            )
            Text(
                text = "👊",
                fontSize = 40.sp,
                modifier = Modifier.align(strikeAlignment).offset { IntOffset((if (invertControls) xOffset else -xOffset).roundToInt(), 0) }
            )
            tutorialHintTime -= 0.016f
        }

        Text(
            text = "MOV(${movementVector.x.roundToInt()},${movementVector.y.roundToInt()})  HIT(${strikeVector.x.roundToInt()},${strikeVector.y.roundToInt()})",
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
        )
    }
}

@Composable
private fun TouchController(
    modifier: Modifier,
    label: String,
    onDrag: (Offset) -> Unit
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(Color(0x55FFFFFF))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { _, amount ->
                            dragOffset += amount
                            onDrag(dragOffset)
                        },
                        onDragEnd = {
                            dragOffset = Offset.Zero
                            onDrag(Offset.Zero)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0x66FFD166)))
        }
        Text(label, color = Color.White)
    }
}

@Composable
private fun ResultScreen(playerScore: Int, enemyScore: Int, onHome: () -> Unit) {
    val victory = playerScore > enemyScore
    ScreenContainer {
        Text(
            if (victory) "¡Ganaste!" else "Derrota",
            fontSize = 44.sp,
            color = if (victory) Color(0xFFFFD166) else Color(0xFFFF595E),
            fontWeight = FontWeight.ExtraBold
        )
        Text("Marcador: $playerScore - $enemyScore", color = Color.White, fontSize = 26.sp)
        CuteButton(text = "Volver al menú", onClick = onHome)
    }
}

@Composable
private fun ScreenContainer(content: @Composable Column.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1F2041)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        content = content
    )
}

@Composable
private fun CuteButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF476F), contentColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.height(56.dp).width(180.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Avatar(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(Color(0xFF2EC4B6)))
        Text(name, color = Color.White)
    }
}
