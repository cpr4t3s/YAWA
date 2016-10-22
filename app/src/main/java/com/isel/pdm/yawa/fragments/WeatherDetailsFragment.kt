package com.isel.pdm.yawa.fragments

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.R


class WeatherDetailsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.weather_detail_fragment_layout, container, true)
    }

    /**
     * Refresh the UI with weather state
     */
    public fun updateUI(weatherState: WeatherStateDO) {
        // Main state
        var tmpTextView = activity.findViewById(R.id.weatherMain) as TextView
        tmpTextView.text = weatherState.mainState
        // Description
        tmpTextView = activity.findViewById(R.id.weatherDescription) as TextView
        tmpTextView.text = weatherState.description
        tmpTextView = activity.findViewById(R.id.weatherDescription) as TextView
        tmpTextView.text = weatherState.description
        tmpTextView = activity.findViewById(R.id.weatherhumidity) as TextView
        tmpTextView.text = weatherState.humidity.toString()
        tmpTextView = activity.findViewById(R.id.weatherTempCurrent) as TextView
        tmpTextView.text = weatherState.temp.toString()
        tmpTextView = activity.findViewById(R.id.weatherTempMax) as TextView
        tmpTextView.text = weatherState.temp_max.toString()
        tmpTextView = activity.findViewById(R.id.weatherTempMin) as TextView
        tmpTextView.text = weatherState.temp_min.toString()

        // Icon. It may be null - when a diferent city is configured
        weatherState.weatherIcon.let {
            val tmpImageView = activity.findViewById(R.id.imageViewWeatherState) as ImageView
            tmpImageView.setImageBitmap(weatherState.weatherIcon)
        }
    }

}