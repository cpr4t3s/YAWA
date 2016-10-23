package com.isel.pdm.yawa.fragments

import android.app.Application
import android.app.ListFragment
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import com.android.volley.VolleyError
import com.isel.pdm.yawa.*
import com.isel.pdm.yawa.DataContainers.ForecastDO
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import java.util.*

class ForecastFragment : ListFragment() {

    private val spinner by lazy {activity.findViewById(R.id.spinner) as ProgressBar }


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
        val cityId = settings.getString(activity.application.settingsCityId, activity.application.settingsCityId)
                // hide spinner
        spinner.visibility = View.GONE
        activity.application.weatherManager.getForecastByCityId(cityId,
                object : ICallbackSet {
                    override fun onError(error: VolleyError) {
                        print(error.cause)
                    }
                    override fun onSucceed(response: Any) {
                        val weatherState = response as ForecastDO
                        spinner.visibility = View.GONE

                        listView.adapter = SimpleAdapter(activity,
                                this@ForecastFragment.buildListViewDataSet(weatherState),
                                android.R.layout.simple_list_item_2,
                                arrayOf("city", "temp","max","min"),
                                intArrayOf(android.R.id.text1, android.R.id.text2,android.R.id.text2,android.R.id.text2))
                    }
                }
        )


    }


}