package com.alamkanak.weekview.sample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alamkanak.weekview.DateTimeInterpreter
import com.alamkanak.weekview.WeekHeaderView
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEvent
import com.alamkanak.weekview.WeekViewUtil
import com.alamkanak.weekview.WeekViewUtil.today
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@SuppressLint("ClickableViewAccessibility")
class CalendarActivity : AppCompatActivity() {

    private var touchHeader = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        val weekHeaderView = findViewById<WeekHeaderView>(R.id.weekHeaderView)
        val weekView = findViewById<WeekView>(R.id.weekView)

        weekHeaderView?.apply {
            setMonthChangeListener { newYear, newMonth ->
                onMonthChangeListenerAllDay(newYear, newMonth)
            }
            dateTimeInterpreter = createDateTimeInterpreter()

            setOnTouchListener { _, _ ->
                touchHeader = true
                false
            }
            setCascadeScrollListener(object : WeekHeaderView.CascadeScrollListener {
                override fun onScrolling(currentOriginX: Float) {
                    if (touchHeader) weekView?.setCurrentOriginX(currentOriginX)
                }

                override fun onScrollEnd(newFirstVisibleDay: Calendar) {
                    if (touchHeader) weekView?.goFirstVisibleDay(newFirstVisibleDay)
                }
            })
            setDayHasEvents(weekView?.dayHasEvents)
        }
        weekView?.apply {
            setMonthChangeListener { newYear, newMonth ->
                onMonthChangeListener(newYear, newMonth)
            }
            dateTimeInterpreter = createDateTimeInterpreter()

            setOnTouchListener { _, _ ->
                touchHeader = false
                false
            }
            setCascadeScrollListener(object : WeekView.CascadeScrollListener {
                override fun onScrolling(currentOriginX: Float) {
                    if (!touchHeader) weekHeaderView?.setCurrentOriginX(currentOriginX)
                }

                override fun onScrollEnd(newFirstVisibleDay: Calendar) {
                    if (!touchHeader) weekHeaderView?.goFirstVisibleDay(newFirstVisibleDay)
                }
            })
        }
    }

    private fun onMonthChangeListener(newYear: Int, newMonth: Int): List<WeekViewEvent> {
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
            add(Calendar.HOUR_OF_DAY, -1)
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

        return events
    }

    private fun onMonthChangeListenerAllDay(newYear: Int, newMonth: Int): List<WeekViewEvent> {
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
        events.add(WeekViewEvent(4, "1", null, startTime, endTime, true).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.event_color_01)
        })

        val startTime1 = (startTime.clone() as Calendar)
        val endTime1 = (startTime1.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
            add(Calendar.HOUR_OF_DAY, 23)
        }
        events.add(WeekViewEvent(5, "2", null, startTime1, endTime1, true).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.event_color_02)
        })

        val startTime2 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val endTime2 = (startTime2.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            add(Calendar.HOUR_OF_DAY, 23)
        }
        events.add(WeekViewEvent(6, "3", null, startTime2, endTime2, true).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.event_color_03)
        })

        val startTime3 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -3)
        }
        val endTime3 = (startTime3.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
            add(Calendar.HOUR_OF_DAY, 23)
        }
        events.add(WeekViewEvent(7, "4", null, startTime3, endTime3, true).apply {
            color = ContextCompat.getColor(this@CalendarActivity, R.color.event_color_04)
        })
        return events
    }

    private fun createDateTimeInterpreter() = object : DateTimeInterpreter {
        override fun interpretDate(date: Calendar): List<String> = try {
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            listOf(
                // TODO
                if (WeekViewUtil.isSameDay(date, Calendar.getInstance())) "今天"
                else sdf.format(date.time).toUpperCase(Locale.getDefault()),
                "${date.get(Calendar.DAY_OF_MONTH)}"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            listOf("", "")
        }

        override fun interpretTime(hour: Int): String = try {
            val calendar = Calendar.getInstance().apply {
                this[Calendar.HOUR_OF_DAY] = hour
                this[Calendar.MINUTE] = 0
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
}
