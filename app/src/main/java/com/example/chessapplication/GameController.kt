package com.example.chessapplication

import android.os.SystemClock
import android.util.Log
import com.example.chesslib.Board
import com.example.chesslib.EvaluationFuncion
import com.example.chesslib.MagicBitBoards
import com.example.chesslib.MovementManager
import com.example.chesslib.Move
import com.example.chesslib.MovesGenerator
import com.example.chesslib.Zobrist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Everything the screen needs to draw one frame. Immutable: safe to read from any thread. */
class GameState(
    val pieces: IntArray,
    val turn: Boolean,
    val attackSquares: Long,
    val captureSquares: Long,
    val lastMove: Long,
    val whiteInCheck: Boolean,
    val blackInCheck: Boolean,
    val gameFinished: Boolean,
    val won: Int,
    val resultReason: String,
    val promotionPending: Boolean,
    val promotionIsWhite: Boolean,
    val whiteAi: Boolean,
    val blackAi: Boolean,
    val aiThinking: Boolean,
    val evalText: String,
    val moveList: List<String>,
    val canUndo: Boolean,
    val ready: Boolean
) {
    val isAiTurn: Boolean get() = if (turn) whiteAi else blackAi
}

class ClockState(val enabled: Boolean, val whiteMs: Long, val blackMs: Long)

/**
 * The only place that touches the engine.
 *
 * The engine (Board + MovementManager) is global static state, so ALL engine work runs on ONE
 * background thread ("chess-engine"): taps, AI search, evaluation, undo, new game. Nothing can run at
 * the same time, so the board can never be corrupted, and the main thread never waits for the engine
 * (no "App not responding").
 *
 * The UI only reads [state], a snapshot published by the engine thread when the board is stable.
 * It lives as long as the app process, so the game, clock and settings survive rotation/theme changes.
 */
object GameController {

    private const val TAG = "GameController"

    // ---------------------------------------------------------------- engine thread
    private val engine: ExecutorService = Executors.newSingleThreadExecutor { r ->
        // Big stack: the search is recursive. Lower priority: the UI thread always wins.
        Thread(null, r, "chess-engine", 16L * 1024 * 1024).apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }

    /** Main-thread scope for the clock ticker. */
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val mm: MovementManager get() = MovementManager.INSTANCE

    /** Bumped every time running/queued AI work must be thrown away (new game, undo, flag fall). */
    @Volatile private var generation = 0

    @Volatile private var started = false

    // ------- engine-thread-only fields (never touched from the main thread)
    private class HistoryEntry(
        val boardState: Board.State,
        val managerState: MovementManager.State,
        val lastMove: Long,
        val evalText: String
    ) {
        var notation: String? = null
        var hashAfter: Long = 0L
    }

    private val history = ArrayList<HistoryEntry>()
    private var initialHash = 0L
    private var aiThinking = false
    private var evalText = "Eval: 0.00"
    private var resultReasonOverride: String? = null
    private var pendingFrom = 0
    private var pendingPiece = 0
    private var pendingCapture = false

    // ------- settings (written on main thread, read on engine thread)
    @Volatile var thinkTimeMs: Long = MovesGenerator.TIME_LIMIT_MS
        private set
    @Volatile var randomStartingColor: Boolean = false
        private set
    var clockDurationSeconds: Int = 0
        private set

    // Board display options, kept here so they survive rotation / theme changes.
    var autoRotatePiecesIn1v1: Boolean = true
    var isPiecesFlippedManual: Boolean = false

    // ------- clock (main thread)
    @Volatile private var whiteMs = 0L
    @Volatile private var blackMs = 0L
    private var clockJob: Job? = null

    private val _state = MutableStateFlow(emptyState())
    val state: StateFlow<GameState> = _state

    private val _clock = MutableStateFlow(ClockState(false, 0L, 0L))
    val clock: StateFlow<ClockState> = _clock

    // ================================================================= public API (main thread)

    /** Safe to call many times (Splash + MainActivity). Builds the tables off the main thread. */
    fun start() {
        if (started) return
        started = true
        runOnEngine("start") {
            MagicBitBoards.init()
            mm.randomStartingColor = false   // the first game uses the players exactly as configured
            mm.resetGame()
            MovesGenerator.newGame()
            history.clear()
            initialHash = Zobrist.hash()
            evalText = "Eval: 0.00"
            resultReasonOverride = null
            publish()
            maybeStartAi()
        }
    }

