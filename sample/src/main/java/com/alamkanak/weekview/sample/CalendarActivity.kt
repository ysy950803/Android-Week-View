package com.alamkanak.weekview.sample

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alamkanak.weekview.WeekViewEvent
import com.alamkanak.weekview.MultiWeekViewLayout
import com.alamkanak.weekview.WeekViewUtil.today
import com.alamkanak.weekview.oneday.OneDayWeekViewLayout
import java.util.Calendar

@SuppressLint("ClickableViewAccessibility")
class CalendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        findViewById<MultiWeekViewLayout>(
            R.id.test_week_view_layout
        )?.eventsLoader = { newYear, newMonth ->
            mutableListOf<WeekViewEvent>().apply {
                addAll(getTestEvents(this@CalendarActivity, newYear, newMonth))
                addAll(getTestAllDayEvents(this@CalendarActivity, newYear, newMonth))
            }
        }
        findViewById<OneDayWeekViewLayout>(
            R.id.test_one_day_week_viewpager
        )?.eventsLoader = { newYear, newMonth, newDayOfMonth ->
            mutableListOf<WeekViewEvent>().apply {
                addAll(getTestEvents(this@CalendarActivity, newYear, newMonth))
                addAll(getTestAllDayEvents(this@CalendarActivity, newYear, newMonth))
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAndRemoveTask()
    }

    private fun getTestEvents(
        context: Context,
        newYear: Int,
        newMonth: Int
    ): MutableList<WeekViewEvent> {
        val events = mutableListOf<WeekViewEvent>()
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
            borderColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_5494ff),
                ContextCompat.getColor(context, R.color.color_b4b4b4)
            )
            bgColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_f1f8ff),
                ContextCompat.getColor(context, R.color.color_f7f8f8)
            )
            textColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_2c5da7),
                ContextCompat.getColor(context, R.color.color_959595)
            )
            baseBgColor =
                ContextCompat.getColor(context, R.color.color_b3ffffff)
            status = WeekViewEvent.Status.TENTATIVE
        })

        val startTime1 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
            add(Calendar.HOUR_OF_DAY, 7)
            this[Calendar.MINUTE] = 15
        }
        val endTime1 = (startTime1.clone() as Calendar).apply {
            add(Calendar.HOUR, 2)
        }
        events.add(
            WeekViewEvent(
                2,
                "测试标题测试标题测试标题测试标题测试标题",
                "三日视图测试地址三日视图测试地址",
                startTime1,
                endTime1
            ).apply {
                borderColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.color_5494ff),
                    ContextCompat.getColor(context, R.color.color_b4b4b4)
                )
                bgColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.color_f1f8ff),
                    ContextCompat.getColor(context, R.color.color_f7f8f8)
                )
                textColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.color_2c5da7),
                    ContextCompat.getColor(context, R.color.color_959595)
                )
                baseBgColor =
                    ContextCompat.getColor(context, R.color.color_b3ffffff)
            }
        )
        val startTime2 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
            this[Calendar.MINUTE] = 30
        }
        val endTime2 = (startTime2.clone() as Calendar).apply {
            add(Calendar.HOUR, 1)
        }
        events.add(WeekViewEvent(3, "三日视图测试2", startTime2, endTime2).apply {
            borderColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_5494ff),
                ContextCompat.getColor(context, R.color.color_b4b4b4)
            )
            bgColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_f1f8ff),
                ContextCompat.getColor(context, R.color.color_f7f8f8)
            )
            textColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_2c5da7),
                ContextCompat.getColor(context, R.color.color_959595)
            )
            baseBgColor =
                ContextCompat.getColor(context, R.color.color_b3ffffff)
            status = WeekViewEvent.Status.CONFIRMED
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
            borderColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_5494ff),
                ContextCompat.getColor(context, R.color.color_b4b4b4)
            )
            bgColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_f1f8ff),
                ContextCompat.getColor(context, R.color.color_f7f8f8)
            )
            textColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_2c5da7),
                ContextCompat.getColor(context, R.color.color_959595)
            )
            baseBgColor =
                ContextCompat.getColor(context, R.color.color_b3ffffff)
            status = WeekViewEvent.Status.CANCELED
        })

        return events
    }

    private fun getTestAllDayEvents(
        context: Context,
        newYear: Int,
        newMonth: Int
    ): MutableList<WeekViewEvent> {
        val events = mutableListOf<WeekViewEvent>()
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
            borderColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_5494ff),
                ContextCompat.getColor(context, R.color.color_b4b4b4)
            )
            bgColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_f1f8ff),
                ContextCompat.getColor(context, R.color.color_f7f8f8)
            )
            textColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_2c5da7),
                ContextCompat.getColor(context, R.color.color_959595)
            )
            baseBgColor =
                ContextCompat.getColor(context, R.color.color_b3ffffff)
        })

        val startTime1 = (startTime.clone() as Calendar)
        val endTime1 = (startTime1.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
            add(Calendar.HOUR_OF_DAY, 23)
        }
        events.add(WeekViewEvent(5, "全天日程2", null, startTime1, endTime1, true).apply {
            borderColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_ff8628),
                ContextCompat.getColor(context, R.color.color_b4b4b4)
            )
            bgColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_fff4e9),
                ContextCompat.getColor(context, R.color.color_f7f8f8)
            )
            textColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_c54e00),
                ContextCompat.getColor(context, R.color.color_959595)
            )
            baseBgColor =
                ContextCompat.getColor(context, R.color.color_b3ffffff)
            status = WeekViewEvent.Status.CONFIRMED
        })

        val startTime2 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val endTime2 = (startTime2.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            add(Calendar.HOUR_OF_DAY, 23)
        }
        events.add(WeekViewEvent(6, "全天日程3", null, startTime2, endTime2, true).apply {
            borderColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_ff8628),
                ContextCompat.getColor(context, R.color.color_b4b4b4)
            )
            bgColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_fff4e9),
                ContextCompat.getColor(context, R.color.color_f7f8f8)
            )
            textColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_c54e00),
                ContextCompat.getColor(context, R.color.color_959595)
            )
            baseBgColor =
                ContextCompat.getColor(context, R.color.color_b3ffffff)
            status = WeekViewEvent.Status.CANCELED
        })

        val startTime3 = (startTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -3)
        }
        val endTime3 = (startTime3.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 2)
            add(Calendar.HOUR_OF_DAY, 23)
        }
        events.add(WeekViewEvent(7, "全天日程4", null, startTime3, endTime3, true).apply {
            borderColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_ff8628),
                ContextCompat.getColor(context, R.color.color_b4b4b4)
            )
            bgColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_fff4e9),
                ContextCompat.getColor(context, R.color.color_f7f8f8)
            )
            textColors = intArrayOf(
                ContextCompat.getColor(context, R.color.color_c54e00),
                ContextCompat.getColor(context, R.color.color_959595)
            )
            baseBgColor =
                ContextCompat.getColor(context, R.color.color_b3ffffff)
            status = WeekViewEvent.Status.CONFIRMED
        })

        val today = today()
        if (newYear == today[Calendar.YEAR] && newMonth - 1 == today[Calendar.MONTH]) {
            // 临时测试超长全天日程
            val startTime0 = (today().clone() as Calendar).apply {
                this[Calendar.HOUR_OF_DAY] = 0
                this[Calendar.MINUTE] = 0
                this[Calendar.MONTH] = 10 - 1
                this[Calendar.YEAR] = 2022
                this.add(Calendar.DAY_OF_YEAR, -180)
            }
            val endTime0 = (startTime0.clone() as Calendar).apply {
                this.add(Calendar.DAY_OF_YEAR, 360)
                this.add(Calendar.HOUR_OF_DAY, 23)
            }
            events.add(WeekViewEvent(8, "这个全天日程有一万年", null, startTime0, endTime0, true).apply {
                borderColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.color_ff8628),
                    ContextCompat.getColor(context, R.color.color_b4b4b4)
                )
                bgColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.color_fff4e9),
                    ContextCompat.getColor(context, R.color.color_f7f8f8)
                )
                textColors = intArrayOf(
                    ContextCompat.getColor(context, R.color.color_c54e00),
                    ContextCompat.getColor(context, R.color.color_959595)
                )
                baseBgColor =
                    ContextCompat.getColor(context, R.color.color_b3ffffff)
            })
        }

        return events
    }
}
