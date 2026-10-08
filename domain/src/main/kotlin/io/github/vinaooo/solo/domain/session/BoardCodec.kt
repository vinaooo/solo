package io.github.vinaooo.solo.domain.session

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.vinkit.core.GameCodec
import kotlinx.serialization.json.Json

/**
 * A board as a short text, for bug reports (vinkit's codec: gzipped JSON in Base64, about a thousand characters). The
 * debug build turns it back into the board to replay the report.
 */
val BoardCodec = GameCodec(GameState.serializer())

/** A whole game, as a bug report's game.json holds it. */
fun decodeSession(text: String): GameSession = reportJson.decodeFromString(text)

private val reportJson = Json { ignoreUnknownKeys = true }