    fun onSquareTapped(square: Int) {
        if (square !in 0..63) return
        runOnEngine("tap") {
            if (!canHumanAct()) return@runOnEngine

            val isTarget = (Board.atackSquares ushr square and 1L) != 0L
            if (isTarget && mm.pieceMoved != 0) {
                val from = mm.pieceMovedPos
                val piece = mm.pieceMoved
                pushHistory()
                try {
                    mm.movePiece(square)
                } catch (t: Throwable) {
                    Log.e(TAG, "movePiece failed, undoing it", t)
                    popHistory()
                    publish()
                    return@runOnEngine
                }
                if (mm.isPromotionPending) {
                    // Wait for the player to pick a piece in the bar under the board.
                    pendingFrom = from
                    pendingPiece = piece
                    pendingCapture = mm.isCapture
                } else {
                    afterMove(notation(piece, from, square, mm.isCapture, null))
                }
            } else {
                mm.showAtackSquare(mm.returnPiece(square), square)
            }
            publish()
            maybeStartAi()
        }
    }

    /** 1 = knight, 2 = rook, 3 = queen, 4 = bishop (engine codes). */
    fun promote(pieceCode: Int) {
        runOnEngine("promote") {
            if (!mm.isPromotionPending) return@runOnEngine
            val white = mm.whitePromotionUI
            val square = mm.promotionsquare
            mm.promotionPiece = pieceCode
            try {
                if (white) mm.whitePromotion(square) else mm.blackPromotion(square)
            } catch (t: Throwable) {
                Log.e(TAG, "promotion failed, undoing the pawn move", t)
                popHistory()
                publish()
                return@runOnEngine
            }
            val letter = when (pieceCode) {
                1 -> "N"; 2 -> "R"; 4 -> "B"; else -> "Q"
            }
            afterMove(notation(pendingPiece, pendingFrom, square, pendingCapture, letter))
            publish()
            maybeStartAi()
        }
    }

    fun newGame() {
        cancelAi()
        resetClocks()
        runOnEngine("newGame") {
            MagicBitBoards.init()
            mm.randomStartingColor = randomStartingColor
            mm.resetGame()                 // may swap sides when random colour is on (human vs AI only)
            MovesGenerator.newGame()       // clear transposition table, killers, history
            history.clear()
            initialHash = Zobrist.hash()
            evalText = "Eval: 0.00"
            resultReasonOverride = null
            aiThinking = false
            publish()
            maybeStartAi()
        }
    }

    fun undo() {
        cancelAi()
        runOnEngine("undo") {
            aiThinking = false
            if (history.isEmpty()) {
                publish()
                return@runOnEngine
            }
            val bothAi = mm.whiteAi && mm.BlackAi
            popHistory()
            // Against the AI: go back to the last position where it's the human's turn.
            while (!bothAi && history.isNotEmpty() && isAiTurnEngine()) {
                popHistory()
            }
            resultReasonOverride = null
            publish()
            maybeStartAi()
        }
    }

    fun setPlayers(whiteAi: Boolean, blackAi: Boolean) {
        cancelAi()
        runOnEngine("setPlayers") {
            mm.whiteAi = whiteAi
            mm.BlackAi = blackAi
        }
        newGame()
    }

    fun setRandomStartingColor(enabled: Boolean) {
        randomStartingColor = enabled
        newGame()
    }

    /** Applies from the AI's next move. */
    fun setThinkTime(ms: Long) {
        thinkTimeMs = ms
        MovesGenerator.TIME_LIMIT_MS = ms
    }

    fun setClockDuration(seconds: Int) {
        clockDurationSeconds = seconds
        resetClocks()
    }

    /** The clock only runs while the game screen is visible. */
    fun setVisible(visible: Boolean) {
        clockJob?.cancel()
        clockJob = null
        if (visible) startClockTicker()
    }

    // ================================================================= clock (main thread)

    private fun resetClocks() {
        whiteMs = clockDurationSeconds * 1000L
        blackMs = clockDurationSeconds * 1000L
        _clock.value = ClockState(clockDurationSeconds > 0, whiteMs, blackMs)
    }

