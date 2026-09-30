package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.scoring.ScoreEvent

/** One rule per move type: each knows when its move is legal and how to perform it. */
internal interface MoveRule<M : Move> {
    fun isLegal(state: GameState, move: M): Boolean

    fun perform(state: GameState, move: M): Transition
}

internal object DrawRule : MoveRule<Move.Draw> {
    override fun isLegal(state: GameState, move: Move.Draw) = state.stock.isNotEmpty()

    override fun perform(state: GameState, move: Move.Draw): Transition {
        val drawn = state.stock.takeLast(state.drawMode.cardsPerDraw).reversed().map { it.faceUp() }
        return Transition(state.copy(stock = state.stock.dropLast(drawn.size), waste = state.waste + drawn))
    }
}

internal object RecycleRule : MoveRule<Move.Recycle> {
    override fun isLegal(state: GameState, move: Move.Recycle) = state.stock.isEmpty() && state.waste.isNotEmpty()

    override fun perform(state: GameState, move: Move.Recycle): Transition {
        val recycles = state.recycles + 1
        return Transition(
            state.copy(stock = state.waste.reversed().map { it.faceDown() }, waste = emptyList(), recycles = recycles),
            listOf(ScoreEvent.Recycle(state.drawMode, recycles)),
        )
    }
}

internal object WasteToTableauRule : MoveRule<Move.WasteToTableau> {
    override fun isLegal(state: GameState, move: Move.WasteToTableau): Boolean {
        val card = state.waste.lastOrNull() ?: return false
        return move.to.isTableauIndex() && PlacementRules.canStackOnTableau(card, state.tableau[move.to])
    }

    override fun perform(state: GameState, move: Move.WasteToTableau) = Transition(
        state.copy(waste = state.waste.dropLast(1))
            .withTableauPile(move.to, state.tableau[move.to] + state.waste.last()),
        listOf(ScoreEvent.WasteToTableau),
    )
}

internal object WasteToFoundationRule : MoveRule<Move.WasteToFoundation> {
    override fun isLegal(state: GameState, move: Move.WasteToFoundation): Boolean {
        val card = state.waste.lastOrNull() ?: return false
        return move.to.isFoundationIndex() && PlacementRules.canStackOnFoundation(card, state.foundations[move.to])
    }

    override fun perform(state: GameState, move: Move.WasteToFoundation) = Transition(
        state.copy(waste = state.waste.dropLast(1))
            .withFoundationPile(move.to, state.foundations[move.to] + state.waste.last()),
        listOf(ScoreEvent.WasteToFoundation),
    )
}

internal object TableauToTableauRule : MoveRule<Move.TableauToTableau> {
    override fun isLegal(state: GameState, move: Move.TableauToTableau): Boolean {
        if (!move.from.isTableauIndex() || !move.to.isTableauIndex() || move.from == move.to) return false
        val source = state.tableau[move.from]
        if (move.count !in 1..source.size) return false
        val run = source.takeLast(move.count)
        return PlacementRules.isMovableRun(run) && PlacementRules.canStackOnTableau(run.first(), state.tableau[move.to])
    }

    override fun perform(state: GameState, move: Move.TableauToTableau): Transition {
        val (taken, run) = state.takeFromTableau(move.from, move.count)
        return taken.copy(state = taken.state.withTableauPile(move.to, taken.state.tableau[move.to] + run))
    }
}

internal object TableauToFoundationRule : MoveRule<Move.TableauToFoundation> {
    override fun isLegal(state: GameState, move: Move.TableauToFoundation): Boolean {
        if (!move.from.isTableauIndex() || !move.to.isFoundationIndex()) return false
        val card = state.tableau[move.from].lastOrNull() ?: return false
        return PlacementRules.canStackOnFoundation(card, state.foundations[move.to])
    }

    override fun perform(state: GameState, move: Move.TableauToFoundation): Transition {
        val (taken, cards) = state.takeFromTableau(move.from, 1)
        return Transition(
            taken.state.withFoundationPile(move.to, taken.state.foundations[move.to] + cards),
            listOf(ScoreEvent.TableauToFoundation) + taken.events,
        )
    }
}

internal object FoundationToTableauRule : MoveRule<Move.FoundationToTableau> {
    override fun isLegal(state: GameState, move: Move.FoundationToTableau): Boolean {
        if (!move.from.isFoundationIndex() || !move.to.isTableauIndex()) return false
        val card = state.foundations[move.from].lastOrNull() ?: return false
        return PlacementRules.canStackOnTableau(card, state.tableau[move.to])
    }

    override fun perform(state: GameState, move: Move.FoundationToTableau): Transition {
        val card = state.foundations[move.from].last()
        return Transition(
            state.withFoundationPile(move.from, state.foundations[move.from].dropLast(1))
                .withTableauPile(move.to, state.tableau[move.to] + card),
            listOf(ScoreEvent.FoundationToTableau),
        )
    }
}
