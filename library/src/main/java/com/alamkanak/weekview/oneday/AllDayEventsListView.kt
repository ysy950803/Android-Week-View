package com.alamkanak.weekview.oneday

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.alamkanak.weekview.R
import com.alamkanak.weekview.WeekViewEvent
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AllDayEventsListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var maxHeight: Int = 0

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.AllDayEventsListView, 0, 0)
        try {
            maxHeight = a.getDimensionPixelSize(R.styleable.AllDayEventsListView_maxHeight, 0)
        } finally {
            a.recycle()
        }
        adapter = InnerListAdapter()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(
            widthSpec,
            if (maxHeight in 1..MeasureSpec.getSize(heightSpec))
                MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
            else heightSpec
        )
    }

    fun setData(data: MutableList<WeekViewEvent>) {
        (adapter as? InnerListAdapter)?.setNewInstance(data)
    }

    private inner class InnerListAdapter : BaseQuickAdapter<WeekViewEvent, BaseViewHolder>(
        R.layout.wv_item_all_day_events
    ) {
        override fun convert(holder: BaseViewHolder, item: WeekViewEvent) {
            (holder.itemView as? AllDayEventsItemView)?.updateData(item)
        }
    }
}
