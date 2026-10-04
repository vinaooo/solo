package io.github.vinaooo.solo.domain.session

import io.github.vinaooo.solo.domain.model.GameState
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.serialization.json.Json

/**
 * A board as a short text, for bug reports: its JSON, gzipped and in Base64, about a thousand characters, short
 * enough for a GitHub issue link. The debug build turns it back into the board to replay the report.
 */
object GameCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(state: GameState): String {
        val bytes = ByteArrayOutputStream()
        GZIPOutputStream(bytes).use { it.write(json.encodeToString(state).toByteArray()) }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    /** Line breaks and spaces, as an issue or an email may add, are ignored. */
    fun decode(code: String): GameState {
        val zipped = Base64.getMimeDecoder().decode(code.trim())
        return json.decodeFromString(GZIPInputStream(zipped.inputStream()).use { String(it.readBytes()) })
    }

    /** A whole game, as a bug report's game.json holds it. */
    fun decodeSession(text: String): GameSession = json.decodeFromString(text)
}
