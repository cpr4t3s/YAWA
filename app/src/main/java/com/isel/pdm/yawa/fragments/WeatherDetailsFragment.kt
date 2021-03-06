package com.isel.pdm.yawa.fragments

import android.app.Fragment
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.isel.pdm.yawa.*
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.provider.DbSchema
import com.isel.pdm.yawa.provider.IconCacheContract
import com.isel.pdm.yawa.tools.DateConverter
import com.isel.pdm.yawa.tools.MetricsResolver
import java.text.SimpleDateFormat
import java.util.*


/**
 * UI can be updated right away when this fragment is created, setting this property
 *  - used to show forecast in ForecastActivity
 * Or it can be set later, calling updateUi() method and passing a WeatherStateDO
 *  - used to show current weather in MainActivity
 */
class WeatherDetailsFragment: Fragment() {
    companion object {
        val POSITION_TAG: String = "positiontag"
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.weather_detail_fragment_layout, container, false)
    }

    override fun onActivityCreated (savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set title
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        val city = sharedPref.getString(activity.application.settingsLocationStr, activity.application.defaultLocation)
        val txtTitleCity = activity.findViewById(R.id.txtTitleCity) as TextView
        txtTitleCity.text = city

        // Set weather data if already exists
        arguments?.let {
            if(arguments.containsKey(POSITION_TAG)) {
                val position = arguments.getInt(POSITION_TAG) - 1
                val weatherDO =
                        activity.application.weatherManager.getSpecificForecastDay(
                                city, position, activity.application.cacheResolver)
                updateUI(weatherDO)
            }
        }
    }

    /**
     * Refresh the UI with weather state
     */
    fun updateUI(weatherState: WeatherStateDO) {
        val unit = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext).
                getString(activity.application.settingsMetricStr, activity.application.defaultMetric)
        val unitSymbol = MetricsResolver.getMetricSymbol(unit)

        // Main state
        var tmpTextView = activity.findViewById(R.id.weatherMain) as TextView
        tmpTextView.text = weatherState.mainState
        // Description
        tmpTextView = activity.findViewById(R.id.weatherDescription) as TextView
        tmpTextView.text = weatherState.description
        tmpTextView = activity.findViewById(R.id.weatherhumidity) as TextView
        tmpTextView.text = weatherState.humidity.toString()
        // we dont have Current Temp when in forecast
        tmpTextView = activity.findViewById(R.id.weatherTempCurrent) as TextView
        tmpTextView.text = "${weatherState.temp} $unitSymbol"
        var tmpLabel = activity.findViewById(R.id.current_temp_label) as TextView
        if(activity.componentName.className.equals(ForecastActivity::class.java.name)) {
            tmpTextView.visibility = View.GONE
            tmpLabel.visibility = View.GONE
        }
        //
        tmpTextView = activity.findViewById(R.id.weatherTempMax) as TextView
        tmpTextView.text = "${weatherState.temp_max} $unitSymbol"
        tmpTextView = activity.findViewById(R.id.weatherTempMin) as TextView
        tmpTextView.text = "${weatherState.temp_min} $unitSymbol"
        tmpTextView = activity.findViewById(R.id.lastUpdateTextView) as TextView
        val date: String
        if(weatherState.updateDate > 0)
            date = DateConverter.unixSecondsToDateString(weatherState.updateDate,
                    TimeZone.getDefault(), SimpleDateFormat("yyyy-MM-dd, HH:mm"))
        else
            date = "--"
        tmpTextView.text = date

        weatherState.weatherIcon.let {
            val tmpImageView = activity.findViewById(R.id.imageViewWeatherState) as ImageView
            tmpImageView.setImageBitmap(weatherState.weatherIcon)
        }
    }
}