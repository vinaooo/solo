package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.vinaooo.solo.core.ui.formatElapsed
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.feature.game.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The win dialog: confetti, streamers and balloons fill the screen while the card springs in, and its trophy pops
 * out and keeps swinging.
 */
@Composable
internal fun WinDialog(record: ScoreRecord, onNewGame: () -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val card = remember { Animatable(0f) }
        LaunchedEffect(Unit) { card.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Celebration(Modifier.fillMaxSize())
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(32.dp).widthIn(min = 280.dp, max = 360.dp).graphicsLayer {
                    scaleX = 0.6f + 0.4f * card.value
                    scaleY = scaleX
                    alpha = card.value.coerceIn(0f, 1f)
                },
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Trophy()
                    Text(
                        stringResource(R.string.you_won),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(R.string.win_score, record.points))
                    Text(stringResource(R.string.win_time, formatElapsed(record.elapsedSeconds)))
                    Text(stringResource(R.string.win_moves, record.moves))
                    Button(onClick = onNewGame, modifier = Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.new_game))
                    }
                }
            }
        }
    }
}

/** A trophy that pops in just after the card, then swings gently from side to side. */
@Composable
private fun Trophy() {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
    }
    val swing by rememberInfiniteTransition(label = "trophy").animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "swing",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                rotationZ = swing
            }
            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
    ) {
        Icon(
            Icons.Rounded.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(56.dp),
        )
    }
}

/** Something that crosses the screen in a loop: where across it ([x], 0..1), how far into its trip ([phase]), etc. */
private class Piece(random: Random, val color: Color) {
    val x = random.nextFloat()
    val phase = random.nextFloat()
    val speed = 0.12f + random.nextFloat() * 0.18f
    val sway = random.nextFloat() * 2 * PI.toFloat()
    val spin = 2f + random.nextFloat() * 4f
    val size = 0.6f + random.nextFloat() * 0.8f
}

/** Confetti and streamers falling, balloons rising, forever: drawn every frame from the elapsed time alone. */
@Composable
private fun Celebration(modifier: Modifier) {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFFFFC107),
        Color(0xFFE91E63),
        Color(0xFF03A9F4),
        Color(0xFF8BC34A),
        Color(0xFFFF5722),
        Color(0xFF9C27B0),
    )
    val pieces = remember(palette) {
        val random = Random(seed = 7)
        Triple(
            List(CONFETTI) { Piece(random, palette[it % palette.size]) },
            List(STREAMERS) { Piece(random, palette[(it + 3) % palette.size]) },
            List(BALLOONS) { Piece(random, palette[(it + 5) % palette.size]) },
        )
    }
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        // The infinite-animation clock lets UI tests settle instead of waiting for this loop to end.
        val start = withInfiniteAnimationFrameNanos { it }
        while (true) withInfiniteAnimationFrameNanos { seconds = (it - start) / NANOS_PER_SECOND }
    }
    Canvas(modifier) {
        val t = seconds
        pieces.third.forEach { drawBalloon(it, t) }
        pieces.second.forEach { drawStreamer(it, t) }
        pieces.first.forEach { drawConfetti(it, t) }
    }
}

/** How far down the screen a falling piece is at [t], looping from just above the top to just below the bottom. */
private fun DrawScope.fallingY(piece: Piece, t: Float, speedFactor: Float = 1f): Float =
    ((piece.phase + piece.speed * speedFactor * t) % FALL_LOOP - FALL_LEAD) * size.height

private fun DrawScope.drawConfetti(piece: Piece, t: Float) {
    val x = piece.x * size.width + sin(t * CONFETTI_SWAY_RATE + piece.sway) * CONFETTI_SWAY.toPx()
    val y = fallingY(piece, t)
    val w = CONFETTI_SIZE.width.toPx() * piece.size
    val h = CONFETTI_SIZE.height.toPx() * piece.size
    // Flipping in 3D, faked by squeezing its width.
    val flip = abs(cos(t * piece.spin + piece.sway))
    rotate(degrees = t * piece.spin * CONFETTI_TURN_DEGREES, pivot = Offset(x, y)) {
        drawRect(piece.color, topLeft = Offset(x - w * flip / 2, y - h / 2), size = Size(w * flip, h))
    }
}

