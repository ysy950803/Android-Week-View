package com.alamkanak.weekview.oneday

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import com.alamkanak.weekview.R
import com.alamkanak.weekview.WeekViewEvent
import com.alamkanak.weekview.withBgStyle
import com.alamkanak.weekview.withBorderStyle
import com.alamkanak.weekview.withTextStyle
import com.blankj.utilcode.util.ConvertUtils

class AllDayEventsItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val eventBackgroundPaint = Paint()
    private val eventBorderPaint = Paint()
    private var titleTextView: TextView
    private var data: WeekViewEvent? = null

    init {
        inflate(context, R.layout.wv_layout_all_day_events_item_view, this)
        setWillNotDraw(false)
        titleTextView = findViewById(R.id.all_day_events_item_text)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        eventBackgroundPaint.reset()
        eventBorderPaint.reset()
        data?.run {
            val rectLeft = 0f
            val rectTop = 0f
            val rectRight = width.toFloat()
            val rectBottom = height.toFloat()
            // 日程背景
            canvas.drawRect(
                rectLeft, rectTop, rectRight, rectBottom,
                eventBackgroundPaint.withBgStyle(this)
            )
            // 日程左侧装饰边界线
            canvas.drawRect(
                rectLeft, rectTop, rectLeft + ConvertUtils.dp2px(2f), rectBottom,
                eventBorderPaint.withBorderStyle(this)
            )
        }
    }

    fun updateData(data: WeekViewEvent) {
        this.data = data
        titleTextView.withTextStyle(data)
        invalidate()
    }
}
