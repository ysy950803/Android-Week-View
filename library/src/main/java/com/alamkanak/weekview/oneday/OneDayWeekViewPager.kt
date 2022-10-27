package com.alamkanak.weekview.oneday

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.alamkanak.weekview.R
import com.alamkanak.weekview.WeekViewEvent
import com.alamkanak.weekview.WeekViewUtil.allDaysBetween
import com.alamkanak.weekview.WeekViewUtil.today
import java.util.Calendar

class OneDayWeekViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var eventsLoader: ((
        newYear: Int, newMonth: Int,
        newDayOfMonth: Int
    ) -> MutableList<WeekViewEvent>)? = null

    companion object {
        private const val MIN_YEAR = 1971
        private const val MIN_YEAR_MONTH = 1
        private const val MIN_MONTH_DAY = 1
        private const val MAX_YEAR = 2055
        private const val MAX_YEAR_MONTH = 12
        private const val MAX_MONTH_DAY = 31
    }

    private lateinit var weekViewPager: ViewPager
    private var viewCount = 0
    private var currentView: OneDayWeekView? = null
    private lateinit var startDate: Calendar
    private lateinit var endDate: Calendar

    init {
        inflate(context, R.layout.wv_layout_one_day_week_viewpager, this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        eventsLoader = null
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        startDate = Calendar.getInstance().apply {
            set(MIN_YEAR, MIN_YEAR_MONTH - 1, MIN_MONTH_DAY)
            this[Calendar.HOUR_OF_DAY] = 0
            this[Calendar.MINUTE] = 0
            this[Calendar.SECOND] = 0
            this[Calendar.MILLISECOND] = 0
        }
        endDate = Calendar.getInstance().apply {
            set(MAX_YEAR, MAX_YEAR_MONTH - 1, MAX_MONTH_DAY)
            this[Calendar.HOUR_OF_DAY] = 0
            this[Calendar.MINUTE] = 0
            this[Calendar.SECOND] = 0
            this[Calendar.MILLISECOND] = 0
        }
        viewCount = allDaysBetween(startDate, endDate)

        weekViewPager = findViewById(R.id.one_day_week_viewpager)
        weekViewPager.apply {
            adapter = InnerPagerAdapter()
            currentItem = allDaysBetween(startDate, today())
        }
    }

    private inner class InnerPagerAdapter : PagerAdapter() {
        override fun getCount(): Int = viewCount

        override fun isViewFromObject(view: View, `object`: Any) = view == `object`

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            currentView = `object` as? OneDayWeekView
            super.setPrimaryItem(container, position, `object`)
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            return runCatching {
                (OneDayWeekView::class.java.getConstructor(
                    Context::class.java
                ).newInstance(context) as OneDayWeekView).apply {
                    updateCurrentDate((startDate.clone() as Calendar).apply {
                        add(Calendar.DATE, position)
                    }, eventsLoader)
                    container.addView(this)
                }
            }.getOrDefault(View(context))
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            (`object` as? OneDayWeekView)?.let { container.removeView(it) }
        }
    }
}
