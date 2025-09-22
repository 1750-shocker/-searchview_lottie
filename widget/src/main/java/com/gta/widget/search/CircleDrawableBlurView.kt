package com.gta.widget.search

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.createBitmap
import kotlin.math.max


internal class CircleDrawableBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var blurRadius = 25f
    private var overlayColor = Color.argb(128, 0, 0, 0) // 0.5f * 255 = 128

    private var mDrawable: Drawable? = null
    private var frameDirty = false
    private var configDirty = false

    private var bmpToBlur: Bitmap? = null
    private var bmpBlurred: Bitmap? = null
    private var blurringCanvas: Canvas? = null

    private val paint = Paint()
    private val rectSrc = Rect()
    private val rectDst = Rect()
    private var clipPath = Path()

    private var rsHelper: BlurHelper? = null

    private var released = false

    private val mDrawableCallback = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) {
            frameDirty = true
            invalidate()
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            handler.postAtTime(what, who, `when`)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            handler.removeCallbacks(what, who)
        }
    }

    fun setBlurDrawable(drawable: Drawable?) {
        mDrawable?.callback = null

        mDrawable = drawable?.also {
            it.callback = mDrawableCallback
            it.invalidateSelf()
        }

        frameDirty = true
        invalidate()
    }

    fun setBlurRadius(radius: Float) {
        require(radius >= 0) { "Blur radius must be >= 0" }
        blurRadius = radius
        configDirty = true
        invalidate()
    }

    fun setBlurOverlayColor(color: Int) {
        overlayColor = color
        configDirty = true
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initBlur()
        rebindCallbackIfNeeded()
        frameDirty = true
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mDrawable?.callback = null
        release()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            rebindCallbackIfNeeded()
            frameDirty = true
            invalidate()
        } else { // INVISIBLE 或 GONE
            mDrawable?.callback = null
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        clipPath = Path().apply {
            addCircle(w / 2f, h / 2f, max(w, h) / 2f, Path.Direction.CW)
        }
        releaseBitmaps()
        frameDirty = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (released) return
        if (frameDirty || configDirty) {
            captureAndBlur()
            frameDirty = false
            configDirty = false
        }

        drawBlurredBitmap(canvas)
    }

    private fun rebindCallbackIfNeeded() {
        mDrawable?.let {
            it.callback = mDrawableCallback
            it.invalidateSelf()
        }
    }

    private fun captureAndBlur() {
        val src = mDrawable ?: return
        if (!prepareBitmaps()) return

        bmpToBlur?.eraseColor(overlayColor and 0xffffff)
        blurringCanvas?.let { c ->
            src.setBounds(0, 0, c.width, c.height)
            src.draw(c)
        }

        rsHelper?.blur(bmpToBlur, bmpBlurred)
    }

    private fun drawBlurredBitmap(canvas: Canvas) {
        canvas.clipPath(clipPath)
        bmpBlurred?.let { bmp ->
            rectSrc.set(0, 0, bmp.width, bmp.height)
            rectDst.set(0, 0, width, height)
            canvas.drawBitmap(bmp, rectSrc, rectDst, null)
        }
        paint.color = overlayColor
        canvas.drawRect(rectDst, paint)
    }

    private fun prepareBitmaps(): Boolean {
        val w = max(width, 1)
        val h = max(height, 1)

        val currentRsHelper = rsHelper ?: return false

        if (blurringCanvas == null ||
            bmpBlurred?.width != w ||
            bmpBlurred?.height != h
        ) {
            releaseBitmaps()
            try {
                bmpToBlur = createBitmap(w, h)
                bmpBlurred = createBitmap(w, h)
                blurringCanvas = Canvas(bmpToBlur!!)
            } catch (_: OutOfMemoryError) {
                release()
                return false
            }
            currentRsHelper.prepare(context, bmpToBlur, blurRadius.coerceAtMost(25f))
        }

        if (configDirty) {
            currentRsHelper.prepare(context, bmpToBlur, blurRadius.coerceAtMost(25f))
        }
        return true
    }

    private fun releaseBitmaps() {
        val currentBmpToBlur = bmpToBlur
        bmpToBlur = null
        currentBmpToBlur?.recycle()

        val currentBmpBlurred = bmpBlurred
        bmpBlurred = null
        currentBmpBlurred?.recycle()

        blurringCanvas = null
    }

    private fun initBlur() {
        if (rsHelper == null) {
            rsHelper = BlurHelper()
        }
    }

    private fun release() {
        releaseBitmaps()
        val currentRsHelper = rsHelper
        rsHelper = null
        currentRsHelper?.release()
    }
}