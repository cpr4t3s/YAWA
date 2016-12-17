package com.isel.pdm.yawa


import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.preference.PreferenceManager
import android.provider.BaseColumns
import android.util.Log

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.isel.pdm.yawa.DataContainers.ForecastDO
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.openweather_tools.OpenWeatherParser
import com.isel.pdm.yawa.openweather_tools.URLTranslator
import com.isel.pdm.yawa.provider.DbSchema
import com.isel.pdm.yawa.provider.WeatherContract



class WeatherManager constructor(context: Context, val requester: IRequestParser) {

    private val context: Context
    // default locations when there is no shared preference
    val defaultLocation: String by lazy { context.resources.getString(R.string.default_location) }
    //
    val defaultForecastDays: String by lazy { context.resources.getString(R.string.default_forecast_days) }
    // key for locations in shared preferences
    val settingsLocationStr: String by lazy { context.resources.getString(R.string.settings_location_str) }
    // key for forecast days in shared preferences
    val settingsForecastDays: String by lazy { context.resources.getString(R.string.settings_forecast_days_str) }

    init {
        this.context = context.applicationContext
    }

    companion object {
        private val WEATHER_REQUEST_TAG = "weatherReq"
        private val WEATHER_SEARCH_CITY_TAG = "cityListReq"
        private val FORECAST_CITY_BY_ID_TAG = "forecastReq"
    }


    /**
     * Used to know when can just update an existing row or need to insert a new one
     */
    private fun countExisting(city: String, current: Int): List<Int> {
        val existingIds: MutableList<Int> = mutableListOf<Int>()
        val projection = arrayOf<String>(
                BaseColumns._ID,
                WeatherContract.Weather.CITY_ID,
                WeatherContract.Weather.CURRENT
        )
        val selectionClause: String =
                WeatherContract.Weather.CITY_ID + " = ? AND " + WeatherContract.Weather.CURRENT + " = ?"
        val selectionArgs = arrayOf(city, current.toString())
        //
        val cursor = context.contentResolver.query(
                WeatherContract.Weather.CONTENT_URI,
                projection,
                selectionClause,
                selectionArgs,
                WeatherContract.Weather.DEFAULT_SORT_ORDER)

        while(cursor.moveToNext()) {
            existingIds.add(cursor.getInt(DbSchema.Weather.COLUMNS_ID.COL_ID.ordinal))
        }
        cursor.close()

        return existingIds
    }

    private fun saveCurrentWeatherIcon(image: Bitmap) {
        // TODO
    }

    private fun saveCurrentWeather(weather: WeatherStateDO) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val city = sharedPref.getString(settingsLocationStr, defaultLocation)
        val selectionClause: String =
                WeatherContract.Weather.CITY_ID + " = ? AND " + WeatherContract.Weather.CURRENT + " = ?"
        val selectionArgs = arrayOf(city, YAWA.CURRENT_WEATHER_FLAG.toString())

        val mNewValues = buildCommonNewValues(weather)

