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
import kotlin.math.floor

/**
 * Draws the board from an immutable [GameState] and reports taps.
 * It never touches the engine directly: all engine work happens in [GameController] on its own thread.
 */
class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var state: GameState? = null

    /** Called with the engine square index (0..63) the user tapped. */
    var onSquareTapped: ((Int) -> Unit)? = null

    // Auto-rotate pieces in 1v1 mode when turn changes
    var autoRotatePiecesIn1v1: Boolean = true
        set(value) { field = value; invalidate() }

    // Manual flip pieces 180°
    var isPiecesFlippedManual: Boolean = false
        set(value) { field = value; invalidate() }

    // Color definitions
    private val lightTileColor = Color.rgb(235, 236, 208)
    private val darkTileColor = Color.rgb(119, 149, 86)

    private val moveHighlightColor = Color.rgb(185, 202, 67)
    private val checkColor = Color.argb(140, 220, 40, 40)
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

    init {
        loadRawPieceBitmaps()
    }

    fun render(newState: GameState) {
        state = newState
        invalidate()
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, width)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        tileSize = w.toFloat() / 8f

        scaledBitmaps.clear()
        val sizeInt = tileSize.toInt().coerceAtLeast(1)
        for ((key, raw) in rawBitmaps) {
            scaledBitmaps[key] = Bitmap.createScaledBitmap(raw, sizeInt, sizeInt, true)
        }
    }

    private val shouldRotateBoard180: Boolean
        get() {
            val s = state ?: return isPiecesFlippedManual
            val is1v1 = !s.whiteAi && !s.blackAi
            val playingAsBlack = s.whiteAi && !s.blackAi
            if (playingAsBlack) return true
            if (is1v1 && autoRotatePiecesIn1v1) return !s.turn
            return isPiecesFlippedManual
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = state

        canvas.save()
        val rotateBoard = shouldRotateBoard180
        if (rotateBoard) {
            canvas.rotate(180f, tileSize * 4f, tileSize * 4f)
        }

        // King in check gets a red square
        var checkMask = 0L
        if (s != null) {
            for (i in 0 until 64) {
                val p = s.pieces[i]
                if ((p == 6 && s.whiteInCheck) || (p == 12 && s.blackInCheck)) checkMask = checkMask or (1L shl i)
            }
        }

        // 1. Board, highlights, move dots
        for (c in 0 until 8) {
            for (r in 0 until 8) {
                val arrayPos = 63 - (c * 8 + r)

                val isLight = (c + r) % 2 == 0
                tilePaint.color = if (isLight) lightTileColor else darkTileColor
                if (s != null && (s.lastMove ushr arrayPos and 1L) != 0L) {
                    tilePaint.color = moveHighlightColor
                }

                val left = r * tileSize
                val top = c * tileSize
                tileRect.set(left, top, left + tileSize, top + tileSize)
                canvas.drawRect(tileRect, tilePaint)

                if ((checkMask ushr arrayPos and 1L) != 0L) {
                    overlayPaint.color = checkColor
                    canvas.drawRect(tileRect, overlayPaint)
                }

                if (s == null) continue
                if ((s.attackSquares ushr arrayPos and 1L) != 0L) {
                    overlayPaint.color = attackDotColor
                    canvas.drawCircle(left + tileSize / 2f, top + tileSize / 2f, tileSize * 0.25f, overlayPaint)
                }
                if ((s.captureSquares ushr arrayPos and 1L) != 0L) {
                    overlayPaint.color = captureRingColor
                    canvas.drawCircle(left + tileSize / 2f, top + tileSize / 2f, tileSize * 0.4f, overlayPaint)
                }
            }
        }

        // 2. Pieces (kept upright when the board is rotated). Square i is drawn at column 7 - i%8, row 7 - i/8.
        if (s != null) {
            for (i in 0 until 64) {
                val pieceType = s.pieces[i]
                if (pieceType == 0) continue
                val bmp = scaledBitmaps[pieceType] ?: continue
                val drawX = (7 - i % 8) * tileSize
                val drawY = (7 - i / 8) * tileSize
                if (rotateBoard) {
                    canvas.save()
                    canvas.rotate(180f, drawX + tileSize / 2f, drawY + tileSize / 2f)
                    canvas.drawBitmap(bmp, drawX, drawY, piecePaint)
                    canvas.restore()
                } else {
                    canvas.drawBitmap(bmp, drawX, drawY, piecePaint)
                }
            }
        }

        canvas.restore()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event)
        performClick()

        val s = state ?: return true
        // Quick UI-side filter; GameController checks again on the engine thread.
        if (!s.ready || s.gameFinished || s.aiThinking || s.isAiTurn || s.promotionPending) return true
        if (tileSize <= 0f) return true

        val x = event.x
        val y = event.y
        if (x < 0 || y < 0 || x > 8 * tileSize || y > 8 * tileSize) return true

        var col = floor(x / tileSize).toInt().coerceIn(0, 7)
        var row = floor(y / tileSize).toInt().coerceIn(0, 7)
        if (shouldRotateBoard180) {
            col = 7 - col
            row = 7 - row
        }

        onSquareTapped?.invoke(63 - (row * 8 + col))
        return true
    }
}
