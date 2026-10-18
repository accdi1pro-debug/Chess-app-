package com.example.chessapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.chesslib.Board
import com.example.chesslib.MagicBitBoards
import com.example.chesslib.MovementManager
import kotlin.math.floor

class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Thread-safe local snapshot of board state.
     * Prevents board flickering and ANR during AI calculation.
     */
    class BoardSnapshot {
        val pieces = IntArray(64)
        var checkSquares: Long = 0L
        var kingCheckSquares: Long = 0L
        var moveHighlights: Long = 0L
        var attackSquares: Long = 0L
        var captureSquares: Long = 0L
        var turn: Boolean = true
        var whitePromotionUI: Boolean = false
        var blackPromotionUI: Boolean = false
        var gameFinished: Boolean = false
        var whiteAi: Boolean = false
        var blackAi: Boolean = true
    }

    private val snapshot = BoardSnapshot()

    var onStateChangedListener: (() -> Unit)? = null

    // Auto-rotate pieces in 1v1 mode when turn changes
    var autoRotatePiecesIn1v1: Boolean = true

    // Manual flip pieces 180°
    var isPiecesFlippedManual: Boolean = false

    // Color definitions
    private val lightTileColor = Color.rgb(235, 236, 208)
    private val darkTileColor = Color.rgb(119, 149, 86)
    private val checkColor = Color.argb(255 ,255, 255, 0)
    private val kingCheckColor = Color.argb(255 ,255, 255, 0)
    private val moveHighlightColor = Color.rgb(185, 202, 67)
    private val attackDotColor = Color.argb(40, 0, 0, 0)
    private val captureRingColor = Color.argb(50, 0, 0, 0)

    // Reusable Paints & Rects to optimize rendering
    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val piecePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tileRect = RectF()

    // Original Bitmaps
    private val rawBitmaps = HashMap<Int, Bitmap>()
    // Scaled Bitmaps cached for current tile size
    private val scaledBitmaps = HashMap<Int, Bitmap>()

    private var tileSize: Float = 0f
    private var boardOffsetX: Float = 0f
    private var boardOffsetY: Float = 0f

    init {
        initEngineIfNeeded()
        loadRawPieceBitmaps()
        updateSnapshotFromEngine()
    }

    private fun initEngineIfNeeded() {
        try {
            MagicBitBoards.init()
            MovementManager.INSTANCE.updateWhiteBoard()
            MovementManager.INSTANCE.updateBlackBoard()
            MovementManager.INSTANCE.updateBoard()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadRawPieceBitmaps() {
        val res = context.resources
        rawBitmaps[1] = BitmapFactory.decodeResource(res, R.drawable.wp)
        rawBitmaps[2] = BitmapFactory.decodeResource(res, R.drawable.wb)
        rawBitmaps[3] = BitmapFactory.decodeResource(res, R.drawable.wr)
        rawBitmaps[4] = BitmapFactory.decodeResource(res, R.drawable.wq)
        rawBitmaps[5] = BitmapFactory.decodeResource(res, R.drawable.wkn)
        rawBitmaps[6] = BitmapFactory.decodeResource(res, R.drawable.wk)

        rawBitmaps[7] = BitmapFactory.decodeResource(res, R.drawable.bp)
        rawBitmaps[8] = BitmapFactory.decodeResource(res, R.drawable.bb)
        rawBitmaps[9] = BitmapFactory.decodeResource(res, R.drawable.br)
        rawBitmaps[10] = BitmapFactory.decodeResource(res, R.drawable.bq)
        rawBitmaps[11] = BitmapFactory.decodeResource(res, R.drawable.bkh)
        rawBitmaps[12] = BitmapFactory.decodeResource(res, R.drawable.bk)
    }

    fun updateSnapshotFromEngine() {
        val mm = MovementManager.INSTANCE
        for (i in 0 until 64) {
            snapshot.pieces[i] = mm.returnPiece(i)
        }
        snapshot.checkSquares = Board.checkSquares
        snapshot.kingCheckSquares = Board.kingCheckSquares
        snapshot.moveHighlights = Board.move
        snapshot.attackSquares = Board.atackSquares
        snapshot.captureSquares = Board.captureSquares
        snapshot.turn = mm.turn
        snapshot.whitePromotionUI = mm.whitePromotionUI
        snapshot.blackPromotionUI = mm.blackPromotionUI
        snapshot.gameFinished = mm.gameFinished
        snapshot.whiteAi = mm.whiteAi
        snapshot.blackAi = mm.BlackAi
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, width)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val boardDim = w.toFloat()
        tileSize = boardDim / 8f
        boardOffsetX = 0f
        boardOffsetY = -0.05f

        scaledBitmaps.clear()
        val sizeInt = tileSize.toInt().coerceAtLeast(1)
        for ((key, raw) in rawBitmaps) {
            scaledBitmaps[key] = Bitmap.createScaledBitmap(raw, sizeInt, sizeInt, true)
        }
    }

    val is1v1Mode: Boolean
        get() = !snapshot.whiteAi && !snapshot.blackAi

    val isPlayingAsBlack: Boolean
        get() = snapshot.whiteAi && !snapshot.blackAi

    val shouldRotateBoard180: Boolean
        get() {
            if (isPlayingAsBlack) return true
            if (is1v1Mode && autoRotatePiecesIn1v1) {
                return !snapshot.turn
            }
            return isPiecesFlippedManual
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.translate(boardOffsetX, boardOffsetY)

        val rotateBoard = shouldRotateBoard180
        if (rotateBoard) {
            canvas.rotate(180f, tileSize * 4f, tileSize * 4f)
        }

        // 1. Draw Board Grid & Tile Highlights
        for (c in 0 until 8) {
            for (r in 0 until 8) {
                val arrayPos = 63 - (c * 8 + r)

                val isLight = (c + r) % 2 == 0
                tilePaint.color = if (isLight) lightTileColor else darkTileColor

                if ((snapshot.checkSquares ushr arrayPos and 1L) != 0L) {
                    tilePaint.color = checkColor
                }
                if ((snapshot.kingCheckSquares ushr arrayPos and 1L) != 0L) {
                    tilePaint.color = kingCheckColor
                }
                if ((snapshot.moveHighlights ushr arrayPos and 1L) != 0L) {
                    tilePaint.color = moveHighlightColor
                }

                val left = r * tileSize
                val top = c * tileSize
                tileRect.set(left, top, left + tileSize, top + tileSize)
                canvas.drawRect(tileRect, tilePaint)

                if ((snapshot.attackSquares ushr arrayPos and 1L) != 0L) {
                    overlayPaint.color = attackDotColor
                    canvas.drawCircle(left + tileSize / 2f, top + tileSize / 2f, tileSize * 0.25f, overlayPaint)
                }
                if ((snapshot.captureSquares ushr arrayPos and 1L) != 0L) {
                    overlayPaint.color = captureRingColor
                    canvas.drawCircle(left + tileSize / 2f, top + tileSize / 2f, tileSize * 0.4f, overlayPaint)
                }
            }
        }

        // 2. Draw Pieces
        var count = 0
        var xIndex = 7
        var yIndex = 7

        for (i in 0 until 64) {
            val drawX = xIndex * tileSize
            val drawY = yIndex * tileSize

            val pieceType = snapshot.pieces[i]
            if (pieceType != 0) {
                scaledBitmaps[pieceType]?.let { bmp ->
                    canvas.drawBitmap(bmp, drawX, drawY, piecePaint)
                }
            }

            xIndex--
            if (count == 7) {
                yIndex--
                xIndex = 7
                count = 0
            } else {
                count++
            }
        }

        // 3. Draw Promotion Overlay if active
        if (snapshot.blackPromotionUI) {
            drawPromotionRow(canvas, isWhite = false)
        }
        if (snapshot.whitePromotionUI) {
            drawPromotionRow(canvas, isWhite = true)
        }

        canvas.restore()
    }

    private fun drawPromotionRow(canvas: Canvas, isWhite: Boolean) {
        val y = if (isWhite) 7 * tileSize else 0f
        val pieces = if (isWhite) intArrayOf(5, 3, 4, 2) else intArrayOf(11, 9, 10, 8)
        val startCol = 2

        overlayPaint.color = Color.argb(220, 255, 255, 255)
        canvas.drawRect(startCol * tileSize, y, (startCol + 4) * tileSize, y + tileSize, overlayPaint)

        for (idx in pieces.indices) {
            val pieceId = pieces[idx]
            scaledBitmaps[pieceId]?.let { bmp ->
                canvas.drawBitmap(bmp, (startCol + idx) * tileSize, y, piecePaint)
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event)
        performClick()

        val mm = MovementManager.INSTANCE
        if (mm.gameFinished) return true

        val isAiTurn = (mm.turn && mm.whiteAi) || (!mm.turn && mm.BlackAi)
        if (isAiTurn) return true

        val touchX = event.x - boardOffsetX
        val touchY = event.y - boardOffsetY

        if (touchX < 0 || touchY < 0 || touchX > 8 * tileSize || touchY > 8 * tileSize) {
            return true
        }

        var col = floor(touchX / tileSize).toInt().coerceIn(0, 7)
        var row = floor(touchY / tileSize).toInt().coerceIn(0, 7)

        if (shouldRotateBoard180) {
            col = 7 - col
            row = 7 - row
        }

        val arrayPos = 63 - (row * 8 + col)

        if (mm.whitePromotionUI || mm.blackPromotionUI) {
            val promoRow = if (mm.whitePromotionUI) 7 else 0
            if (row == promoRow && col in 2..5) {
                mm.promotionPiece = when (col) {
                    5 -> 1
                    4 -> 2
                    3 -> 3
                    2 -> 4
                    else -> 2
                }
                if (mm.whitePromotionUI) {
                    mm.whitePromotion(mm.promotionsquare)
                } else {
                    mm.blackPromotion(mm.promotionsquare)
                }
                updateSnapshotFromEngine()
                onStateChangedListener?.invoke()
            }
            return true
        }

        if ((Board.atackSquares ushr arrayPos and 1L) != 0L) {
            mm.movePiece(arrayPos)
        } else {
            val piece = mm.returnPiece(arrayPos)
            mm.showAtackSquare(piece, arrayPos)
        }

        updateSnapshotFromEngine()
        onStateChangedListener?.invoke()
        return true
    }

    fun resetGame() {
        val mm = MovementManager.INSTANCE
        mm.resetState()
        MagicBitBoards.init()
        mm.updateWhiteBoard()
        mm.updateBlackBoard()
        mm.updateBoard()
        Board.atackSquares = 0
        Board.captureSquares = 0
        Board.move = 0
        updateSnapshotFromEngine()
        onStateChangedListener?.invoke()
    }
}