private fun DrawScope.drawStreamer(piece: Piece, t: Float) {
    val length = STREAMER_LENGTH.toPx() * piece.size
    val x0 = piece.x * size.width
    val y0 = fallingY(piece, t, STREAMER_SPEED)
    val path = Path()
    for (i in 0..STREAMER_STEPS) {
        val along = i / STREAMER_STEPS.toFloat()
        val wave = sin(along * STREAMER_WAVES * PI.toFloat() + t * STREAMER_WAVE_RATE + piece.sway)
        val x = x0 + wave * STREAMER_AMPLITUDE.toPx()
        val y = y0 + along * length
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, piece.color, style = Stroke(width = STREAMER_WIDTH.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawBalloon(piece: Piece, t: Float) {
    val w = BALLOON_WIDTH.toPx() * (BALLOON_MIN_SCALE + piece.size * BALLOON_SCALE_RANGE)
    val h = w * BALLOON_ASPECT
    val cx = piece.x * size.width + sin(t * BALLOON_SWAY_RATE + piece.sway) * BALLOON_SWAY.toPx()
    // Rising from below the bottom edge to above the top, in a loop.
    val trip = (piece.phase + piece.speed * BALLOON_SPEED * t) % RISE_LOOP
    val cy = size.height * (1 + RISE_LEAD) - trip * size.height
    val bottom = cy + h / 2
    val string = Path().apply {
        moveTo(cx, bottom)
        quadraticTo(
            cx + sin(t * STRING_SWAY_RATE + piece.sway) * STRING_SWAY.toPx(),
            cy + h,
            cx,
            bottom + h * STRING_LENGTH,
        )
    }
    drawPath(string, Color.White.copy(alpha = STRING_ALPHA), style = Stroke(width = STRING_WIDTH.toPx()))
    val knot = KNOT_SIZE.toPx()
    drawPath(
        Path().apply {
            moveTo(cx, bottom - knot / 2)
            lineTo(cx - knot, bottom + knot)
            lineTo(cx + knot, bottom + knot)
            close()
        },
        piece.color,
    )
    drawOval(piece.color, topLeft = Offset(cx - w / 2, cy - h / 2), size = Size(w, h))
    // A shine on the upper left.
    drawOval(
        Color.White.copy(alpha = SHINE_ALPHA),
        topLeft = Offset(cx - w * SHINE_INSET.x, cy - h * SHINE_INSET.y),
        size = Size(w * SHINE_SIZE.width, h * SHINE_SIZE.height),
    )
}

private const val CONFETTI = 80
private const val STREAMERS = 14
private const val BALLOONS = 7
private const val NANOS_PER_SECOND = 1_000_000_000f

/** Falling pieces loop over 1.2 screen heights, starting 0.1 above the top; balloons over 1.4, from 0.15 below. */
private const val FALL_LOOP = 1.2f
private const val FALL_LEAD = 0.1f
private const val RISE_LOOP = 1.4f
private const val RISE_LEAD = 0.15f

private val CONFETTI_SIZE = DpSize(8.dp, 5.dp)
private val CONFETTI_SWAY = 12.dp
private const val CONFETTI_SWAY_RATE = 2f
private const val CONFETTI_TURN_DEGREES = 40f

private val STREAMER_LENGTH = 70.dp
private val STREAMER_AMPLITUDE = 7.dp
private val STREAMER_WIDTH = 3.dp
private const val STREAMER_SPEED = 0.7f
private const val STREAMER_STEPS = 16
private const val STREAMER_WAVES = 3f
private const val STREAMER_WAVE_RATE = 5f

private val BALLOON_WIDTH = 40.dp
private const val BALLOON_MIN_SCALE = 0.8f
private const val BALLOON_SCALE_RANGE = 0.3f
private const val BALLOON_ASPECT = 1.25f
private val BALLOON_SWAY = 16.dp
private const val BALLOON_SWAY_RATE = 1.3f
private const val BALLOON_SPEED = 0.6f
private val STRING_SWAY = 10.dp
private const val STRING_SWAY_RATE = 3f
private const val STRING_LENGTH = 1.1f
private val STRING_WIDTH = 1.5.dp
private const val STRING_ALPHA = 0.7f
private val KNOT_SIZE = 5.dp
private const val SHINE_ALPHA = 0.35f
private val SHINE_INSET = Offset(0.3f, 0.35f)
private val SHINE_SIZE = Size(0.22f, 0.3f)