    private fun startClockTicker() {
        clockJob = mainScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(100L)
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - last
                last = now
                tickClock(elapsed)
            }
        }
    }

    private fun tickClock(elapsedMs: Long) {
        if (clockDurationSeconds <= 0) return
        val s = _state.value
        // Uses the PUBLISHED side to move, never the engine fields (they flip during a search).
        if (!s.ready || s.gameFinished) return
        if (s.turn) {
            if (whiteMs <= 0L) return
            whiteMs = (whiteMs - elapsedMs).coerceAtLeast(0L)
            if (whiteMs == 0L) flagFell(whiteLost = true)
        } else {
            if (blackMs <= 0L) return
            blackMs = (blackMs - elapsedMs).coerceAtLeast(0L)
            if (blackMs == 0L) flagFell(whiteLost = false)
        }
        _clock.value = ClockState(true, whiteMs, blackMs)
    }

    private fun flagFell(whiteLost: Boolean) {
        cancelAi()
        runOnEngine("flag") {
            aiThinking = false
            if (!mm.gameFinished) {
                if (mm.isPromotionPending) popHistory()   // the unfinished pawn move doesn't count
                mm.gameFinished = true
                mm.won = if (whiteLost) 2 else 1
                resultReasonOverride = "on time"
                Board.atackSquares = 0L
                Board.captureSquares = 0L
            }
            publish()
        }
    }

    // ================================================================= AI (engine thread)

    /** Main thread: throw away any running or queued search. The engine thread finishes within ~ms. */
    private fun cancelAi() {
        generation++
        MovesGenerator.requestStop()
    }

    private fun isAiTurnEngine(): Boolean = if (mm.turn) mm.whiteAi else mm.BlackAi

    private fun maybeStartAi() {
        if (aiThinking || mm.gameFinished || mm.isPromotionPending || !isAiTurnEngine()) return
        aiThinking = true
        Board.atackSquares = 0L
        Board.captureSquares = 0L
        publish()
        val myGeneration = generation
        // Queued (not run inline) so a New game / Undo submitted meanwhile runs first and cancels it.
        runOnEngine("ai") { runAi(myGeneration) }
    }

    private fun runAi(myGeneration: Int) {
        // Order matters: clear the old stop flag FIRST, then check whether this search is still wanted.
        MovesGenerator.clearStop()
        if (myGeneration != generation || mm.gameFinished || !isAiTurnEngine()) {
            aiThinking = false
            publish()
            return
        }

        val boardBefore = Board.captureState()
        val managerBefore = mm.captureState()
        val lastMoveBefore = Board.move
        val result: MovesGenerator.SearchResult? = try {
            MovesGenerator.search(thinkTimeFor(mm.turn))
        } catch (t: Throwable) {
            Log.e(TAG, "search crashed, using a fallback move", t)
            null
        }
        // The search undoes its own moves, but restore anyway so a bug can never leave a broken board.
        Board.resetState(boardBefore)
        mm.resetState(managerBefore)
        Board.move = lastMoveBefore
        aiThinking = false

        if (myGeneration != generation) {   // cancelled while thinking: drop the result
            publish()
            return
        }

        var move = result?.move()
        if (move == null || !isLegal(move)) move = firstLegalMove()
        if (move == null) {                 // nothing playable: show the position as it is
            publish()
            return
        }

        val piece = move.piece()
        val from = move.from()
        val to = move.to()
        pushHistory()
        try {
            MovesGenerator.Move(move)
        } catch (t: Throwable) {
            Log.e(TAG, "AI move failed, undoing it", t)
            popHistory()
            publish()
            return
        }
        if (mm.isPromotionPending) {        // should never happen (AI always queens) but never get stuck
            mm.promotionPiece = 3
            if (mm.whitePromotionUI) mm.whitePromotion(mm.promotionsquare) else mm.blackPromotion(mm.promotionsquare)
        }
        val promo = if ((piece == 1 && to >= 56) || (piece == 7 && to < 8)) "Q" else null
        afterMove(notation(piece, from, to, mm.isCapture, promo))
        if (result != null && result.depth() > 0) {
            evalText = "Eval: ${formatScore(result.score())} | Depth: ${result.depth()}"
        }
        publish()
        maybeStartAi()                      // AI vs AI: next move
    }

    /** Uses the game clock when it's on: about 1/30 of the remaining time, never more than the setting. */
    private fun thinkTimeFor(white: Boolean): Long {
        val setting = thinkTimeMs
        if (clockDurationSeconds <= 0) return setting
        val remaining = if (white) whiteMs else blackMs
        return (remaining / 30L).coerceIn(100L, setting.coerceAtLeast(100L))
    }

    private fun legalMoves(): List<Move> {
        val saved = Board.captureState()
        return try {
            if (mm.turn) MovesGenerator.generateWhiteMoves() else MovesGenerator.generateBlackMoves()
        } finally {
            Board.resetState(saved)
        }
    }

    private fun isLegal(move: Move): Boolean = legalMoves().any { it == move }

    private fun firstLegalMove(): Move? = legalMoves().firstOrNull()

    // ================================================================= history / repetition

    private fun pushHistory() {
        history.add(HistoryEntry(Board.captureState(), mm.captureState(), Board.move, evalText))
    }

    private fun popHistory() {
        if (history.isEmpty()) return
        val e = history.removeAt(history.size - 1)
        Board.resetState(e.boardState)
        mm.resetState(e.managerState)
        Board.move = e.lastMove
        Board.atackSquares = 0L
        Board.captureSquares = 0L
        mm.pieceMoved = 0
        mm.pieceMovedPos = 0
        evalText = e.evalText
    }

    /** Called after every completed move (human, promotion or AI). */
    private fun afterMove(notationText: String) {
        val entry = history.lastOrNull() ?: return
        entry.notation = notationText
        entry.hashAfter = Zobrist.hash()

        // Threefold repetition (the engine itself doesn't know the game history).
        if (!mm.gameFinished) {
            var count = if (initialHash == entry.hashAfter) 1 else 0
            for (h in history) if (h.notation != null && h.hashAfter == entry.hashAfter) count++
            if (count >= 3) {
                mm.gameFinished = true
                mm.won = 3
                resultReasonOverride = "threefold repetition"
            }
        }

        // Next player is a human: show the static evaluation (cheap, and safe here on the engine thread).
        if (!mm.gameFinished && !isAiTurnEngine()) {
            evalText = "Eval: ${formatScore(safeEvaluate())} (static)"
        }
        if (mm.gameFinished) evalText = ""
    }

    private fun safeEvaluate(): Int {
        val saved = Board.captureState()
        return try {
            EvaluationFuncion.Evaluate()
        } catch (t: Throwable) {
            Log.e(TAG, "evaluation failed", t)
            0
        } finally {
            Board.resetState(saved)
        }
    }

    // ================================================================= publishing

    private fun canHumanAct(): Boolean =
        started && !aiThinking && !mm.gameFinished && !mm.isPromotionPending && !isAiTurnEngine()

    /** Engine thread only, and only when the board is stable (never during a search). */
    private fun publish() {
        val pieces = IntArray(64) { mm.returnPiece(it) }
        val reason = resultReasonOverride ?: when (mm.won) {
            1, 2 -> "by checkmate"
            3 -> if (mm.count >= 50) "50-move rule" else "stalemate"
            else -> ""
        }
        _state.value = GameState(
            pieces = pieces,
            turn = mm.turn,
            attackSquares = Board.atackSquares,
            captureSquares = Board.captureSquares,
            lastMove = Board.move,
            whiteInCheck = mm.whiteIsInCheck,
            blackInCheck = mm.blackIsInCheck,
            gameFinished = mm.gameFinished,
            won = mm.won,
            resultReason = reason,
            promotionPending = mm.isPromotionPending,
            promotionIsWhite = mm.whitePromotionUI,
            whiteAi = mm.whiteAi,
            blackAi = mm.BlackAi,
            aiThinking = aiThinking,
            evalText = evalText,
            moveList = history.mapNotNull { it.notation },
            canUndo = history.isNotEmpty() && !(mm.whiteAi && mm.BlackAi),
            ready = true
        )
    }

    private fun emptyState() = GameState(
        pieces = IntArray(64), turn = true, attackSquares = 0L, captureSquares = 0L, lastMove = 0L,
        whiteInCheck = false, blackInCheck = false, gameFinished = false, won = 0, resultReason = "",
        promotionPending = false, promotionIsWhite = true, whiteAi = false, blackAi = true,
        aiThinking = false, evalText = "Loading…", moveList = emptyList(), canUndo = false, ready = false
    )

    /** Every engine task goes through here: one thread, and an engine bug can never crash the app. */
    private inline fun runOnEngine(name: String, crossinline task: () -> Unit) {
        engine.execute {
            try {
                task()
            } catch (t: Throwable) {
                Log.e(TAG, "engine task '$name' failed", t)
                try {
                    publish()
                } catch (ignored: Throwable) {
                    Log.e(TAG, "publish failed after '$name'", ignored)
                }
            }
        }
    }

    // ================================================================= text helpers

    private fun squareName(sq: Int): String {
        val file = 'a' + (7 - sq % 8)
        val rank = sq / 8 + 1
        return "$file$rank"
    }

    private fun notation(piece: Int, from: Int, to: Int, capture: Boolean, promotion: String?): String {
        val isKing = piece == 6 || piece == 12
        val base = if (isKing && kotlin.math.abs(from - to) == 2) {
            if (to < from) "O-O" else "O-O-O"
        } else {
            val letter = when (piece) {
                2, 8 -> "B"; 3, 9 -> "R"; 4, 10 -> "Q"; 5, 11 -> "N"; 6, 12 -> "K"; else -> ""
            }
            letter + squareName(from) + (if (capture) "x" else "-") + squareName(to) +
                (if (promotion != null) "=$promotion" else "")
        }
        val suffix = when {
            mm.gameFinished && (mm.won == 1 || mm.won == 2) -> "#"
            mm.whiteIsInCheck || mm.blackIsInCheck -> "+"
            else -> ""
        }
        return base + suffix
    }

    private fun formatScore(score: Int): String = when {
        score >= 8_000_000 -> "mate for White"
        score <= -8_000_000 -> "mate for Black"
        else -> String.format(Locale.US, "%+.2f", score / 100.0)
    }
}