        if(countExisting(city, YAWA.CURRENT_WEATHER_FLAG).size == 0) {
            mNewValues.put(WeatherContract.Weather.CITY_ID, city)
            mNewValues.put(WeatherContract.Weather.CURRENT, YAWA.CURRENT_WEATHER_FLAG)
            context.contentResolver.insert(
                    WeatherContract.Weather.CONTENT_URI,
                    mNewValues
            )
        } else {
            context.contentResolver.update(
                    WeatherContract.Weather.CONTENT_URI,
                    mNewValues,
                    selectionClause,
                    selectionArgs
            )
        }
    }

    private fun buildCommonNewValues(weatherState: WeatherStateDO): ContentValues {
        val mNewValues = ContentValues()

        mNewValues.put(WeatherContract.Weather.LAST_UPDATE, weatherState.updateDate)
        mNewValues.put(WeatherContract.Weather.FORECAST_DATE, weatherState.forecastDate)
        mNewValues.put(WeatherContract.Weather.DESCRIPTION, weatherState.description)
        mNewValues.put(WeatherContract.Weather.TEMPERATURE, weatherState.temp)
        mNewValues.put(WeatherContract.Weather.TEMPERATURE_MAX, weatherState.temp_max)
        mNewValues.put(WeatherContract.Weather.TEMPERATURE_MIN, weatherState.temp_min)
        mNewValues.put(WeatherContract.Weather.MAIN_STATE, weatherState.mainState)
        mNewValues.put(WeatherContract.Weather.ICON_ID, weatherState.weatherIconID)
        mNewValues.put(WeatherContract.Weather.PRESSURE, weatherState.pressure)
        mNewValues.put(WeatherContract.Weather.SEA_LEVEL, weatherState.seaLevel)
        mNewValues.put(WeatherContract.Weather.GROUND_LEVEL, weatherState.groundLevel)
        mNewValues.put(WeatherContract.Weather.HUMIDITY, weatherState.humidity)
        mNewValues.put(WeatherContract.Weather.WIND_SPEED, weatherState.windSpeed)
        mNewValues.put(WeatherContract.Weather.WIND_DEGREES, weatherState.windDegree)

        return mNewValues
    }

    private fun saveForecastWeather(forecastWeather: ForecastDO) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val city = sharedPref.getString(settingsLocationStr, defaultLocation)
        val selectionClause: String =
                BaseColumns._ID + " = ? AND " + WeatherContract.Weather.CITY_ID + " = ? AND " + WeatherContract.Weather.CURRENT + " = ?"
        val forecastDays: Int = sharedPref.getInt(settingsForecastDays, defaultForecastDays.toInt())

        val existingEntries: List<Int> = countExisting(city, YAWA.FORECAST_WEATHER_FLAG)
        val toUpdate: Int = if(forecastDays <= existingEntries.size) forecastDays else existingEntries.size

        for(i in 0 until toUpdate) {
            val mNewValues = buildCommonNewValues(forecastWeather.weatherStateDOList[i])
            val _id: Int = existingEntries[i]
            val selectionArgs = arrayOf(_id.toString(), city, YAWA.FORECAST_WEATHER_FLAG.toString())
            context.contentResolver.update(
                    WeatherContract.Weather.CONTENT_URI,
                    mNewValues,
                    selectionClause,
                    selectionArgs
            )
        }

        for(i in toUpdate until forecastDays) {
            val mNewValues = buildCommonNewValues(forecastWeather.weatherStateDOList[i])

            mNewValues.put(WeatherContract.Weather.CITY_ID, city)
            mNewValues.put(WeatherContract.Weather.CURRENT, YAWA.FORECAST_WEATHER_FLAG)
            context.contentResolver.insert(
                    WeatherContract.Weather.CONTENT_URI,
                    mNewValues
            )
        }
    }

    fun updateCurrentWeather(callbackSet : ICallbackSet?) {
        val url = URLTranslator.getCurrentWeatherURL(this.context)

        val jsObjRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener<org.json.JSONObject> { response ->
            val weatherState = OpenWeatherParser.parseWeatherState(response)
            // Stores the current weather on the content provider
            saveCurrentWeather(weatherState)
            callbackSet?.onSucceed(weatherState)
            // Get the icon
            // TODO: Possivelmente tem de se passar as imagens para um BD independente para podermos fazer cache
            // TODO: corrigir depois de se saber como serão as caches
            this@WeatherManager.getWeatherIcon(weatherState.weatherIconID,
                    object: ICallbackSet {
                        override fun onError(error: VolleyError) {
                            Log.e(YAWA.YAWA_ERROR_TAG, error.message)
                        }
                        override fun onSucceed(response: Any?) {
                            if(response != null)
                                saveCurrentWeatherIcon(response as Bitmap)
                        }

                    })
        }, Response.ErrorListener { error ->
            Log.e(YAWA.YAWA_ERROR_TAG, "Error on 'updateCurrentWeather()'.\n" + error.message)
            callbackSet?.onError(error)
        })

        // add a TAG to easily cancel the request if necessary
        jsObjRequest.tag = WEATHER_REQUEST_TAG
        //
        this.requester.addRequest(jsObjRequest)
    }

    fun getWeatherIcon(iconID: String, callbackSet : ICallbackSet) {
        val imageLoader = this.requester.getImgLoader()
        val url = URLTranslator.getWeatherIconURL(this.context, iconID)
        imageLoader.get(
                url,
                object : ImageLoader.ImageListener {
                    override fun onResponse(response: ImageLoader.ImageContainer, isImmediate: Boolean) {
                        callbackSet.onSucceed(response.bitmap)
                    }

                    override fun onErrorResponse(error: VolleyError) {
                        Log.e(YAWA.YAWA_ERROR_TAG, "Error on 'getWeatherIcon()'.\n" + error.message)
                        //TODO: set an error image
                        //WeatherManager.this.weatherState.setWeatherIcon(R.drawable.err_image);
                        callbackSet.onError(error)
                    }
                })
    }

    fun cancelAllRequests() {
        this.requester.cancelAllRequests()
    }

    fun searchCityByName(cityName : String, callbackSet : ICallbackSet) {
        val url = URLTranslator.getSearchCitiesURL(this.context,cityName)

        val jsObjRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener<org.json.JSONObject> { response ->
            callbackSet.onSucceed( OpenWeatherParser.parseCitiesList(response) )

        }, Response.ErrorListener { error ->
            callbackSet.onError(error)
        })

        // add a TAG to easily cancel the request if necessary
        jsObjRequest.tag = WEATHER_SEARCH_CITY_TAG
        //
        this.requester.addRequest(jsObjRequest)
    }

    private fun setForegroundWeatherIcon(bitmap: Bitmap, index: Int) {
        //TODO: cache
        //forecastState?.weatherStateDOList?.get(index)?.weatherIcon = bitmap
    }

    /**
     * Get icons bitmap. Only call onSucceed when last == true
     */
    private fun getWeatherIconForForecast(iconID: String, callbackSet : ICallbackSet, index: Int) {
        //TODO: cache
//        val imageLoader = this.requester.getImgLoader()
//
//        val url = URLTranslator.getWeatherIconURL(this.context, iconID)
//        imageLoader.get(
//                url,
//                object : ImageLoader.ImageListener {
//                    override fun onResponse(response: ImageLoader.ImageContainer, isImmediate: Boolean) {
//                        response.bitmap?.let { setForegroundWeatherIcon(response.bitmap, index) }
//                        callbackSet.onSucceed( this@WeatherManager.forecastState!! )
//                    }
//
//                    override fun onErrorResponse(error: VolleyError) {
//                        Log.e(YAWA.YAWA_ERROR_TAG, "Error on 'getWeatherIconForForecast()'.\n" + error.message)
//                        //TODO: set an error image
//                        //WeatherManager.this.weatherState.setWeatherIcon(R.drawable.err_image);
//                        callbackSet.onError(error)
//                    }
//                })
    }

    /**
     * Updates the forecast weather.
     * To get all the weather icons it calls getWeatherIconForForecast() for each day
     * city - Ex.: Lisbon,pt
     */
    fun updateForecastWeather(callbackSet: ICallbackSet?) {
        val url = URLTranslator.getForecastWeather(this.context)

        val jsObjRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener<org.json.JSONObject> { response ->
            val forecastWeather = OpenWeatherParser.parseForecastCity(response)

            // Stores the forecast weather on the content provider
            saveForecastWeather(forecastWeather)
            callbackSet?.onSucceed(forecastWeather)

            // TODO: corrigir depois de saber como devem ser as caches
            // Get all the weather icons bitmap for each day in forecast
//            var idx = 0
//            for(weatherDO in forecastState?.weatherStateDOList!!) {
//                getWeatherIconForForecast(weatherDO.weatherIconID, callbackSet, idx)
//                idx++
//            }
        }, Response.ErrorListener { error ->
            Log.e(YAWA.YAWA_ERROR_TAG, "Error on 'updateForecastWeather()'.\n" + error.message)
            callbackSet?.onError(error)
        })

        // add a TAG to easily cancel the request if necessary
        jsObjRequest.tag = FORECAST_CITY_BY_ID_TAG
        //
        this.requester.addRequest(jsObjRequest)
    }

    fun getSpecificForecastDay(city: String, id: Int): WeatherStateDO {

        val selectionClause: String =
                WeatherContract.Weather.CITY_ID + " = ? AND " + WeatherContract.Weather.CURRENT + " = ?"
        val selectionArgs = arrayOf(city, YAWA.FORECAST_WEATHER_FLAG.toString())
        //
        val cursor = context.contentResolver.query(
                WeatherContract.Weather.CONTENT_URI,
                WeatherContract.Weather.SELECT_ALL,
                selectionClause,
                selectionArgs,
                WeatherContract.Weather.DEFAULT_SORT_ORDER)

        // set the cursor in the right position
        cursor.moveToPosition(id)
        val weatherState = OpenWeatherParser.parseWeatherState(cursor)
        cursor.close()

        return weatherState
    }
}
