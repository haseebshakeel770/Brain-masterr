package com.mathmaster

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathmaster.ui.theme.MathMasterTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MathMasterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MathMasterApp()
                }
            }
        }
    }
}

// ------------------------------------------------------------- Navigation
sealed class Screen {
    object Home : Screen()
    data class Game(val difficulty: Difficulty) : Screen()
    data class Result(
        val score: Int,
        val total: Int,
        val difficulty: Difficulty,
        val bestStreak: Int,
        val previousHighScore: Int
    ) : Screen()
}

// ------------------------------------------------------------- High scores
private object HighScoreStore {
    private const val PREFS = "math_master_scores"
    fun get(context: Context, difficulty: Difficulty): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("high_${difficulty.name}", 0)
    fun set(context: Context, difficulty: Difficulty, score: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt("high_${difficulty.name}", score).apply()
    }
}

@Composable
fun MathMasterApp() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var highScores by remember {
        mutableStateOf(
            mapOf(
                Difficulty.EASY to HighScoreStore.get(context, Difficulty.EASY),
                Difficulty.MEDIUM to HighScoreStore.get(context, Difficulty.MEDIUM),
                Difficulty.HARD to HighScoreStore.get(context, Difficulty.HARD)
            )
        )
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(320))) togetherWith
            (slideOutHorizontally(tween(320)) { -it / 4 } + fadeOut(tween(320)))
        },
        label = "screen"
    ) { current ->
        when (current) {
            is Screen.Home -> HomeScreen(
                highScores = highScores,
                onStart = { diff -> screen = Screen.Game(diff) }
            )
            is Screen.Game -> GameScreen(
                difficulty = current.difficulty,
                onFinish = { score, bestStreak ->
                    val oldHigh = highScores[current.difficulty] ?: 0
                    if (score > oldHigh) {
                        HighScoreStore.set(context, current.difficulty, score)
                        highScores = highScores + (current.difficulty to score)
                    }
                    screen = Screen.Result(score, 10, current.difficulty, bestStreak, oldHigh)
                },
                onHome = { screen = Screen.Home }
            )
            is Screen.Result -> ResultScreen(
                score = current.score,
                total = current.total,
                bestStreak = current.bestStreak,
                previousHighScore = current.previousHighScore,
                currentHighScore = highScores[current.difficulty] ?: 0,
                onPlayAgain = { screen = Screen.Game(current.difficulty) },
                onHome = { screen = Screen.Home }
            )
        }
    }
}

