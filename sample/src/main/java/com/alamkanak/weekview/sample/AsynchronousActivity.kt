package com.alamkanak.weekview.sample

import android.widget.Toast
import com.alamkanak.weekview.WeekViewEvent
import com.alamkanak.weekview.sample.apiclient.Event
import com.alamkanak.weekview.sample.apiclient.MyJsonService
import retrofit.Callback
import retrofit.RestAdapter
import retrofit.RetrofitError
import retrofit.client.Response
import java.util.Calendar

/**
 * An example of how events can be fetched from network and be displayed on the week view.
 * Created by Raquib-ul-Alam Kanak on 1/3/2014.
 * Website: http://alamkanak.github.io
 */
class AsynchronousActivity : BaseActivity(), Callback<List<Event>> {

    private val events: MutableList<WeekViewEvent> = ArrayList()
    private var calledNetwork = false

    override fun onMonthChange(newYear: Int, newMonth: Int): MutableList<WeekViewEvent> {
        // Download events from network if it hasn't been done already. To understand how events are
        // downloaded using retrofit, visit http://square.github.io/retrofit
        if (!calledNetwork) {
            val retrofit = RestAdapter.Builder()
                .setEndpoint("https://api.myjson.com/bins")
                .build()
            val service = retrofit.create(MyJsonService::class.java)
            service.listEvents(this)
            calledNetwork = true
        }

        // Return only the events that matches newYear and newMonth.
        val matchedEvents: MutableList<WeekViewEvent> = ArrayList()
        for (event in events) {
            if (eventMatches(event, newYear, newMonth)) {
                matchedEvents.add(event)
            }
        }
        return matchedEvents
    }

    /**
     * Checks if an event falls into a specific year and month.
     * @param event The event to check for.
     * @param year The year.
     * @param month The month.
     * @return True if the event matches the year and month.
     */
    private fun eventMatches(event: WeekViewEvent, year: Int, month: Int): Boolean {
        return event.startTime[Calendar.YEAR] == year && event.startTime[Calendar.MONTH] == month - 1 || event.endTime[Calendar.YEAR] == year && event.endTime[Calendar.MONTH] == month - 1
    }

    override fun success(events: List<Event>, response: Response) {
        this.events.clear()
        for (event in events) {
            this.events.add(event.toWeekViewEvent())
        }
        weekView.notifyDatasetChanged()
    }

    override fun failure(error: RetrofitError) {
        error.printStackTrace()
        Toast.makeText(this, R.string.async_error, Toast.LENGTH_SHORT).show()
    }
}
