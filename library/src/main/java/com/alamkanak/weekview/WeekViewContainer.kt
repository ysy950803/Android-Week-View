package com.alamkanak.weekview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.core.widget.NestedScrollView
import kotlin.math.abs

class WeekViewContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private var scrollable = true
    private val mScaledTouchSlop: Int
    private var startX = 0f
    private var startY = 0f

    init {
        mScaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!scrollable) return false
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val distanceX = abs(ev.x - startX)
                val distanceY = abs(ev.y - startY)
                if (distanceX > mScaledTouchSlop && distanceX * 2f > distanceY) {
                    // 判断为横向滑动
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    fun setScrollable(enabled: Boolean) {
        scrollable = enabled
    }
}
