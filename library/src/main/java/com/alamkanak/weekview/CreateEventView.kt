package com.alamkanak.weekview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView

class CreateEventView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var createTextView: TextView
    private var createBg: LinearLayout
    private var newWidth = -1
    private var newHeight = -1
    private var showCreateText = true
    private var isConflict = true

    init {
        inflate(context, R.layout.select_layout, this)
        createTextView = findViewById(R.id.create_text)
        createBg = findViewById(R.id.create_bg)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (newWidth != -1 && newHeight != -1) {
            setMeasuredDimension(newWidth, newHeight)
        }
    }

    fun setWidthHeight(width: Int, height: Int) {
        this.newWidth = width
        this.newHeight = height
        requestLayout()
    }

    private fun setCreateBg(resId: Int) {
        createBg.setBackgroundResource(resId)
    }

    fun setInfo(conflict: Boolean, showCreateText: Boolean, touching: Boolean, textType: Int) {
        isConflict = conflict
        this.showCreateText = showCreateText
        createTextView.visibility = VISIBLE
        // TODO 翻译
        if (isConflict) {
            createTextView.text = "该时段有人冲突"
            createTextView.setTextColor(Color.parseColor("#FFFD7443"))
            setCreateBg(if (touching) R.drawable.create_event_bg_red_m else R.drawable.create_event_bg_red)
        } else {
            val str = if (textType == 0) "点击新建" else "所有人都有时间"
            createTextView.text = if (showCreateText) str else ""
            createTextView.setTextColor(Color.parseColor("#FF00B38B"))
            setCreateBg(if (touching) R.drawable.create_event_bg_m else R.drawable.create_event_bg)
        }
    }
}
