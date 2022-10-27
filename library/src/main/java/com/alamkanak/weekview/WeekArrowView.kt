package com.alamkanak.weekview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import com.blankj.utilcode.util.ConvertUtils

class WeekArrowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var upArrow: ImageView? = null
    private var downArrow: ImageView? = null
    private var arrowAnimSet: AnimatorSet? = null

    init {
        inflate(context, R.layout.layout_week_arrow_view, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        upArrow = findViewById(R.id.iv_remind_up_arrow)
        downArrow = findViewById(R.id.iv_remind_down_arrow)
    }

    fun setUpArrowClickListener(listener: OnClickListener) {
        upArrow?.setOnClickListener(listener)
    }

    fun setDownArrowClickListener(listener: OnClickListener) {
        downArrow?.setOnClickListener(listener)
    }

    fun setArrowsVisible(upVisible: Boolean, downVisible: Boolean) {
        upArrow?.isVisible = upVisible
        downArrow?.isVisible = downVisible
    }

    fun startArrowAnim() {
        val height = ConvertUtils.dp2px(6f).toFloat()
        val u1 = ObjectAnimator.ofFloat(upArrow, View.TRANSLATION_Y, 0f, height)
        val u2 = ObjectAnimator.ofFloat(upArrow, View.ALPHA, 0.4f, 1f)
        val u3 = ObjectAnimator.ofFloat(upArrow, View.TRANSLATION_Y, height, 0f)
        val u4 = ObjectAnimator.ofFloat(upArrow, View.ALPHA, 1f, 0.4f)
        val b1 = ObjectAnimator.ofFloat(downArrow, View.TRANSLATION_Y, 0f, height)
        val b2 = ObjectAnimator.ofFloat(downArrow, View.ALPHA, 0.4f, 1f)
        val b3 = ObjectAnimator.ofFloat(downArrow, View.TRANSLATION_Y, height, 0f)
        val b4 = ObjectAnimator.ofFloat(downArrow, View.ALPHA, 1f, 0.4f)
        val up1 = AnimatorSet().apply { play(u3).with(u2) }
        val up2 = AnimatorSet().apply { play(u1).with(u4) }
        val upSet = AnimatorSet().apply { playSequentially(up1, up2) }
        val bottom1 = AnimatorSet().apply { play(b1).with(b2) }
        val bottom2 = AnimatorSet().apply { play(b3).with(b4) }
        val bottomSet = AnimatorSet().apply { playSequentially(bottom1, bottom2) }
        arrowAnimSet = AnimatorSet().apply {
            play(upSet).with(bottomSet)
            addListener(object : AnimatorListenerAdapter() {
                private var canceled = false
                override fun onAnimationEnd(animation: Animator) {
                    if (!canceled) {
                        animation.start()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }
            })
            duration = 600L
            post { start() }
        }
    }

    fun clearArrowAnim() {
        upArrow?.clearAnimation()
        downArrow?.clearAnimation()
        arrowAnimSet?.run {
            removeAllListeners()
            cancel()
        }
        arrowAnimSet = null
    }
}
