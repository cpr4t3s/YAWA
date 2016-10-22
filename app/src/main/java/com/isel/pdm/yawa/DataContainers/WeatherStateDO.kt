package com.isel.pdm.yawa.DataContainers

import android.graphics.Bitmap

class WeatherStateDO (
        val mainState: String,
        val description: String,
        val weatherIconID: String,
        var weatherIcon: Bitmap?,
        val temp: Double,
        val temp_min: Double,
        val temp_max: Double,
        val pressure: Double,
        val seaLevel: Double,
        val groundLevel: Double,
        val humidity: Double,
        val windSpeed: Double,
        val windDegree: Double
)
{ }
