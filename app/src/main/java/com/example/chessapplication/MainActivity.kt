package com.example.chessapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.chesslib.EvaluationFuncion
import com.example.chesslib.MovementManager
import com.example.chesslib.MovesGenerator
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var chessBoardView: ChessBoardView
    private lateinit var tvStatus: TextView
    private lateinit var tvEval: TextView
    private lateinit var tvWhiteClock: TextView
    private lateinit var tvBlackClock: TextView
    private lateinit var pbThinking: ProgressBar

    private lateinit var btnResetGame: Button
    private lateinit var btnOpenSettings: Button

    private var aiJob: Job? = null
    private var clockJob: Job? = null

    // Tournament Clock state
    private var clockDurationSeconds: Int = 0 // 0 = Off, 180 = 3m, 300 = 5m, 600 = 10m
    private var whiteTimeSeconds: Int = 0
    private var blackTimeSeconds: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupListeners()
        resetClocks()
        startClockTicker()
        updateGameStatusUI()
        checkAndTriggerAiMove()
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
    }

    private fun setupListeners() {
        chessBoardView.onStateChangedListener = {
            updateGameStatusUI()
            checkAndTriggerAiMove()
        }

        btnResetGame.setOnClickListener {
            aiJob?.cancel()
            chessBoardView.resetGame()
            resetClocks()
            updateGameStatusUI()
            checkAndTriggerAiMove()
        }

        btnOpenSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun resetClocks() {
        whiteTimeSeconds = clockDurationSeconds
        blackTimeSeconds = clockDurationSeconds
        if (clockDurationSeconds > 0) {
            tvWhiteClock.visibility = View.VISIBLE
            tvBlackClock.visibility = View.VISIBLE
            updateClockDisplay()
        } else {
            tvWhiteClock.visibility = View.GONE
            tvBlackClock.visibility = View.GONE
        }
    }

    private fun updateClockDisplay() {
        tvWhiteClock.text = "White: ${formatTime(whiteTimeSeconds)}"
        tvBlackClock.text = "Black: ${formatTime(blackTimeSeconds)}"
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%d:%02d", m, s)
    }

    private fun startClockTicker() {
        clockJob?.cancel()
        clockJob = lifecycleScope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(1000L)
                val mm = MovementManager.INSTANCE
                if (clockDurationSeconds > 0 && !mm.gameFinished) {
                    if (mm.turn) {
                        if (whiteTimeSeconds > 0) {
                            whiteTimeSeconds--
                            if (whiteTimeSeconds == 0) {
                                mm.gameFinished = true
                                mm.won = 2 // Black wins on time
                                updateGameStatusUI()
                            }
                        }
                    } else {
                        if (blackTimeSeconds > 0) {
                            blackTimeSeconds--
                            if (blackTimeSeconds == 0) {
                                mm.gameFinished = true
                                mm.won = 1 // White wins on time
                                updateGameStatusUI()
                            }
                        }
                    }
                    updateClockDisplay()
                }
            }
        }
    }

    private fun showSettingsDialog() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null, false)
        dialog.setContentView(view)

        val tgWhite = view.findViewById<MaterialButtonToggleGroup>(R.id.tgWhitePlayerDialog)
        val tgBlack = view.findViewById<MaterialButtonToggleGroup>(R.id.tgBlackPlayerDialog)
        val tgTime = view.findViewById<MaterialButtonToggleGroup>(R.id.tgTimeLimitDialog)
        val tgClock = view.findViewById<MaterialButtonToggleGroup>(R.id.tgClockDurationDialog)
        val tgTheme = view.findViewById<MaterialButtonToggleGroup>(R.id.tgThemeDialog)

        val switchRandomColor = view.findViewById<MaterialSwitch>(R.id.switchRandomColorDialog)
        val switchAutoRotate = view.findViewById<MaterialSwitch>(R.id.switchAutoRotateDialog)
        val switchManualFlip = view.findViewById<MaterialSwitch>(R.id.switchManualFlipDialog)
        val btnClose = view.findViewById<Button>(R.id.btnCloseSettings)

        val mm = MovementManager.INSTANCE
        tgWhite.check(if (mm.whiteAi) R.id.btnWhiteAiDialog else R.id.btnWhiteHumanDialog)
        tgBlack.check(if (mm.BlackAi) R.id.btnBlackAiDialog else R.id.btnBlackHumanDialog)

        val timeBtn = when (MovesGenerator.TIME_LIMIT_MS) {
            1000L -> R.id.btnTime1sDialog
            2000L -> R.id.btnTime2sDialog
            8000L -> R.id.btnTime8sDialog
            else -> R.id.btnTime4sDialog
        }
        tgTime.check(timeBtn)

        val clockBtn = when (clockDurationSeconds) {
            180 -> R.id.btnClock3m
            300 -> R.id.btnClock5m
            600 -> R.id.btnClock10m
            else -> R.id.btnClockNone
        }
        tgClock.check(clockBtn)

        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        val themeBtn = when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.id.btnThemeLight
            AppCompatDelegate.MODE_NIGHT_YES -> R.id.btnThemeDark
            else -> R.id.btnThemeSystem
        }
        tgTheme.check(themeBtn)

        switchRandomColor.isChecked = mm.randomStartingColor
        switchAutoRotate.isChecked = chessBoardView.autoRotatePiecesIn1v1
        switchManualFlip.isChecked = chessBoardView.isPiecesFlippedManual

        // Instant reset helper when setting changes
        val triggerInstantReset = {
            aiJob?.cancel()
            chessBoardView.resetGame()
            resetClocks()
            updateGameStatusUI()
            checkAndTriggerAiMove()
        }

        tgWhite.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                mm.whiteAi = (checkedId == R.id.btnWhiteAiDialog)
                triggerInstantReset()
            }
        }

        tgBlack.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                mm.BlackAi = (checkedId == R.id.btnBlackAiDialog)
                triggerInstantReset()
            }
        }

        tgTime.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                MovesGenerator.TIME_LIMIT_MS = when (checkedId) {
                    R.id.btnTime1sDialog -> 1000L
                    R.id.btnTime2sDialog -> 2000L
                    R.id.btnTime4sDialog -> 4000L
                    R.id.btnTime8sDialog -> 8000L
                    else -> 4000L
                }
            }
        }

        tgClock.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                clockDurationSeconds = when (checkedId) {
                    R.id.btnClock3m -> 180
                    R.id.btnClock5m -> 300
                    R.id.btnClock10m -> 600
                    else -> 0
                }
                resetClocks()
            }
        }

        tgTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.btnThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.btnThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        switchRandomColor.setOnCheckedChangeListener { _, isChecked ->
            mm.randomStartingColor = isChecked
            triggerInstantReset()
        }

        switchAutoRotate.setOnCheckedChangeListener { _, isChecked ->
            chessBoardView.autoRotatePiecesIn1v1 = isChecked
            chessBoardView.updateSnapshotFromEngine()
        }

        switchManualFlip.setOnCheckedChangeListener { _, isChecked ->
            chessBoardView.isPiecesFlippedManual = isChecked
            chessBoardView.updateSnapshotFromEngine()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateGameStatusUI() {
        val mm = MovementManager.INSTANCE

        val turnStr = when {
            mm.gameFinished -> {
                when (mm.won) {
                    1 -> "White Wins!"
                    2 -> "Black Wins!"
                    else -> "Game Over (Draw)"
                }
            }
            mm.turn -> "White to Move"
            else -> "Black to Move"
        }

        val checkStr = when {
            mm.gameFinished -> ""
            mm.whiteIsInCheck -> " (Check!)"
            mm.blackIsInCheck -> " (Check!)"
            else -> ""
        }

        tvStatus.text = "$turnStr$checkStr"

        val eval = try { EvaluationFuncion.Evaluate() } catch (e: Exception) { 0 }
        val depth = MovesGenerator.lastCompletedDepth
        tvEval.text = "Eval: $eval | Depth: $depth"
    }

    private fun checkAndTriggerAiMove() {
        val mm = MovementManager.INSTANCE
        if (mm.gameFinished) {
            pbThinking.visibility = View.INVISIBLE
            return
        }

        val isAiTurn = (mm.turn && mm.whiteAi) || (!mm.turn && mm.BlackAi)

        if (isAiTurn) {
            if (aiJob?.isActive == true) return // already calculating

            pbThinking.visibility = View.VISIBLE
            aiJob = lifecycleScope.launch(Dispatchers.Default) {
                val bestMove = try {
                    MovesGenerator.getBestMove()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                withContext(Dispatchers.Main) {
                    pbThinking.visibility = View.INVISIBLE
                    if (bestMove != null && !mm.gameFinished) {
                        MovesGenerator.Move(bestMove)
                    }
                    chessBoardView.updateSnapshotFromEngine()
                    updateGameStatusUI()

                    val nextAiTurn = (mm.turn && mm.whiteAi) || (!mm.turn && mm.BlackAi)
                    if (nextAiTurn && !mm.gameFinished) {
                        checkAndTriggerAiMove()
                    }
                }
            }
        } else {
            pbThinking.visibility = View.INVISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        aiJob?.cancel()
        clockJob?.cancel()
    }
}
