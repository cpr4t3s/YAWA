package com.isel.pdm.yawa.openweather_tools

import android.database.Cursor
import android.graphics.Bitmap
import com.isel.pdm.yawa.DataContainers.CityDO
import com.isel.pdm.yawa.DataContainers.ForecastDO

import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.iterator
import com.isel.pdm.yawa.provider.DbSchema
import com.isel.pdm.yawa.tools.ICacheSystem

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.util.*

object OpenWeatherParser {

    private fun getDoubleFromJSONObject(obj: JSONObject, key: String): Double {
        try {
            return obj.getDouble(key)
        }
        catch (e: JSONException) {
            return 0.0
        }
    }

    private fun buildWeatherStateDO(weather: JSONObject, weatherDescription: JSONObject, wind: JSONObject) : WeatherStateDO {
        return WeatherStateDO(
                weatherDescription.getString("main"),
                weatherDescription.getString("description"),
                weatherDescription.getString("icon"),
                null,
                getDoubleFromJSONObject(weather, "temp"),
                getDoubleFromJSONObject(weather, "temp_min"),
                getDoubleFromJSONObject(weather, "temp_max"),
                getDoubleFromJSONObject(weather, "pressure"),
                getDoubleFromJSONObject(weather, "sea_level"),
                getDoubleFromJSONObject(weather, "grnd_level"),
                getDoubleFromJSONObject(weather, "humidity"),
                getDoubleFromJSONObject(wind, "speed"),
                getDoubleFromJSONObject(wind, "deg"),
                System.currentTimeMillis() / 1000L,
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
                getDoubleFromJSONObject(temp, "min"),
                getDoubleFromJSONObject(temp, "max"),
                getDoubleFromJSONObject(root, "pressure"),
                0.0,
                0.0,
                getDoubleFromJSONObject(root, "humidity"),
                getDoubleFromJSONObject(root, "speed"),
                getDoubleFromJSONObject(root, "deg"),
                System.currentTimeMillis() / 1000L,
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
    fun parseWeatherState(data: Cursor, cache: ICacheSystem<Bitmap>): WeatherStateDO {
        val weatherState: WeatherStateDO?
        // we are only interested in the first row
        if (data.moveToNext()) {
            val icon: Bitmap? = getIconFromId(data.getString(DbSchema.Weather.COLUMNS_ID.COL_ICON_ID.ordinal), cache)

            weatherState = WeatherStateDO(
                    data.getString(DbSchema.Weather.COLUMNS_ID.COL_MAIN_STATE.ordinal),
                    data.getString(DbSchema.Weather.COLUMNS_ID.COL_DESCRIPTION.ordinal),
                    data.getString(DbSchema.Weather.COLUMNS_ID.COL_ICON_ID.ordinal),
                    icon,
                    data.getDouble(DbSchema.Weather.COLUMNS_ID.COL_TEMPERATURE.ordinal),
                    data.getDouble(DbSchema.Weather.COLUMNS_ID.COL_TEMPERATURE_MIN.ordinal),
                    data.getDouble(DbSchema.Weather.COLUMNS_ID.COL_TEMPERATURE_MAX.ordinal),
                    data.getDouble(DbSchema.Weather.COLUMNS_ID.COL_PRESSURE.ordinal),
                    data.getDouble(DbSchema.Weather.COLUMNS_ID.COL_SEA_LEVEL.ordinal),
                    data.getDouble(DbSchema.Weather.COLUMNS_ID.COL_GROUND_LEVEL.ordinal),
                    data.getDouble(DbSchema.Weather.COLUMNS_ID.COL_HUMIDITY.ordinal),
                    data.getDouble(DbSchema.Weather.COLUMNS_ID.COL_WIND_SPEED.ordinal),
                    data.getDouble(DbSchema.Weather.COLUMNS_ID.COL_WIND_DEGREES.ordinal),
                    data.getLong(DbSchema.Weather.COLUMNS_ID.COL_LAST_UPDATE.ordinal),
                    data.getLong(DbSchema.Weather.COLUMNS_ID.COL_FORECAST_DATE.ordinal)
            )
        }
        // Create a dummy weather
        else {
            weatherState = WeatherStateDO("--", "--", "0", null, .0, .0, .0, .0, .0, .0, .0, .0, .0, 0, 0)
        }

        return weatherState
    }

    private fun getIconFromId(iconId: String, cache: ICacheSystem<Bitmap>): Bitmap? {
        val entry = cache.getItem(iconId)
        return entry.item
    }

    fun parseCitiesList(jsonObject: JSONObject) : List<CityDO> {
        val citiesList: ArrayList<CityDO> = ArrayList<CityDO>()

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
        val  weatherList = parseWeatherStateList(temp)
        // Build City DO
        val forecastDO: ForecastDO = ForecastDO(weatherList)
        return forecastDO
    }
}
