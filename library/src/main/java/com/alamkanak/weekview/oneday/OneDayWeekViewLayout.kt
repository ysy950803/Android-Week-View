package com.alamkanak.weekview.oneday

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.alamkanak.weekview.R
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEvent
import com.alamkanak.weekview.WeekViewUtil.allDaysBetween
import com.alamkanak.weekview.WeekViewUtil.calculateDiffDay
import com.alamkanak.weekview.WeekViewUtil.today
import java.util.Calendar
import java.util.concurrent.TimeUnit

class OneDayWeekViewLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val MIN_YEAR = 1971
        private const val MIN_YEAR_MONTH = 1
        private const val MIN_MONTH_DAY = 1
        private const val MAX_YEAR = 2055
        private const val MAX_YEAR_MONTH = 12
        private const val MAX_MONTH_DAY = 31
    }

    var dayChangeListener: ((curDate: Calendar) -> Unit)? = null
    var eventsLoader: ((
        newYear: Int, newMonth: Int,
        newDayOfMonth: Int
    ) -> MutableList<WeekViewEvent>)? = null
    var viewEventClickListener: ((
        weekViewEvent: WeekViewEvent?,
        createSTime: Calendar?, createETime: Calendar?
    ) -> Unit)? = null

    private val weekViewPager: ViewPager
    private val startDate: Calendar
        get() = field.clone() as Calendar
    private val viewCount: Int
    private var currentView: OneDayWeekView? = null

    init {
        inflate(context, R.layout.wv_layout_one_day_week_viewpager, this)

        startDate = today().apply {
            set(MIN_YEAR, MIN_YEAR_MONTH - 1, MIN_MONTH_DAY)
        }
        val endDate = today().apply {
            set(MAX_YEAR, MAX_YEAR_MONTH - 1, MAX_MONTH_DAY)
        }
        viewCount = allDaysBetween(startDate, endDate)

        weekViewPager = findViewById<ViewPager>(R.id.one_day_week_viewpager).apply {
            adapter = InnerPagerAdapter()
            currentItem = allDaysBetween(startDate, WeekView.initCurDate)
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    dayChangeListener?.invoke(startDate.apply {
                        add(Calendar.DATE, position)
                    })
                }

                override fun onPageScrollStateChanged(state: Int) {
                }
            })
        }
    }

    private inner class InnerPagerAdapter : PagerAdapter() {
        override fun getCount(): Int = viewCount

        override fun isViewFromObject(view: View, `object`: Any) = view == `object`

        override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
            currentView = `object` as? OneDayWeekView
            post { currentView?.weekViewContainer?.scrollY = OneDayWeekView.curScrollY }
            super.setPrimaryItem(container, position, `object`)
        }

        override fun instantiateItem(container: ViewGroup, position: Int) = runCatching {
            (OneDayWeekView::class.java.getConstructor(
                Context::class.java
            ).newInstance(context) as OneDayWeekView).apply {
                updateCurrentDate(startDate.apply {
                    add(Calendar.DATE, position)
                }, eventsLoader)
                this.viewEventClickListener = this@OneDayWeekViewLayout.viewEventClickListener
                container.addView(this)

                post { weekViewContainer.scrollY = OneDayWeekView.curScrollY }
            }
        }.getOrDefault(View(context))

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            (`object` as? OneDayWeekView)?.let { container.removeView(it) }
        }
    }

    fun goToDate(date: Calendar) {
        weekViewPager.currentItem = calculateDiffDay(startDate, date)
        if (date[Calendar.HOUR_OF_DAY] != 0 || date[Calendar.MINUTE] != 0) {
            postDelayed({
                currentView?.weekView?.goToHour(date[Calendar.HOUR_OF_DAY])
            }, TimeUnit.SECONDS.toMillis(1))
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dayChangeListener = null
        eventsLoader = null
        viewEventClickListener = null
    }
}
