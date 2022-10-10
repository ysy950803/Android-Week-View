package com.alamkanak.weekview.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alamkanak.weekview.DateTimeInterpreter
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEvent
import com.alamkanak.weekview.WeekViewUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        findViewById<WeekView>(R.id.weekView)?.apply {
            setMonthChangeListener { newYear, newMonth ->
                val events: MutableList<WeekViewEvent> = ArrayList()
                val startTime = Calendar.getInstance().apply {
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
                    add(Calendar.DAY_OF_YEAR, 1)
                    this[Calendar.MINUTE] = 30
                }
                val endTime2 = (startTime2.clone() as Calendar).apply {
                    add(Calendar.HOUR, 1)
                }
                events.add(WeekViewEvent(2, "三日视图测试2", startTime2, endTime2).apply {
                    color = ContextCompat.getColor(this@CalendarActivity, R.color.color_FFF4E9)
                })

                val startTime3 = (startTime2.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    this[Calendar.HOUR_OF_DAY] = 0
                    this[Calendar.MINUTE] = 0
                }
                val endTime3 = (startTime3.clone() as Calendar).apply {
                    add(Calendar.HOUR_OF_DAY, 23)
                }
                events.add(WeekViewEvent(3, "全天日程", null, startTime3, endTime3, true).apply {
                    color = ContextCompat.getColor(this@CalendarActivity, R.color.color_FFF4E9)
                })

                events
            }
            dateTimeInterpreter = object : DateTimeInterpreter {
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
        }
    }
}
