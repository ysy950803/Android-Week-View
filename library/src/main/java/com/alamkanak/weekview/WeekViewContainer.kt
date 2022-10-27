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

    private val scaledTouchSlop: Int
    private var scrollable = true
    private var startX = 0f
    private var startY = 0f

    init {
        scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
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
                if (distanceX > scaledTouchSlop && distanceX * 2f > distanceY) {
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
