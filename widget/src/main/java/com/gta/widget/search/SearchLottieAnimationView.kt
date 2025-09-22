package com.gta.widget.search

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.gta.widget.R


/*直接引用该View，并设置静止自动播放
* app:lottie_autoPlay="false"
  app:lottie_loop="false"
  手动设置setStatus为1，循环播放“等待”动画
  * */
class SearchLottieAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LottieAnimationView(context, attrs, defStyleAttr) {

    // 当前状态
    private var currentState: SearchState = SearchState.IDLE
        set(value) {
            field = value
            // 每次状态改变时播放对应的动画
            playCurrentState()
        }

    init {
        setAnimation(R.raw.search_status)
        // 确保动画不会自动播放
        repeatMode = LottieDrawable.RESTART
        speed = 1f
        addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                // 动画结束时切换到下一个状态，实现无缝的循环动画
                when (currentState) {
                    SearchState.IDLE_TO_LISTENING -> currentState = SearchState.LISTENING
                    SearchState.LISTENING_TO_LOADING -> currentState = SearchState.LOADING
                    else -> {}
                }
            }
        })
    }


    fun setState(state: SearchState) {
        currentState = state
    }

    // 播放当前状态的动画
    private fun playCurrentState() {
        repeatCount = if (currentState == SearchState.LOADING ||
            currentState == SearchState.LISTENING ||
            currentState == SearchState.IDLE) LottieDrawable.INFINITE else 0
        setMinAndMaxFrame(currentState.startFrame, currentState.endFrame)
        playAnimation()
    }

    enum class SearchState(val startFrame: Int, val endFrame: Int) {
        IDLE(0, 95),
        IDLE_TO_LISTENING(96, 198),
        LISTENING(138, 198),
        LISTENING_TO_LOADING(201, 229),
        LOADING(230, 269),
    }
}