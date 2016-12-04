package com.isel.pdm.yawa


import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.preference.PreferenceManager
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
import com.isel.pdm.yawa.provider.WeatherContract

import org.json.JSONObject

class WeatherManager constructor(context: Context, val requester: IRequestParser) {

    private val context: Context
    //private var weatherState: WeatherStateDO? = null
    private var forecastState: ForecastDO? = null
    // true when when the app has completed at least one update of current weather
    private var updated = false
    // Counter to know when can update current weather's state
    private var elapsedTime: Long = 0
    // true when when the app has completed at least one update of forecast weather
    private var forecastUpdated = false
    // Counter to know when can update forecast weather's state
    private var forecastElapsedTime: Long = 0
    // default locations when there is no settings file
    val defaultLocation: String by lazy { context.resources.getString(R.string.default_location) }
    // key for locations on settings file
    val settingsLocationStr: String by lazy { context.resources.getString(R.string.settings_location_str) }

    init {
        this.context = context.applicationContext
    }

    companion object {
        private val MILLISECONDS = 1000L
        private val WEATHER_REQUEST_TAG = "weatherReq"
        private val WEATHER_SEARCH_CITY_TAG = "cityListReq"
        private val FORECAST_CITY_BY_ID_TAG = "forecastReq"
    }


    /**
     * Used to know when can just update an existing row or need to insert a new one
     */
    private fun countExisting(city: String, current: Int): Int {
        val projection = arrayOf<String>(
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

        val count = cursor.count
        cursor.close()

        return count
    }

    private fun saveCurrentWeatherIcon(image: Bitmap) {
        // TODO: guardar a imagem em BD diferente?
    }

    private fun saveCurrentWeather(weather: WeatherStateDO) {
        val CURRENT = 1
        val mNewValues = ContentValues()

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val city = sharedPref.getString(settingsLocationStr, defaultLocation)
        val selectionClause: String =
                WeatherContract.Weather.CITY_ID + " = ? AND " + WeatherContract.Weather.CURRENT + " = ?"
        val selectionArgs = arrayOf(city, CURRENT.toString())
        val currentTime = System.currentTimeMillis() / MILLISECONDS

        mNewValues.put(WeatherContract.Weather.LAST_UPDATE, currentTime)
        mNewValues.put(WeatherContract.Weather.DESCRIPTION, weather.description)
        mNewValues.put(WeatherContract.Weather.TEMPERATURE, weather.temp)
        mNewValues.put(WeatherContract.Weather.TEMPERATURE_MAX, weather.temp_max)
        mNewValues.put(WeatherContract.Weather.TEMPERATURE_MIN, weather.temp_min)

        if(countExisting(city, CURRENT) == 0) {
            mNewValues.put(WeatherContract.Weather.CITY_ID, city)
            mNewValues.put(WeatherContract.Weather.CURRENT, CURRENT)
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

    fun updateCurrentWeather(callbackSet : ICallbackSet?) {
        val url = URLTranslator.getCurrentWeatherURL(this.context)

        val jsObjRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener<org.json.JSONObject> { response ->
            val weatherState = OpenWeatherParser.parseWeatherState(response)
            // Stores the current weather on the content provider
            saveCurrentWeather(weatherState)
            // Get the icon
            // TODO: Possivelmente tem de se passar as imagens para um BD independente para podermos fazer cache
            // TODO: por isso, para já guarda-se na tabela numa BLOB
            this@WeatherManager.getWeatherIcon(weatherState.weatherIconID,
                    object: ICallbackSet {
                        override fun onError(error: VolleyError) {
                            callbackSet?.onError(error)
                        }
                        override fun onSucceed(response: Any?) {
                            if(response != null)
                                saveCurrentWeatherIcon(response as Bitmap)
                            callbackSet?.onSucceed(weatherState)
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

    private fun getWeatherIcon(iconID: String, callbackSet : ICallbackSet) {
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

    private fun setForecastWeather(jsonObject: JSONObject) {
        forecastState = OpenWeatherParser.parseForecastCity(jsonObject)
    }

    private fun setForegroundWeatherIcon(bitmap: Bitmap, index: Int) {
        forecastState?.weatherStateDOList?.get(index)?.weatherIcon = bitmap
    }

    /**
     * Get icons bitmap. Only call onSucceed when last == true
     */
    private fun getWeatherIconForForecast(iconID: String, callbackSet : ICallbackSet, index: Int) {
        val imageLoader = this.requester.getImgLoader()

        val url = URLTranslator.getWeatherIconURL(this.context, iconID)
        imageLoader.get(
                url,
                object : ImageLoader.ImageListener {
                    override fun onResponse(response: ImageLoader.ImageContainer, isImmediate: Boolean) {
                        response.bitmap?.let { setForegroundWeatherIcon(response.bitmap, index) }
                        callbackSet.onSucceed( this@WeatherManager.forecastState!! )
                    }

                    override fun onErrorResponse(error: VolleyError) {
                        Log.e(YAWA.YAWA_ERROR_TAG, "Error on 'getWeatherIconForForecast()'.\n" + error.message)
                        //TODO: set an error image
                        //WeatherManager.this.weatherState.setWeatherIcon(R.drawable.err_image);
                        callbackSet.onError(error)
                    }
                })
    }

    /**
     * Updates the forecast weather.
     * To get all the weather icons it calls getWeatherIconForForecast() for each day
     * city - Ex.: Lisbon,pt
     */
    private fun updateForecastWeather(city: String, callbackSet: ICallbackSet) {
        val url = URLTranslator.getForecastWeather(this.context, city)

        val jsObjRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener<org.json.JSONObject> { response ->
            setForecastWeather(response)
            // Get all the weather icons bitmap for each day in forecast
            var idx = 0
            for(weatherDO in forecastState?.weatherStateDOList!!) {
                getWeatherIconForForecast(weatherDO.weatherIconID, callbackSet, idx)
                idx++
            }
        }, Response.ErrorListener { error ->
            Log.e(YAWA.YAWA_ERROR_TAG, "Error on 'updateForecastWeather()'.\n" + error.message)
            callbackSet.onError(error)
        })

        // add a TAG to easily cancel the request if necessary
        jsObjRequest.tag = FORECAST_CITY_BY_ID_TAG
        //
        this.requester.addRequest(jsObjRequest)
    }

    fun getForecastWeather(city : String, callbackSet : ICallbackSet){
        if (SystemClock.elapsedRealtime() - this.forecastElapsedTime < 60000) {
            if (this.forecastUpdated) {
                callbackSet.onSucceed(this.forecastState!!)
            }

            return
        }
        updateForecastWeather(city, callbackSet)
    }

    /**
     * Force the update
     */
    fun refreshForecastWeather(city : String, callbackSet : ICallbackSet) {
        updateForecastWeather(city, callbackSet)
    }

    /**
     * Set the weather to a given arg.
     * Useful when the user select a new city - the search by city already have the weather
     *
     * Note: field weatherIcon may be null
     */
    fun setWeather(weather: WeatherStateDO) {
        //weatherState = weather

        if (weather.weatherIcon == null) {
            getWeatherIcon(weather.weatherIconID,
                    object : ICallbackSet {
                        override fun onError(error: VolleyError) {
                        }

                        override fun onSucceed(response: Any?) {
                        }
                    })
        }
    }

    /**
     *
     */
    fun getLocalForecastWeather(): ForecastDO {
        return forecastState!!
    }

    /**
     * Used to clean forecast cache when a new city is configured.
     * Cleaning the cache and reset its state forces the update next time getForecastByCityId is called
     */
    fun onChangeCity() {
        forecastState = null
        forecastUpdated = false
        forecastElapsedTime = 0
    }
}
