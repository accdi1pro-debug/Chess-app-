package com.example.chessapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Pure UI: draws [GameController.state] and forwards clicks.
 * It never calls the engine itself, so the main thread never waits for it.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var chessBoardView: ChessBoardView
    private lateinit var tvStatus: TextView
    private lateinit var tvEval: TextView
    private lateinit var tvWhiteClock: TextView
    private lateinit var tvBlackClock: TextView
    private lateinit var pbThinking: ProgressBar

    private lateinit var btnResetGame: Button
    private lateinit var btnOpenSettings: Button

    private lateinit var promotionBar: View
    private lateinit var btnPromoQueen: ImageButton
    private lateinit var btnPromoRook: ImageButton
    private lateinit var btnPromoBishop: ImageButton
    private lateinit var btnPromoKnight: ImageButton
    private var promotionBarIsWhite: Boolean? = null


    /** Collects the game state while the screen is visible (started in onStart, stopped in onStop). */
    private var stateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GameController.start()   // no-op if the Splash already started it

        bindViews()
        chessBoardView.autoRotatePiecesIn1v1 = GameController.autoRotatePiecesIn1v1
        chessBoardView.isPiecesFlippedManual = GameController.isPiecesFlippedManual
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        GameController.setVisible(true)
        stateJob?.cancel()
        stateJob = lifecycleScope.launch {
            launch { GameController.state.collect { render(it) } }
            launch { GameController.clock.collect { renderClock(it) } }
        }
    }

    override fun onStop() {
        super.onStop()
        stateJob?.cancel()
        stateJob = null
        GameController.setVisible(false)   // pause the game clock while the app is in the background
    }

    private fun bindViews() {
        chessBoardView = findViewById(R.id.chessBoardView)
        tvStatus = findViewById(R.id.tvStatus)
        tvEval = findViewById(R.id.tvEval)
        tvWhiteClock = findViewById(R.id.tvWhiteClock)
        tvBlackClock = findViewById(R.id.tvBlackClock)
        pbThinking = findViewById(R.id.pbThinking)

        btnResetGame = findViewById(R.id.btnResetGame)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)

        promotionBar = findViewById(R.id.promotionBar)
        btnPromoQueen = findViewById(R.id.btnPromoQueen)
        btnPromoRook = findViewById(R.id.btnPromoRook)
        btnPromoBishop = findViewById(R.id.btnPromoBishop)
        btnPromoKnight = findViewById(R.id.btnPromoKnight)
    }

    private fun setupListeners() {
        chessBoardView.onSquareTapped = { square -> GameController.onSquareTapped(square) }

        btnResetGame.setOnClickListener { GameController.newGame() }
        btnOpenSettings.setOnClickListener { showSettingsDialog() }

        // Engine codes: 1 = knight, 2 = rook, 3 = queen, 4 = bishop
        btnPromoQueen.setOnClickListener { GameController.promote(3) }
        btnPromoRook.setOnClickListener { GameController.promote(2) }
        btnPromoBishop.setOnClickListener { GameController.promote(4) }
        btnPromoKnight.setOnClickListener { GameController.promote(1) }
    }

    // ------------------------------------------------------------------ rendering

    private fun render(s: GameState) {
        chessBoardView.render(s)

        val turnStr = when {
            !s.ready -> "Loading…"
            s.gameFinished -> when (s.won) {
                1 -> "White wins ${s.resultReason}"
                2 -> "Black wins ${s.resultReason}"
                else -> "Draw (${s.resultReason})"
            }
            s.promotionPending -> "Choose a piece below"
            s.turn -> "White to move"
            else -> "Black to move"
        }
        val checkStr = if (!s.gameFinished && (s.whiteInCheck || s.blackInCheck)) " (Check!)" else ""
        tvStatus.text = turnStr.trim() + checkStr

        tvEval.text = if (s.aiThinking) "Thinking…" else s.evalText
        pbThinking.visibility = if (s.aiThinking) View.VISIBLE else View.INVISIBLE

        renderPromotionBar(s)
    }

    private fun renderPromotionBar(s: GameState) {
        if (!s.promotionPending) {
            promotionBar.visibility = View.GONE
            return
        }
        if (promotionBarIsWhite != s.promotionIsWhite) {
            promotionBarIsWhite = s.promotionIsWhite
            if (s.promotionIsWhite) {
                btnPromoQueen.setImageResource(R.drawable.wq)
                btnPromoRook.setImageResource(R.drawable.wr)
                btnPromoBishop.setImageResource(R.drawable.wb)
                btnPromoKnight.setImageResource(R.drawable.wkn)
            } else {
                btnPromoQueen.setImageResource(R.drawable.bq)
                btnPromoRook.setImageResource(R.drawable.br)
                btnPromoBishop.setImageResource(R.drawable.bb)
                btnPromoKnight.setImageResource(R.drawable.bkh)
            }
        }
        promotionBar.visibility = View.VISIBLE
    }

    private fun renderClock(c: ClockState) {
        val visibility = if (c.enabled) View.VISIBLE else View.GONE
        tvWhiteClock.visibility = visibility
        tvBlackClock.visibility = visibility
        if (!c.enabled) return
        tvWhiteClock.text = "White: ${formatTime(c.whiteMs)}"
        tvBlackClock.text = "Black: ${formatTime(c.blackMs)}"
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms + 999) / 1000   // round up: show 0:00 only when the time is really over
        return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    // ------------------------------------------------------------------ settings

    private fun showSettingsDialog() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null, false)
        dialog.setContentView(view)

        val tgWhite = view.findViewById<MaterialButtonToggleGroup>(R.id.tgWhitePlayerDialog)
        val tgBlack = view.findViewById<MaterialButtonToggleGroup>(R.id.tgBlackPlayerDialog)
        val tgTime = view.findViewById<MaterialButtonToggleGroup>(R.id.tgTimeLimitDialog)
        val tgClock = view.findViewById<MaterialButtonToggleGroup>(R.id.tgClockDurationDialog)

        val switchRandomColor = view.findViewById<MaterialSwitch>(R.id.switchRandomColorDialog)
        val switchAutoRotate = view.findViewById<MaterialSwitch>(R.id.switchAutoRotateDialog)
        val switchManualFlip = view.findViewById<MaterialSwitch>(R.id.switchManualFlipDialog)
        val btnClose = view.findViewById<Button>(R.id.btnCloseSettings)

        // true while WE change the toggles (so their listeners don't start another new game)
        var syncing = false

        fun syncPlayerToggles(s: GameState) {
            syncing = true
            tgWhite.check(if (s.whiteAi) R.id.btnWhiteAiDialog else R.id.btnWhiteHumanDialog)
            tgBlack.check(if (s.blackAi) R.id.btnBlackAiDialog else R.id.btnBlackHumanDialog)
            syncing = false
        }

        syncPlayerToggles(GameController.state.value)

        tgTime.check(
            when (GameController.thinkTimeMs) {
                1000L -> R.id.btnTime1sDialog
                2000L -> R.id.btnTime2sDialog
                8000L -> R.id.btnTime8sDialog
                else -> R.id.btnTime4sDialog
            }
        )

        tgClock.check(
            when (GameController.clockDurationSeconds) {
                180 -> R.id.btnClock3m
                300 -> R.id.btnClock5m
                600 -> R.id.btnClock10m
                else -> R.id.btnClockNone
            }
        )

        switchRandomColor.isChecked = GameController.randomStartingColor
        switchAutoRotate.isChecked = chessBoardView.autoRotatePiecesIn1v1
        switchManualFlip.isChecked = chessBoardView.isPiecesFlippedManual

        // Keep the White/Black toggles in sync with the real game (random colour can swap them).
        val syncJob: Job = lifecycleScope.launch {
            GameController.state.collect { syncPlayerToggles(it) }
        }
        dialog.setOnDismissListener { syncJob.cancel() }

        tgWhite.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !syncing) {
                val whiteAi = checkedId == R.id.btnWhiteAiDialog
                val blackAi = tgBlack.checkedButtonId == R.id.btnBlackAiDialog
                GameController.setPlayers(whiteAi, blackAi)
            }
        }

        tgBlack.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !syncing) {
                val whiteAi = tgWhite.checkedButtonId == R.id.btnWhiteAiDialog
                val blackAi = checkedId == R.id.btnBlackAiDialog
                GameController.setPlayers(whiteAi, blackAi)
            }
        }

        tgTime.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                GameController.setThinkTime(
                    when (checkedId) {
                        R.id.btnTime1sDialog -> 1000L
                        R.id.btnTime2sDialog -> 2000L
                        R.id.btnTime8sDialog -> 8000L
                        else -> 4000L
                    }
                )
            }
        }

        tgClock.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                GameController.setClockDuration(
                    when (checkedId) {
                        R.id.btnClock3m -> 180
                        R.id.btnClock5m -> 300
                        R.id.btnClock10m -> 600
                        else -> 0
                    }
                )
            }
        }

        switchRandomColor.setOnCheckedChangeListener { _, isChecked ->
            GameController.setRandomStartingColor(isChecked)
        }

        switchAutoRotate.setOnCheckedChangeListener { _, isChecked ->
            GameController.autoRotatePiecesIn1v1 = isChecked
            chessBoardView.autoRotatePiecesIn1v1 = isChecked
        }

        switchManualFlip.setOnCheckedChangeListener { _, isChecked ->
            GameController.isPiecesFlippedManual = isChecked
            chessBoardView.isPiecesFlippedManual = isChecked
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
