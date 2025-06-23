package com.gta.widget.search

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ImageDecoder
import android.graphics.PixelFormat
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textfield.TextInputLayout
import com.gta.widget.R
import com.gta.widget.ext.withStyledAttributes
import kotlin.math.abs
import kotlin.math.max


/**
 * 支持丰富动效的搜索框, 适用于语音搜索场景
 */
class MotionVoiceSearchBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.textInputStyle
) : LinearLayout(context, attrs) {
    private val mTextInputLayout: TextInputLayout
    private val mIconLayout: RelativeLayout

    private val mIconIdle: ImageView

    private val mIconActive: SearchLottieAnimationView
    private val mIconBackgroundBlurActive: CircleDrawableBlurView
    private val mActiveLayout: RelativeLayout

    private var mInitialized = false
    private var mAttached = true

    // style from attributes
    private val mMinHeight: Int
    private val mActiveIconHeight: Int
    private var mIconMarginStart: Int

    private var mBackgroundIdleColor: Int
    private var mBackgroundActiveColor: Int
    private var mStrokeIdleColor: Int
    private var mStrokeIdleWidth: Float
    private var mStrokeActiveWidth: Float
    private var mStrokeActiveColorA: Int
    private var mStrokeActiveColorB: Int
    private var mStrokeActiveColorC: Int
    private var mMarqueeAnimationDuration: Long
    private val mIconBackgroundActiveBlurOverlay: Float

    private val mShapeAppearanceModel: ShapeAppearanceModel

    private val mIconIdleDrawable: Drawable
    private val mIconActiveBackgroundDrawable: AnimatedImageDrawable
    private val mEndIconDrawable: Drawable?
    private val mEndIconColorTint: ColorStateList
    private val mEndIconColorAlpha: Int

    private val mBackgroundIdleDrawable: MaterialShapeDrawable
    private val mBackgroundActiveDrawable: MarqueeGradientShapeDrawable
    private val mBackgroundDrawable: StateTransitionDrawableDelegate

    // states
    private val mState: MutableLiveData<MotionState> = MutableLiveData()
    private val marqueeStrokeAnimator = ValueAnimator.ofFloat(0f, 1f)
    private val marqueeWaveAnimator = ValueAnimator.ofFloat(0f, 1f)

    private val mLinearInterpolator = LinearInterpolator(context, attrs)
    private val mIconVisibilityController = IconVisibilityController()

    private val mStateTransitionDuration: Long

    private val mMarqueeAnimationListener = ValueAnimator.AnimatorUpdateListener {
        if (!mAttached) return@AnimatorUpdateListener
        if (it === marqueeStrokeAnimator) {
            setMarqueeBorderProgress(it.animatedValue as Float)
        } else if (it === marqueeWaveAnimator) {
            setMarqueeWaveProgress(it.animatedValue as Float)
        }
    }

    private var mStateChangeListener: Observer<MotionState?>? = null

    init {
        val initialState: MotionState
        // SearchBarLayoutStyle implements TextInputLayout, so we can use TextInputLayout's attributes to style the search bar
        context.withStyledAttributes(attrs, com.google.android.material.R.styleable.TextInputLayout) {
            mMinHeight = getDimensionPixelSize(
                com.google.android.material.R.styleable.TextInputLayout_startIconMinSize,
                resources.getDimensionPixelSize(R.dimen.search_bar_min_height)
            )
            mBackgroundIdleColor = getColor(com.google.android.material.R.styleable.TextInputLayout_boxBackgroundColor, Color.TRANSPARENT)
            mStrokeIdleColor = getColor(com.google.android.material.R.styleable.TextInputLayout_boxStrokeColor, Color.TRANSPARENT)
            mStrokeIdleWidth = getDimension(
                com.google.android.material.R.styleable.TextInputLayout_boxStrokeWidth,
                resources.getDimension(R.dimen.search_bar_border_width)
            )
            mIconIdleDrawable = getDrawable(com.google.android.material.R.styleable.TextInputLayout_startIconDrawable)
                ?: AppCompatResources.getDrawable(context, R.drawable.ic_search)
                        ?: throw IllegalArgumentException("Start icon drawable must be provided")

            mShapeAppearanceModel = ShapeAppearanceModel.builder(
                context, attrs, defStyleAttr,
                com.google.android.material.R.style.Widget_Design_TextInputLayout
            ).build()
            mBackgroundIdleDrawable = MaterialShapeDrawable(mShapeAppearanceModel).apply {
                strokeColor = ColorStateList.valueOf(mStrokeIdleColor)
                fillColor = ColorStateList.valueOf(mBackgroundIdleColor)
                strokeWidth = mStrokeIdleWidth
            }

            mEndIconDrawable = getDrawable(com.google.android.material.R.styleable.TextInputLayout_endIconDrawable)
            mEndIconColorTint = getColorStateList(com.google.android.material.R.styleable.TextInputLayout_endIconTint)
                ?: ColorStateList.valueOf(resources.getColor(R.color.searchbar_voice_search_end_icon_tint, context.theme))
            mEndIconColorAlpha = (Color.valueOf(mEndIconColorTint.defaultColor).alpha() * 255f).toInt().coerceIn(0..255)
        }

        context.withStyledAttributes(attrs, R.styleable.MotionVoiceSearchBar) {
            initialState = getInt(R.styleable.MotionVoiceSearchBar_initialState, 0)
                .let { MotionState.entries.getOrElse(it) { MotionState.IDLE } }

            mActiveIconHeight = getDimensionPixelSize(R.styleable.MotionVoiceSearchBar_activeIconHeight, mMinHeight)
            mStrokeActiveWidth = getDimension(
                R.styleable.MotionVoiceSearchBar_boxActiveStrokeWidth,
                resources.getDimension(R.dimen.search_bar_border_active_width)
            )
            mStrokeActiveColorA = getColor(R.styleable.MotionVoiceSearchBar_activeBorderColorA, mStrokeIdleColor)
            mStrokeActiveColorB = getColor(R.styleable.MotionVoiceSearchBar_activeBorderColorB, mStrokeIdleColor)
            mStrokeActiveColorC = getColor(R.styleable.MotionVoiceSearchBar_activeBorderColorC, mStrokeIdleColor)
            mBackgroundActiveColor = getColor(R.styleable.MotionVoiceSearchBar_boxActiveBackgroundColor, Color.TRANSPARENT)

            mMarqueeAnimationDuration = getInt(R.styleable.MotionVoiceSearchBar_marqueeBorderAnimationDuration, 3000).toLong()

            mIconBackgroundActiveBlurOverlay = getFloat(R.styleable.MotionVoiceSearchBar_activeStartIconBlurRadius, 25f)
            mIconActiveBackgroundDrawable = (ImageDecoder.decodeDrawable(
                ImageDecoder.createSource(resources, R.drawable.searchbar_start_icon)
            ) as AnimatedImageDrawable)
                .apply { repeatCount = AnimatedImageDrawable.REPEAT_INFINITE }

            mBackgroundActiveDrawable = MarqueeGradientShapeDrawable(context, mShapeAppearanceModel).apply {
                strokeWidth = mStrokeActiveWidth
                fillColor = ColorStateList.valueOf(mBackgroundActiveColor)
                setGradientColors(listOf(mStrokeActiveColorA, mStrokeActiveColorB, mStrokeActiveColorC))
            }

            mStateTransitionDuration = getInt(R.styleable.MotionVoiceSearchBar_stateTransitionDuration, 300).toLong()
        }

        mBackgroundDrawable = StateTransitionDrawableDelegate()

        context.withStyledAttributes(attrs, R.styleable.TextInputLayout) {
            mIconMarginStart = getDimensionPixelSize(R.styleable.TextInputLayout_startIconPaddingStart, 0)
        }

        // initialize icon and text input layout
        mIconLayout = RelativeLayout(context).apply {
            layoutParams = LayoutParams(mActiveIconHeight, mActiveIconHeight).apply {
                setMargins(mIconMarginStart + (mMinHeight - mActiveIconHeight) / 2, 0, 0, 0)
            }
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = false // allow child views to overflow the bounds of this layout
        }
        // We apply attributes to TextInputLayout and override some styles which has already applied to our root layout
        mTextInputLayout = TextInputLayout(context, attrs, defStyleAttr).apply {
            id = R.id.searchbar_textinputlayout
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, mMinHeight).apply {
                weight = 1f // force inner TextInputLayout to fill the remaining space
            }
            // override default styles
            boxStrokeColor = Color.TRANSPARENT
            boxStrokeWidth = 0
            boxStrokeWidth = 0
            boxStrokeErrorColor = ColorStateList.valueOf(Color.TRANSPARENT)
            backgroundTintList = null
            boxBackgroundColor = Color.TRANSPARENT
            startIconDrawable = null
        }

        mIconIdle = AppCompatImageButton(context).apply {
            layoutParams = RelativeLayout.LayoutParams(mMinHeight, mMinHeight).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            setImageDrawable(mIconIdleDrawable)
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        }

        mIconActive = SearchLottieAnimationView(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            //backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        }

        mIconBackgroundBlurActive = CircleDrawableBlurView(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            setBlurDrawable(mIconActiveBackgroundDrawable)
            setBlurOverlayColor(Color.TRANSPARENT)
            setBlurRadius(mIconBackgroundActiveBlurOverlay)
        }

        mActiveLayout = RelativeLayout(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            addView(mIconBackgroundBlurActive)
            addView(mIconActive)
        }

        minimumHeight = mMinHeight
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = mBackgroundDrawable

        mIconLayout.addView(mIconIdle)
        mIconLayout.addView(mActiveLayout)
        addView(mIconLayout)
        addView(mTextInputLayout)
        mInitialized = true

        marqueeStrokeAnimator.apply {
            duration = mMarqueeAnimationDuration
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = mLinearInterpolator
        }
        marqueeWaveAnimator.apply {
            duration = (mMarqueeAnimationDuration * 1.67f).toLong()
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = mLinearInterpolator
        }

        mBackgroundDrawable.setActiveImmediate(initialState.ordinal >= MotionState.AWAITING_INPUT.ordinal)
        mIconVisibilityController.setActiveImmediate(initialState.ordinal >= MotionState.AWAITING_INPUT.ordinal)
        setState(initialState)
    }

    fun setState(state: MotionState) {
        val prevState = mState.value ?: MotionState.IDLE

        // background and icon state changes
        if (state.ordinal >= MotionState.AWAITING_INPUT.ordinal) {
            mIconActive.setState(state.toSearchIconState())
        }

        mBackgroundDrawable.setActive(state.ordinal >= MotionState.AWAITING_INPUT.ordinal)
        mIconVisibilityController.setActive(state.ordinal >= MotionState.AWAITING_INPUT.ordinal)

        // animation changes
        if (state.ordinal >= MotionState.RECORDING.ordinal) {
            if (prevState.ordinal < MotionState.RECORDING.ordinal) {
                startMarqueeAnimation()
                mIconActiveBackgroundDrawable.start()
            }
        } else {
            if (prevState.ordinal >= MotionState.RECORDING.ordinal) {
                stopMarqueeAnimation()
                mIconActiveBackgroundDrawable.stop()
            }
        }

        mState.value = state
    }

    fun getState(): MotionState {
        return mState.value ?: MotionState.IDLE
    }

    fun setStartIconMarginStart(margin: Int) {
        mIconMarginStart = margin
        mIconLayout.layoutParams = (mIconLayout.layoutParams as LayoutParams).apply {
            setMargins(mIconMarginStart + (mMinHeight - mActiveIconHeight) / 2, topMargin, rightMargin, bottomMargin)
        }
        invalidate()
    }

    fun setBackgroundColor(
        @ColorInt idleColor: Int = mBackgroundIdleColor,
        @ColorInt activeColor: Int = mBackgroundActiveColor
    ) {
        mBackgroundIdleColor = idleColor
        mBackgroundIdleDrawable.fillColor = ColorStateList.valueOf(idleColor)
        mBackgroundActiveColor = activeColor
        mBackgroundActiveDrawable.fillColor = ColorStateList.valueOf(activeColor)

        invalidate()
    }

    fun setIdleStroke(
        @ColorInt color: Int = mStrokeIdleColor,
        width: Float = mStrokeIdleWidth
    ) {
        mStrokeIdleColor = color
        mStrokeIdleWidth = width
        mBackgroundIdleDrawable.strokeColor = ColorStateList.valueOf(color)
        mBackgroundIdleDrawable.strokeWidth = width

        invalidate()
    }

    fun setActiveStroke(
        @ColorInt colorA: Int = mStrokeActiveColorA,
        @ColorInt colorB: Int = mStrokeActiveColorB,
        @ColorInt colorC: Int = mStrokeActiveColorC,
        width: Float = mStrokeActiveWidth
    ) {
        mStrokeActiveColorA = colorA
        mStrokeActiveColorB = colorB
        mStrokeActiveColorC = colorC
        mStrokeActiveWidth = width
        mBackgroundActiveDrawable.setGradientColors(listOf(colorA, colorB, colorC))
        mBackgroundActiveDrawable.strokeWidth = width

        invalidate()
    }

    fun setMarqueeAnimationDuration(duration: Long) {
        mMarqueeAnimationDuration = duration
        marqueeStrokeAnimator.duration = duration
        marqueeWaveAnimator.duration = (duration * 1.67).toLong()
    }

    fun setStartIconOnClickListener(listener: OnClickListener?) {
        mIconIdle.setOnClickListener(listener)
        mIconActive.setOnClickListener(listener)
    }

    fun setStartIconOnLongClickListener(listener: OnLongClickListener?) {
        mIconIdle.setOnLongClickListener(listener)
        mIconActive.setOnLongClickListener(listener)
    }

    fun setEndIconOnClickListener(listener: OnClickListener?) {
        mTextInputLayout.setEndIconOnClickListener(listener)
    }

    fun setEndIconOnLongClickListener(listener: OnLongClickListener?) {
        mTextInputLayout.setEndIconOnLongClickListener(listener)
    }

    fun setOnStateChangeListener(listener: OnStateChangeListener?) {
        if (listener == null) {
            mState.removeObserver(mStateChangeListener ?: return)
        } else {
            mStateChangeListener?.let { mState.removeObserver(it) }
            mState.observeForever(
                Observer<MotionState?> { if (it != null) listener.onStateChanged(it) }
                    .also { mStateChangeListener = it }
            )
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (!mInitialized || child === mIconLayout || child === mTextInputLayout) {
            super.addView(child, index, params)
        } else {
            // delegate to TextInputLayout for adding children
            mTextInputLayout.addView(child, params)
        }
    }

    override fun onAttachedToWindow() {
        mBackgroundActiveDrawable.initBlur()
        marqueeWaveAnimator.addUpdateListener(mMarqueeAnimationListener)
        marqueeStrokeAnimator.addUpdateListener(mMarqueeAnimationListener)
        mAttached = true
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        mAttached = false
        marqueeWaveAnimator.removeUpdateListener(mMarqueeAnimationListener)
        marqueeStrokeAnimator.removeUpdateListener(mMarqueeAnimationListener)
        mBackgroundActiveDrawable.release()
        super.onDetachedFromWindow()
    }

    internal fun setMarqueeBorderProgress(progress: Float) {
        mBackgroundActiveDrawable.setStrokeGradientProgress(progress)

        if (mState.value != MotionState.IDLE) {
            invalidate()
        }
    }

    internal fun setMarqueeWaveProgress(progress: Float) {
        mBackgroundActiveDrawable.setWaveGradientProgress(progress)

        if (mState.value != MotionState.IDLE) {
            invalidate()
        }
    }

    internal fun startMarqueeAnimation() {
        if (!marqueeStrokeAnimator.isRunning) marqueeStrokeAnimator.start()
        if (!marqueeWaveAnimator.isRunning) marqueeWaveAnimator.start()
    }

    internal fun stopMarqueeAnimation() {
        if (marqueeStrokeAnimator.isRunning) marqueeStrokeAnimator.cancel()
        if (marqueeWaveAnimator.isRunning) marqueeWaveAnimator.cancel()
    }

    enum class MotionState {
        IDLE, AWAITING_INPUT, RECORDING, PROCESSING;

        internal fun toSearchIconState(): SearchLottieAnimationView.SearchState {
            return when (this) {
                IDLE -> SearchLottieAnimationView.SearchState.IDLE
                AWAITING_INPUT -> SearchLottieAnimationView.SearchState.IDLE
                RECORDING -> SearchLottieAnimationView.SearchState.IDLE_TO_LISTENING
                PROCESSING -> SearchLottieAnimationView.SearchState.LISTENING_TO_LOADING
            }
        }
    }

    interface OnStateChangeListener {
        fun onStateChanged(state: MotionState)
    }

    private inner class IconVisibilityController {
        private var active: Boolean = false
        private var activeAnimator: ViewPropertyAnimator? = null

        private val mEndIconAnimListener = { _: ValueAnimator ->
            mTextInputLayout.setEndIconTintList(mEndIconColorTint.withAlpha(
                (mEndIconColorAlpha * (1f - mActiveLayout.alpha))
                    .toInt()
                    .coerceIn(0..255)
            ))
        }

        fun setActiveImmediate(value: Boolean) {
            if (value) {
                mIconIdle.visibility = View.GONE
                mActiveLayout.visibility = View.VISIBLE
                mActiveLayout.alpha = 1f
                mTextInputLayout.setEndIconTintList(mEndIconColorTint.withAlpha(0))
            } else {
                mIconIdle.visibility = View.VISIBLE
                mActiveLayout.visibility = View.GONE
                mIconIdle.alpha = 1f
                mTextInputLayout.setEndIconTintList(mEndIconColorTint.withAlpha(255))
            }
            active = value
        }

        fun setActive(value: Boolean) {
            if (active && !value) {
                activeAnimator?.cancel()

                mIconIdle.visibility = View.VISIBLE
                mIconIdle.alpha = 1f
                if (mEndIconDrawable != null) {
                    mTextInputLayout.endIconDrawable = mEndIconDrawable
                    mTextInputLayout.setEndIconTintList(mEndIconColorTint.withAlpha(0))
                }

                mActiveLayout.animate()
                    .also { activeAnimator = it }
                    .setInterpolator(mLinearInterpolator)
                    .alpha(0f)
                    .setDuration(mStateTransitionDuration)
                    .setUpdateListener(mEndIconAnimListener)
                    .onCompleted {
                        mActiveLayout.visibility = View.GONE
                        activeAnimator = null
                    }
                    .start()
            } else if (!active && value) {
                activeAnimator?.cancel()

                mActiveLayout.visibility = View.VISIBLE
                mActiveLayout.alpha = 0f

                if (mEndIconDrawable != null) {
                    mTextInputLayout.endIconDrawable = mEndIconDrawable
                    mTextInputLayout.setEndIconTintList(mEndIconColorTint)
                }

                mActiveLayout.animate()
                    .also { activeAnimator = it }
                    .setInterpolator(mLinearInterpolator)
                    .alpha(1f)
                    .setDuration(mStateTransitionDuration)
                    .setUpdateListener(mEndIconAnimListener)
                    .onCompleted {
                        mIconIdle.visibility = View.GONE
                        activeAnimator = null
                        mTextInputLayout.endIconDrawable = null
                    }
                    .start()
            }

            active = value
        }

        private fun ViewPropertyAnimator.onCompleted(block: () -> Unit): ViewPropertyAnimator {
            return setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) = block()
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
    }

    private inner class StateTransitionDrawableDelegate: Drawable() {
        private var isActive = false
        private var progress = 0f
        private var animator: ValueAnimator? = null

        init {
            mBackgroundIdleDrawable.callback = callback
            mBackgroundActiveDrawable.callback = callback
        }

        fun setActive(value: Boolean) {
            if (isActive == value) return
            isActive = value

            animator?.cancel()
            animator = ValueAnimator.ofFloat(progress, if (value) 1f else 0f).apply {
                duration = (mStateTransitionDuration * abs(progress - if (value) 1f else 0f)).toLong()
                interpolator = mLinearInterpolator
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidateSelf()
                }
                start()
            }
        }

        fun setActiveImmediate(active: Boolean) {
            if (isActive == active) return
            isActive = active

            animator?.cancel()
            progress = if (active) 1f else 0f

            invalidateSelf()
        }

        fun cancel() {
            animator?.cancel()
            animator?.removeAllUpdateListeners()
            animator = null
        }

        override fun draw(canvas: Canvas) {
            // 绘制两个 Drawable 并根据进度调整透明度
            if (progress < 1f) {
                mBackgroundIdleDrawable.alpha = ((1f - progress) * 255).toInt()
                mBackgroundIdleDrawable.draw(canvas)
            }

            if (progress > 0f) {
                mBackgroundActiveDrawable.alpha = (progress * 255).toInt()
                mBackgroundActiveDrawable.draw(canvas)
            }
        }

        override fun setAlpha(alpha: Int) {
            mBackgroundIdleDrawable.alpha = alpha
            mBackgroundActiveDrawable.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            mBackgroundIdleDrawable.colorFilter = colorFilter
            mBackgroundActiveDrawable.colorFilter = colorFilter
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
            super.setBounds(left, top, right, bottom)
            mBackgroundIdleDrawable.setBounds(left, top, right, bottom)
            mBackgroundActiveDrawable.setBounds(left, top, right, bottom)
        }

        override fun getIntrinsicWidth(): Int =
            max(mBackgroundIdleDrawable.intrinsicWidth, mBackgroundActiveDrawable.intrinsicWidth)

        override fun getIntrinsicHeight(): Int =
            max(mBackgroundIdleDrawable.intrinsicHeight, mBackgroundActiveDrawable.intrinsicHeight)
    }
}