// ------------------------------------------------------------- HOME
@Composable
fun HomeScreen(highScores: Map<Difficulty, Int>, onStart: (Difficulty) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF6A3EA1), Color(0xFF8E4EC6), Color(0xFFB085D6))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            Text(text = "🧮", fontSize = 64.sp)
            Text(
                text = "Math Master",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Challenge yourself. Beat your best!",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(36.dp))
            Text(
                text = "CHOOSE DIFFICULTY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.75f),
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))

            DifficultyCard(
                icon = "🌱",
                title = "Easy",
                subtitle = "Numbers up to 20",
                highScore = highScores[Difficulty.EASY] ?: 0,
                gradient = listOf(Color(0xFF43CEA2), Color(0xFF185A9D))
            ) { onStart(Difficulty.EASY) }

            Spacer(Modifier.height(14.dp))

            DifficultyCard(
                icon = "⚡",
                title = "Medium",
                subtitle = "Numbers up to 100",
                highScore = highScores[Difficulty.MEDIUM] ?: 0,
                gradient = listOf(Color(0xFFFFB75E), Color(0xFFED8F03))
            ) { onStart(Difficulty.MEDIUM) }

            Spacer(Modifier.height(14.dp))

            DifficultyCard(
                icon = "🔥",
                title = "Hard",
                subtitle = "Numbers up to 200",
                highScore = highScores[Difficulty.HARD] ?: 0,
                gradient = listOf(Color(0xFFFF416C), Color(0xFFB80044))
            ) { onStart(Difficulty.HARD) }

            Spacer(Modifier.weight(1f))
            Text(
                text = "10 questions  •  20s each  •  100% offline",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun DifficultyCard(
    icon: String,
    title: String,
    subtitle: String,
    highScore: Int,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradient))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 34.sp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "🏆", fontSize = 20.sp)
                    Text(
                        text = "$highScore",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------- GAME
@Composable
fun GameScreen(
    difficulty: Difficulty,
    onFinish: (score: Int, bestStreak: Int) -> Unit,
    onHome: () -> Unit
) {
    val totalQuestions = 10
    val totalTime = 20

    var qNum by remember { mutableIntStateOf(1) }
    var question by remember { mutableStateOf(generateQuestion(difficulty)) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var selected by remember(qNum) { mutableStateOf<Int?>(null) }
    var answered by remember(qNum) { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(totalTime) }
    var wrongShakeTrigger by remember { mutableIntStateOf(0) }

    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }

    // Countdown timer
    LaunchedEffect(qNum) {
        timeLeft = totalTime
        while (timeLeft > 0 && !answered) {
            delay(1000)
            if (!answered) timeLeft--
        }
        if (timeLeft == 0 && !answered) {
            answered = true
            streak = 0
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Advance to next question after answered
    LaunchedEffect(answered, qNum) {
        if (answered) {
            delay(1200)
            if (qNum >= totalQuestions) {
                onFinish(score, bestStreak)
            } else {
                qNum++
                question = generateQuestion(difficulty)
            }
        }
    }

    // Shake animation on wrong answer
    LaunchedEffect(wrongShakeTrigger) {
        if (wrongShakeTrigger > 0) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(18f, tween(50))
            shakeOffset.animateTo(-18f, tween(50))
            shakeOffset.animateTo(12f, tween(50))
            shakeOffset.animateTo(-12f, tween(50))
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    val progress by animateFloatAsState(
        targetValue = qNum.toFloat() / totalQuestions,
        animationSpec = tween(400),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF6A3EA1), Color(0xFF8E4EC6))
                )
            )
            .safeDrawingPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onHome) {
                Text("🏠 Home", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            CircularTimer(timeLeft = timeLeft, totalTime = totalTime, size = 58.dp)
        }

        Spacer(Modifier.height(6.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF43CEA2), Color(0xFF4CAF50))
                        )
                    )
            )
        }

        Spacer(Modifier.height(14.dp))

        // Score + streak row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InfoChip(label = "Q $qNum/$totalQuestions")
            InfoChip(label = "⭐ $score")
            if (streak > 1) InfoChip(label = "🔥 $streak")
        }

        Spacer(Modifier.height(18.dp))

        // Question card with shake
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer { translationX = shakeOffset.value },
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Solve",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6A3EA1).copy(alpha = 0.6f),
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "${question.questionText} = ?",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF2A1A4A)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Answer buttons 2x2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowOptions.forEach { option ->
                        AnswerButton(
                            option = option,
                            correctAnswer = question.correctAnswer,
                            selected = selected,
                            answered = answered,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!answered) {
                                    answered = true
                                    selected = option
                                    if (option == question.correctAnswer) {
                                        score++
                                        streak++
                                        if (streak > bestStreak) bestStreak = streak
                                        haptic.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                    } else {
                                        streak = 0
                                        wrongShakeTrigger++
                                        haptic.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun InfoChip(label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AnswerButton(
    option: Int,
    correctAnswer: Int,
    selected: Int?,
    answered: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = when {
            !answered -> Color.White
            option == correctAnswer -> Color(0xFF4CAF50)
            option == selected -> Color(0xFFE53935)
            else -> Color(0xFFE8E2F1)
        },
        animationSpec = tween(280),
        label = "answerBg"
    )
    val fg by animateColorAsState(
        targetValue = when {
            !answered -> Color(0xFF2A1A4A)
            option == correctAnswer || option == selected -> Color.White
            else -> Color(0xFF8A7FA0)
        },
        animationSpec = tween(280),
        label = "answerFg"
    )

    Button(
        onClick = onClick,
        enabled = !answered,
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = fg,
            disabledContainerColor = bg,
            disabledContentColor = fg
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            disabledElevation = 2.dp
        )
    ) {
        Text(text = option.toString(), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun CircularTimer(timeLeft: Int, totalTime: Int, size: Dp) {
    val progress = timeLeft.toFloat() / totalTime
    val color = when {
        progress <= 0.25f -> Color(0xFFFF5252)
        progress <= 0.5f -> Color(0xFFFFB74D)
        else -> Color(0xFF69F0AE)
    }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val stroke = 5.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$timeLeft",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ------------------------------------------------------------- RESULT
@Composable
fun ResultScreen(
    score: Int,
    total: Int,
    bestStreak: Int,
    previousHighScore: Int,
    currentHighScore: Int,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val percent = if (total > 0) (score * 100) / total else 0
    val stars = when {
        percent >= 90 -> 3
        percent >= 70 -> 2
        percent >= 50 -> 1
        else -> 0
    }
    val isNewRecord = score > previousHighScore && score > 0
    val message = when {
        percent >= 90 -> "Outstanding! 🏆"
        percent >= 70 -> "Great job! 🎉"
        percent >= 50 -> "Good effort! 👍"
        else -> "Keep practicing! 💪"
    }
    val bgColors = if (percent >= 50)
        listOf(Color(0xFF43CEA2), Color(0xFF185A9D))
    else
        listOf(Color(0xFFFF8A65), Color(0xFFD84315))

    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(1200),
        label = "scoreAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = message, fontSize = 26.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            // Stars
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Text(
                        text = if (i < stars) "⭐" else "☆",
                        fontSize = 42.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "$animatedScore / $total",
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "correct answers",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "🏆 Best streak: $bestStreak",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isNewRecord) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFFD54F),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "🎯 NEW HIGH SCORE!",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        color = Color(0xFF4A2A00),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                }
            } else if (currentHighScore > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "High score: $currentHighScore",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = bgColors.last()
                )
            ) {
                Text("🔄  Play Again", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("🏠  Home", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
