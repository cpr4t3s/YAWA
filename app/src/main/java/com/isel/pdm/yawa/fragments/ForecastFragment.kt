package com.isel.pdm.yawa.fragments

import android.app.ListFragment
import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import android.widget.Toast
import com.android.volley.VolleyError
import com.isel.pdm.yawa.*
import com.isel.pdm.yawa.DataContainers.ForecastDO
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
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
                val weatherState = response as ForecastDO

                listView.adapter = SimpleAdapter(activity,
                        this@ForecastFragment.buildListViewDataSet(weatherState),
                        android.R.layout.simple_list_item_2,
                        arrayOf("city", "temp","max","min"),
                        intArrayOf(android.R.id.text1, android.R.id.text2,android.R.id.text2,android.R.id.text2))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.forecast_fragment_layout, container, false)
    }
    private fun buildListViewDataSet(forecastList: ForecastDO) : ArrayList<Map<String, Any>> {
        val retList = ArrayList<Map<String, Any>>()
        for(forecastDO: WeatherStateDO in forecastList.weatherStateDOList) {
            retList.add(
                    mapOf(
                            Pair("city", forecastList.cityName),
                            Pair("temp", forecastDO.temp),
                            Pair("max", forecastDO.temp_max),
                            Pair("min", forecastDO.temp_min)
                    )
            )
        }

        return retList
    }
    override fun onActivityCreated (savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val settings = activity.getSharedPreferences(activity.application.settingsFileName, Context.MODE_PRIVATE)
        val cityId = settings.getString(activity.application.settingsCityIDStr, activity.application.defaultCityId)

        activity.application.weatherManager.getForecastByCityId(cityId, callbackSet)

        swR.setOnRefreshListener({
                    activity.application.weatherManager.refreshForecastWeather(cityId, callbackSet)
                })
    }

}