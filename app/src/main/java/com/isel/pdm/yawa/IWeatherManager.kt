package com.isel.pdm.yawa


// TODO: add public methods of WeatherManager class
interface IWeatherManager {
    companion  object {
        val DATE_KEY: String = "dateKey"
        val MAX_TEMP_KEY: String = "maxTempKey"
        val MIN_TEMP_KEY: String = "minTempKey"
        val WEATHER_ICON_KEY: String = "iconKey"
    }
    fun getWeatherIcon(iconID: String, callbackSet : ICallbackSet)
}