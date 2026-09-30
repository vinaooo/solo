package io.github.vinaooo.solo.data.game

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.session.GameSession
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FileSavedGameRepositoryTest {

    @TempDir
    lateinit var dir: File

    private val dispatcher = StandardTestDispatcher()
    private val file get() = File(dir, "saved_game.json")
    private val repository get() = FileSavedGameRepository(file, dispatcher)

    private val session = GameSession(11, Dealer().deal(SeededShuffler(11), DrawMode.THREE))
        .play(Move.Draw, GameEngine())!!

    @Test
    fun `nothing saved loads as null`() = runTest(dispatcher) {
        repository.load().shouldBeNull()
    }

    @Test
    fun `a saved session loads back with its undo history`() = runTest(dispatcher) {
        repository.save(session)

        val loaded = FileSavedGameRepository(file, dispatcher).load()

        loaded shouldBe session
        loaded!!.undo()!!.state.stock shouldBe session.undo()!!.state.stock
    }

    @Test
    fun `saving again replaces the previous game`() = runTest(dispatcher) {
        repository.save(session)
        val newer = session.play(Move.Draw, GameEngine())!!
        repository.save(newer)

        repository.load() shouldBe newer
    }

    @Test
    fun `clear removes the saved game`() = runTest(dispatcher) {
        repository.save(session)
        repository.clear()

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a corrupted file is discarded instead of crashing`() = runTest(dispatcher) {
        file.writeText("{ not json")

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a file from an unknown future version is ignored`() = runTest(dispatcher) {
        repository.save(session)
        file.writeText(file.readText().replace("\"version\":1", "\"version\":99"))

        repository.load().shouldBeNull()
    }

    @Test
    fun `no temporary files are left behind`() = runTest(dispatcher) {
        repository.save(session)

        dir.listFiles()!!.map { it.name } shouldBe listOf("saved_game.json")
    }
}
