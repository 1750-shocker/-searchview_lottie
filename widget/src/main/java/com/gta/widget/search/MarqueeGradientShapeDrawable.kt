package com.gta.widget.search

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.renderscript.RSRuntimeException
import androidx.annotation.FloatRange
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


internal class MarqueeGradientShapeDrawable(
    private val context: Context,
    shapeModel: ShapeAppearanceModel,
    private val segmentPx: Float = 2f, // sampling precision along the path
    lutSize: Int = 512 // requested LUT resolution
) : MaterialShapeDrawable(shapeModel) {

    enum class Direction { CLOCKWISE, COUNTERCLOCKWISE }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap  = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var waveBlur: BlurHelper? = null
    private var useBlur = true

    private var gradientRaw   = intArrayOf(Color.RED, Color.GREEN, Color.BLUE)
    private lateinit var gradientCyclic: IntArray

    private var lutSizePow2   = nextPowerOfTwo(max(lutSize, 4))
    private var lutMask       = lutSizePow2 - 1
    private var colorLut      = IntArray(lutSizePow2)
    private var lutDirty      = true

    private var strokeProgress  = 0f // [0‥1)
    private var direction = Direction.COUNTERCLOCKWISE

    private var mPath = Path()
    private var segments: List<Seg> = emptyList()
    private var totalLen: Float = 0f

    // bottom wave
    private var waveProgress  = 0f // [0‥1)
    private val waveBitmapWidth = 800
    private val waveBitmapHeight = 800
    private var waveBitmap: Bitmap? = null
    private var waveDstRect = Rect()
    private var waveDstBaseOffsetX = 0

    // cached values
    private var boundWidth = bounds.width()
    private var waveDestWidth = waveDstRect.width()
    private var visibleWaveWidth = waveDstRect.width() * 0.67f

    init {
        setGradientColors(gradientRaw.toList())
        wavePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    override fun setStrokeWidth(strokeWidth: Float) {
        if (strokeWidth != strokePaint.strokeWidth) {
            strokePaint.strokeWidth = strokeWidth
            rebuildSegments()
        }
        super.setStrokeWidth(strokeWidth)
    }

    fun setGradientColors(colors: List<Int>) {
        require(colors.size >= 2) { "Need at least two colors." }
        gradientRaw = colors.toIntArray()

        // append first colour to close the ring
        gradientCyclic = IntArray(colors.size + 1) { i ->
            if (i < colors.size) colors[i] else colors[0]
        }
        lutDirty = true
        invalidateSelf()
    }

    fun setStrokeGradientProgress(@FloatRange(from = 0.0, to = 1.0) p: Float) {
        val v = p.mod(1f)
        if (v != strokeProgress) {
            strokeProgress = v
        }
    }

    fun setWaveGradientProgress(@FloatRange(from = 0.0, to = 1.0) p: Float) {
        val v = p.mod(1f)
        if (v != waveProgress) {
            waveProgress = v
        }
    }

    fun setDirection(dir: Direction) {
        if (dir != direction) {
            direction = dir
            invalidateSelf()
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        rebuildSegments()

        val height = bounds.height()
        val xOffset = -(waveBitmapWidth * 0.25).toInt()
        val yOffset = -(waveBitmapHeight * 0.206).toInt()

        waveDstBaseOffsetX = xOffset
        waveDstRect = Rect(
            0 + xOffset,
            height + yOffset,
            0 + xOffset + waveBitmapWidth + waveBitmapWidth / 2,
            waveBitmapHeight + height + yOffset
        )

        boundWidth = bounds.width()
        waveDestWidth = waveDstRect.width()
        visibleWaveWidth = waveDestWidth * 0.67f
    }

    override fun setAlpha(alpha: Int) {
        super.setAlpha(alpha)
        strokePaint.alpha = alpha
        wavePaint.alpha = alpha
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (strokePaint.strokeWidth <= 0f || segments.isEmpty()) return
        ensureLutReady()

        // 保存当前 alpha 以便在绘制时使用
        val currentAlpha = alpha

        val shift = if (direction == Direction.CLOCKWISE) strokeProgress else 1f - strokeProgress
        val lutSizeF = lutSizePow2.toFloat()

        for (seg in segments) {
            val idx = (((seg.t0 + shift) * lutSizeF).roundToInt() and lutMask)
            strokePaint.color = applyAlpha(colorLut[idx], currentAlpha)
            canvas.drawLine(seg.x1, seg.y1, seg.x2, seg.y2, strokePaint)
        }

        canvas.withClip(mPath) {
            // 应用 alpha 值到波浪绘制
            wavePaint.alpha = currentAlpha
            val currentWaveBitmap = waveBitmap ?: return@withClip

            canvas.drawBitmap(
                currentWaveBitmap,
                null,
                waveDstRect.applyProgressOffset(boundWidth, waveDestWidth, visibleWaveWidth, waveProgress),
                wavePaint
            )
            canvas.drawBitmap(
                currentWaveBitmap,
                null,
                waveDstRect.applyProgressOffset(boundWidth, waveDestWidth, visibleWaveWidth, waveProgress + 0.33f),
                wavePaint.apply {
                    colorFilter
                }
            )
            canvas.drawBitmap(
                currentWaveBitmap,
                null,
                waveDstRect.applyProgressOffset(boundWidth, waveDestWidth, visibleWaveWidth, waveProgress + 0.67f),
                wavePaint
            )
        }
    }

    fun initBlur() {
        waveBlur = BlurHelper()
        waveBitmap = createWaveBitmap()
    }

    fun release() {
        val currWaveBitmap = waveBitmap
        waveBitmap = null
        currWaveBitmap?.recycle()
        try {
            val currWaveBlur = waveBlur
            waveBlur = null
            currWaveBlur?.release()
        } catch (ex: RSRuntimeException) {
            // Ignore, already released
        }
    }

    private fun rebuildSegments() {
        if (bounds.isEmpty) { segments = emptyList(); return }

        val inset = strokePaint.strokeWidth / 2f
        val pathWidth  = max(0f, bounds.width()  - strokePaint.strokeWidth)
        val pathHeight = max(0f, bounds.height() - strokePaint.strokeWidth)

        val path = Path()
        getPathForSize(pathWidth.toInt(), pathHeight.toInt(), path) // fit shape to new size
        path.offset(inset, inset)
        mPath = path

        val pm = PathMeasure(path, true)
        totalLen = pm.length
        if (totalLen == 0f) { segments = emptyList(); return }

        val segs = ArrayList<Seg>()
        val pos  = FloatArray(2)
        val prev = FloatArray(2)
        pm.getPosTan(0f, prev, null)

        var accLen = 0f
        var d = 0f
        while (d < totalLen) {
            d += segmentPx
            pm.getPosTan(min(d, totalLen), pos, null)
            val dx = pos[0] - prev[0]
            val dy = pos[1] - prev[1]
            val len = hypot(dx, dy)
            segs += Seg(prev[0], prev[1], pos[0], pos[1], len, accLen / totalLen)
            accLen += len
            prev[0] = pos[0]; prev[1] = pos[1]
        }
        segments = segs
    }

    private fun createWaveBitmap(): Bitmap {
        val w = waveBitmapWidth
        val h = waveBitmapHeight
        val srcWaveBitmap = createBitmap(w, h)
        val dstWaveBitmap = createBitmap(w, h)

        val srcCanvas = Canvas(srcWaveBitmap)
        val centerX  = w / 2f
        val centerY  = h / 2f
        val radius   = hypot(centerX / 2, centerY / 2)

        val shader = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(0xFF0F9BFF.toInt(), 0x0F8018FF), // todo: customize wave colors
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }

        srcCanvas.drawCircle(w / 2f,  h / 2f, radius, p)

        try {
            val currentWaveBlur = waveBlur

            if (useBlur && currentWaveBlur != null && currentWaveBlur.prepare(context, srcWaveBitmap, 25f)) {
                currentWaveBlur.blur(srcWaveBitmap, dstWaveBitmap)
            } else {
                val dstCanvas = Canvas(dstWaveBitmap)
                dstCanvas.drawBitmap(srcWaveBitmap, 0f, 0f, null)
            }
        } catch (ex: RSRuntimeException) {
            useBlur = false
            val dstCanvas = Canvas(dstWaveBitmap)
            dstCanvas.drawBitmap(srcWaveBitmap, 0f, 0f, null)
        }


        srcWaveBitmap.recycle()
        return dstWaveBitmap
    }

    private fun ensureLutReady() { if (lutDirty) { buildColorLut(); lutDirty = false } }

    private fun buildColorLut() {
        val nSeg = gradientCyclic.size - 1 // N-1 gradient segments
        val step = 1f / lutSizePow2
        var t = 0f
        for (i in 0 until lutSizePow2) {
            val scaled = t * nSeg
            val seg    = scaled.toInt()
            val f      = scaled - seg
            colorLut[i] = blend(
                gradientCyclic[seg],
                gradientCyclic[seg + 1],
                f
            )
            t += step
        }
        // Loop closure – guarantee exact first colour at end
        colorLut[lutSizePow2 - 1] = gradientCyclic[0]
    }

    private fun Rect.applyProgressOffset(
        boundWidth: Int,
        waveDestWidth: Int,
        visibleWaveWidth: Float = width() * 0.67f,
        progress: Float
    ): Rect {
        val offset = (boundWidth + visibleWaveWidth) * (progress % 1f) - visibleWaveWidth

        left = waveDstBaseOffsetX + offset.toInt()
        right = left + waveDestWidth

        return this
    }

    private fun blend(c1: Int, c2: Int, f: Float): Int {
        val i   = (f.coerceIn(0f, 1f) * 255 + 0.5f).toInt()
        val inv = 255 - i
        val a = (Color.alpha(c1) * inv + Color.alpha(c2) * i) / 255
        val r = (Color.red  (c1) * inv + Color.red  (c2) * i) / 255
        val g = (Color.green(c1) * inv + Color.green(c2) * i) / 255
        val b = (Color.blue (c1) * inv + Color.blue (c2) * i) / 255
        return Color.argb(a, r, g, b)
    }

    private fun nextPowerOfTwo(v: Int): Int {
        var x = v - 1
        x = x or (x shr 1)
        x = x or (x shr 2)
        x = x or (x shr 4)
        x = x or (x shr 8)
        x = x or (x shr 16)
        return x + 1
    }

    private fun applyAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            (Color.alpha(color) * alpha) / 255,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private data class Seg(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val len: Float,
        val t0: Float // pre-computed phase start (0‥1)
    )
}