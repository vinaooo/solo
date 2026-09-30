package io.github.vinaooo.solo.feature.game.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vinaooo.solo.feature.game.FeedbackEvent
import io.github.vinaooo.solo.feature.game.GameFeedback
import io.github.vinaooo.solo.feature.game.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidGameFeedback @Inject constructor(@ApplicationContext context: Context) : GameFeedback {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val sounds = mapOf(
        FeedbackEvent.MOVE to soundPool.load(context, R.raw.sfx_move, 1),
        FeedbackEvent.REJECTED to soundPool.load(context, R.raw.sfx_rejected, 1),
        FeedbackEvent.WIN to soundPool.load(context, R.raw.sfx_win, 1),
    )

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    override fun sound(event: FeedbackEvent) {
        sounds[event]?.let { soundPool.play(it, VOLUME, VOLUME, 1, 0, 1f) }
    }

    override fun haptic(event: FeedbackEvent) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        vibrator.vibrate(effectFor(event))
    }

    private fun effectFor(event: FeedbackEvent): VibrationEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.createPredefined(
            when (event) {
                FeedbackEvent.MOVE -> VibrationEffect.EFFECT_TICK
                FeedbackEvent.REJECTED -> VibrationEffect.EFFECT_DOUBLE_CLICK
                FeedbackEvent.WIN -> VibrationEffect.EFFECT_HEAVY_CLICK
            },
        )
    } else {
        VibrationEffect.createOneShot(
            when (event) {
                FeedbackEvent.MOVE -> SHORT_MILLIS
                FeedbackEvent.REJECTED -> MEDIUM_MILLIS
                FeedbackEvent.WIN -> LONG_MILLIS
            },
            VibrationEffect.DEFAULT_AMPLITUDE,
        )
    }

    private companion object {
        const val MAX_STREAMS = 4
        const val VOLUME = 0.6f
        const val SHORT_MILLIS = 10L
        const val MEDIUM_MILLIS = 40L
        const val LONG_MILLIS = 120L
    }
}
