package com.isel.pdm.yawa.DataContainers

class ForecastDO(
        val cityName : String,
        val cityID: Int,
        val cityCountry: String,
        val weatherStateDOList: List<WeatherStateDO>)
{}