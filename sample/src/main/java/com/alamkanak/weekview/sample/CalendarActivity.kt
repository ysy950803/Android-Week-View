package com.alamkanak.weekview.sample

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.alamkanak.weekview.CascadeScrollListener
import com.alamkanak.weekview.DateTimeInterpreter
import com.alamkanak.weekview.MonthLoader
import com.alamkanak.weekview.ScrollListener
import com.alamkanak.weekview.WeekHeaderView
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewContainer
import com.alamkanak.weekview.WeekViewEvent
import com.alamkanak.weekview.WeekViewUtil
import com.alamkanak.weekview.WeekViewUtil.today
import com.blankj.utilcode.util.ConvertUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@SuppressLint("ClickableViewAccessibility")
class CalendarActivity : AppCompatActivity() {

    private lateinit var weekView: WeekView
    private var upArrow: ImageView? = null
    private var downArrow: ImageView? = null
    private var arrowAnimSet: AnimatorSet? = null
    private var touchHeader = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val weekHeaderView = findViewById<WeekHeaderView>(R.id.weekHeaderView)
        weekView = findViewById(R.id.weekView)
        upArrow = findViewById(R.id.iv_remind_up_arrow)
        downArrow = findViewById(R.id.iv_remind_down_arrow)
        upArrow?.setOnClickListener { weekView.goToTopEventRect() }
        downArrow?.setOnClickListener { weekView.goToBottomEventRect() }

        weekHeaderView?.apply {
            monthChangeListener = object : MonthLoader.MonthChangeListener {
                override fun onMonthChange(
                    newYear: Int,
                    newMonth: Int
                ): MutableList<WeekViewEvent> {
                    return onMonthChangeListenerAllDay(newYear, newMonth)
                }
            }
            dateTimeInterpreter = createDateTimeInterpreter()

            setOnTouchListener { _, _ ->
                touchHeader = true
                false
            }
            cascadeScrollListener = object : CascadeScrollListener {
                override fun onScrolling(currentOriginX: Float) {
                    if (touchHeader) weekView.setCurrentOriginX(currentOriginX)
                }

                override fun onScrollEnd(newFirstVisibleDay: Calendar) {
                    if (touchHeader) weekView.goFirstVisibleDay(newFirstVisibleDay)
                }
            }
            dayHasEvents = weekView.dayHasEvents
        }
        weekView.apply {
            monthChangeListener = object : MonthLoader.MonthChangeListener {
                override fun onMonthChange(
                    newYear: Int,
                    newMonth: Int
                ): MutableList<WeekViewEvent> {
                    return onMonthChangeListener(newYear, newMonth).also {
                        post { updateArrowVisible() }
                    }
                }
            }
            dateTimeInterpreter = createDateTimeInterpreter()

            setOnTouchListener { _, _ ->
                touchHeader = false
                false
            }
            cascadeScrollListener = object : CascadeScrollListener {
                override fun onScrolling(currentOriginX: Float) {
                    if (!touchHeader) weekHeaderView?.setCurrentOriginX(currentOriginX)
                }

                override fun onScrollEnd(newFirstVisibleDay: Calendar) {
                    if (!touchHeader) weekHeaderView?.goFirstVisibleDay(newFirstVisibleDay)
                }
            }
            scrollListener = object : ScrollListener {
                override fun onFirstVisibleDayChanged(
                    newFirstVisibleDay: Calendar,
                    oldFirstVisibleDay: Calendar?
                ) {
                    updateArrowVisible()
                }
            }
        }

