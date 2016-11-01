package com.isel.pdm.yawa.openweather_tools

import android.graphics.Bitmap
import com.isel.pdm.yawa.DataContainers.CityDO
import com.isel.pdm.yawa.DataContainers.ForecastDO

import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.iterator

import org.json.JSONArray
import org.json.JSONObject

import java.util.*

object OpenWeatherParser {

    private fun buildWeatherStateDO(weather: JSONObject, weatherDescription: JSONObject, wind: JSONObject) : WeatherStateDO {
        return WeatherStateDO(
                weatherDescription.getString("main"),
                weatherDescription.getString("description"),
                weatherDescription.getString("icon"),
                null,
                weather.getDouble("temp"),
                weather.getDouble("temp_min"),
                weather.getDouble("temp_max"),
                weather.getDouble("pressure"),
                weather.getDouble("sea_level"),
                weather.getDouble("grnd_level"),
                weather.getDouble("humidity"),
                wind.getDouble("speed"),
                wind.getDouble("deg"),
                0
        )
    }
    private fun buildWeatherStateDOForForecast(root: JSONObject,temp: JSONObject, weather: JSONObject) : WeatherStateDO {
        return WeatherStateDO(
                weather.getString("main"),
                weather.getString("description"),
                weather.getString("icon"),
                null,
                0.0,
                temp.getDouble("min"),
                temp.getDouble("max"),
                root.getDouble("pressure"),
                0.0,
                0.0,
                root.getDouble("humidity"),
                root.getDouble("speed"),
                root.getDouble("deg"),
                root.getLong("dt")
        )
    }
    fun parseWeatherState(jsonObject: JSONObject): WeatherStateDO {
        // Build weather DO
        val weather: JSONObject = jsonObject.getJSONObject("main")
        val weatherDescription: JSONObject = jsonObject.getJSONArray("weather").get(0) as JSONObject
        val wind: JSONObject = jsonObject.getJSONObject("wind")
        return buildWeatherStateDO(weather, weatherDescription, wind)
    }

    fun setWeatherIcon(bitmap: Bitmap, weatherStateContainer: WeatherStateDO) {
        weatherStateContainer.weatherIcon = bitmap
    }

    fun parseCitiesList(jsonObject: JSONObject) : List<CityDO> {
        var citiesList: ArrayList<CityDO> = ArrayList<CityDO>()

        // check if the request is valid
        if(!jsonObject.has("list")) {
            return citiesList
        }

        val ts = jsonObject.get("list") as JSONArray
        for (t in ts) {

            // Build weather DO
            val weather: JSONObject = t.getJSONObject("main")
            val weatherDescription: JSONObject = t.getJSONArray("weather").get(0) as JSONObject
            val wind: JSONObject = t.getJSONObject("wind")
            val wState: WeatherStateDO = buildWeatherStateDO(weather, weatherDescription, wind)

            // Build City DO
            val sysObj: JSONObject = t.getJSONObject("sys")
            val cityDO: CityDO = CityDO(
                    t.getString("name"),
                    t.getInt("id"),
                    sysObj.getString("country"),
                    wState
            )

            citiesList.add(cityDO)
        }

        return citiesList
    }

    private fun parseWeatherStateList(jsonArray: JSONArray): List<WeatherStateDO>{
       val toReturn : ArrayList<WeatherStateDO> = ArrayList<WeatherStateDO>()

        for (t in jsonArray){
            // Build weather DO
            val temp: JSONObject = t.getJSONObject("temp")
            val weather: JSONObject = t.getJSONArray("weather").get(0) as JSONObject
            val wState: WeatherStateDO = buildWeatherStateDOForForecast(t, temp, weather)
            toReturn.add(wState)
        }
        return toReturn
    }

    fun parseForecastCity(jsonObject: JSONObject) : ForecastDO{

        val temp = jsonObject.get("list") as JSONArray
        val  weatherList = parseWeatherStateList(temp) as List<WeatherStateDO>
        // Build City DO
        val city: JSONObject = jsonObject.getJSONObject("city")
        val forecastDO: ForecastDO = ForecastDO(
                city.getString("name"),
                city.getInt("id"),
                city.getString("country"),
                weatherList)
        return forecastDO
    }

}
