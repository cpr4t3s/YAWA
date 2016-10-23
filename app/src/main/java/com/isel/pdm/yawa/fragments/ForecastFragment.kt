package com.isel.pdm.yawa.fragments

import android.app.ListFragment
import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import com.android.volley.VolleyError
import com.isel.pdm.yawa.*
import com.isel.pdm.yawa.DataContainers.ForecastDO
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.UI.ForecastListAdapter
import com.isel.pdm.yawa.tools.DateConverter
import java.text.SimpleDateFormat
import java.util.*

class ForecastFragment : ListFragment() {
    private val swR by lazy { activity.findViewById(R.id.forecast_weather_swiperefresh) as SwipeRefreshLayout }
    private val callbackSet : ICallbackSet by lazy {
        object : ICallbackSet {
            override fun onError(error: VolleyError) {
                swR.isRefreshing = false
                Toast.makeText(activity, R.string.error1004, Toast.LENGTH_SHORT).show()
            }
            override fun onSucceed(response: Any) {
                swR.isRefreshing = false
                val forecastState = response as ForecastDO

                listView.adapter = ForecastListAdapter(activity,
                        this@ForecastFragment.buildListViewDataSet(forecastState),
                        activity.application.weatherManager)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.forecast_fragment_layout, container, false)
    }
    private fun buildListViewDataSet(forecastList: ForecastDO) : ArrayList<Map<String, String>> {
        val retList = ArrayList<Map<String, String>>()
        for(forecastDO: WeatherStateDO in forecastList.weatherStateDOList) {
            // Converte Unix seconds to Date
            val date = DateConverter.unixSecondsToDateString(forecastDO.date,
                    TimeZone.getTimeZone("GMT"), SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
            retList.add(
                    mapOf(
                            Pair(IWeatherManager.DATE_KEY, date),
                            Pair(IWeatherManager.MAX_TEMP_KEY, forecastDO.temp_max.toString()),
                            Pair(IWeatherManager.MIN_TEMP_KEY, forecastDO.temp_min.toString()),
                            Pair(IWeatherManager.WEATHER_ICON_KEY, forecastDO.weatherIconID)
                    )
            )
        }

        return retList
    }
    override fun onActivityCreated (savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val settings = activity.getSharedPreferences(activity.application.settingsFileName, Context.MODE_PRIVATE)
        val cityId = settings.getString(activity.application.settingsCityIDStr, activity.application.defaultCityId)

        // Set click listener
        listView.setOnItemClickListener { adapterView, view, position, id ->
            onListViewItemClicked(adapterView, view, position, id)
        }

        activity.application.weatherManager.getForecastByCityId(cityId, callbackSet)

        swR.setOnRefreshListener({
                    activity.application.weatherManager.refreshForecastWeather(cityId, callbackSet)
                })
    }

    /**
     * Called when the user press an item of ListView
     */
    private fun onListViewItemClicked(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        println("------------ Position")
        println(position)
        println(id)
        println(DateConverter.unixSecondsToDateString(activity.application.weatherManager.getLocalForecastWeather().weatherStateDOList[position].date, TimeZone.getTimeZone("GMT"), SimpleDateFormat("yyyy-MM-dd HH:mm:ss")))
    }

}