        findViewById<WeekViewContainer>(
            R.id.weekview_container
        )?.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
            updateArrowVisible()
        })
        startArrowAnim()
    }

    private fun onMonthChangeListener(newYear: Int, newMonth: Int): MutableList<WeekViewEvent> {
        val events: MutableList<WeekViewEvent> = ArrayList()
        val startTime = (today().clone() as Calendar).apply {
            this[Calendar.HOUR_OF_DAY] = 14
            this[Calendar.MINUTE] = 0
            this[Calendar.MONTH] = newMonth - 1
            this[Calendar.YEAR] = newYear
        }
        val endTime = (startTime.clone() as Calendar).apply {
            add(Calendar.HOUR, 1)
        }
        events.add(WeekViewEvent(1, "三日视图测试", startTime, endTime).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.color_FFF4E9)
        })

        val startTime1 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
            add(Calendar.HOUR_OF_DAY, 7)
            this[Calendar.MINUTE] = 15
        }
        val endTime1 = (startTime1.clone() as Calendar).apply {
            add(Calendar.HOUR, 2)
        }
        events.add(WeekViewEvent(2, "三日视图测试1", startTime1, endTime1).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.color_FFF4E9)
        })

        val startTime2 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
            this[Calendar.MINUTE] = 30
        }
        val endTime2 = (startTime2.clone() as Calendar).apply {
            add(Calendar.HOUR, 1)
        }
        events.add(WeekViewEvent(3, "三日视图测试2", startTime2, endTime2).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.color_FFF4E9)
        })

        val startTime3 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            this[Calendar.HOUR_OF_DAY] = 2
            this[Calendar.MINUTE] = 30
        }
        val endTime3 = (startTime3.clone() as Calendar).apply {
            add(Calendar.HOUR, 3)
        }
        events.add(WeekViewEvent(4, "三日视图测试3", startTime3, endTime3).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.color_FFF4E9)
        })

        return events
    }

    private fun onMonthChangeListenerAllDay(
        newYear: Int,
        newMonth: Int
    ): MutableList<WeekViewEvent> {
        val events: MutableList<WeekViewEvent> = ArrayList()
        val startTime = (today().clone() as Calendar).apply {
            this[Calendar.HOUR_OF_DAY] = 0
            this[Calendar.MINUTE] = 0
            this[Calendar.MONTH] = newMonth - 1
            this[Calendar.YEAR] = newYear
        }
        val endTime = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            add(Calendar.HOUR_OF_DAY, 23)
        }
        events.add(WeekViewEvent(4, "全天日程1", null, startTime, endTime, true).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.color_FFF4E9)
        })

        val startTime1 = (startTime.clone() as Calendar)
        val endTime1 = (startTime1.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
            add(Calendar.HOUR_OF_DAY, 23)
        }
        events.add(WeekViewEvent(5, "全天日程2", null, startTime1, endTime1, true).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.color_FFF4E9)
        })

        val startTime2 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val endTime2 = (startTime2.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            add(Calendar.HOUR_OF_DAY, 23)
        }
        events.add(WeekViewEvent(6, "全天日程3", null, startTime2, endTime2, true).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.color_FFF4E9)
        })

        val startTime3 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -3)
        }
        val endTime3 = (startTime3.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
            add(Calendar.HOUR_OF_DAY, 23)
        }
        events.add(WeekViewEvent(7, "全天日程4", null, startTime3, endTime3, true).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.color_FFF4E9)
        })
        return events
    }

    private fun createDateTimeInterpreter() = object : DateTimeInterpreter {
        override fun interpretDate(date: Calendar): List<String> = try {
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            listOf(
                // TODO 翻译
                if (WeekViewUtil.isSameDay(date, Calendar.getInstance())) "今天"
                else sdf.format(date.time).toUpperCase(Locale.getDefault()),
                "${date.get(Calendar.DAY_OF_MONTH)}"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            listOf("", "")
        }

        override fun interpretTime(hour: Int, minute: Int): String = try {
            val calendar = Calendar.getInstance().apply {
                this[Calendar.HOUR_OF_DAY] = hour
                this[Calendar.MINUTE] = minute
            }
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(calendar.time)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ""
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearArrowAnim()
    }

    private fun updateArrowVisible() {
        upArrow?.isVisible = !weekView.isTopEventRectVisible()
        downArrow?.isVisible = !weekView.isBottomEventRectVisible()
    }

    private fun startArrowAnim() {
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
            start()
        }
    }

    private fun clearArrowAnim() {
        upArrow?.clearAnimation()
        downArrow?.clearAnimation()
        arrowAnimSet?.run {
            removeAllListeners()
            cancel()
        }
        arrowAnimSet = null
    }
}
