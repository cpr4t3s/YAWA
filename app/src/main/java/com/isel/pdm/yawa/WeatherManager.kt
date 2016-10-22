package com.isel.pdm.yawa


import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.openweather_tools.OpenWeatherParser
import com.isel.pdm.yawa.openweather_tools.OpenWeatherRequester
import com.isel.pdm.yawa.openweather_tools.URLTranslator

import org.json.JSONObject

class WeatherManager constructor(context: Context, val requester: IRequestParser) {

    private val context: Context
    private var weatherState: WeatherStateDO? = null
    // true when when the app has completed at least one update
    private var updated = false
    // Counter to know when can update weather's state
    private var elapsedTime: Long = 0

    init {
        this.context = context.applicationContext
    }

    companion object {
        private val MILLISECONDS = 1000
        private val UPDATE_INTERVAL = 3600 * MILLISECONDS // one hour
        private val WEATHER_REQUEST_TAG = "weatherReq"
        private val WEATHER_SEARCH_CITY_TAG = "cityListReq"
    }

    /**
     * Update manager's internal state after every (onSucceed and onError) update.
     * It's called even after onError to avoid continuous tries after UPDATE_INTERVAL without updates
     */
    private fun updateInternalState() {
        this.elapsedTime = SystemClock.elapsedRealtime()
    }

    /**
     * Only called when the update has succeed
     */
    private fun updateInternalStateOnSucceed() {
        this.updated = true
    }

    private fun setWeatherState(jsonObject: JSONObject) {
        this.weatherState = OpenWeatherParser.parseWeatherState(jsonObject)
    }

    private fun setWeatherIcon(bitmap: Bitmap) {
        // Don't need to check weatherState nullability because setWeatherState is called first
        OpenWeatherParser.setWeatherIcon(bitmap, this.weatherState!!)
    }

    private fun updateCurrentWeather(callbackSet : ICallbackSet) {
        val url = URLTranslator.getCurrentWeatherURL(this.context)

        val jsObjRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener<org.json.JSONObject> { response ->
            this@WeatherManager.setWeatherState(response)
            // Get the icon
            this@WeatherManager.getWeatherIcon(this@WeatherManager.weatherState!!.weatherIconID!!, callbackSet)
        }, Response.ErrorListener { error ->
            // update internal state
            updateInternalState()

            callbackSet.onError(error)
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
                        response.bitmap?.let { setWeatherIcon(response.bitmap) }
                        // update internal state
                        updateInternalState()
                        // the update has succeed!
                        updateInternalStateOnSucceed()

                        callbackSet.onSucceed(this@WeatherManager.weatherState!!)
                    }

                    override fun onErrorResponse(error: VolleyError) {
                        // update internal state
                        updateInternalState()

                        //TODO: set an error image
                        //WeatherManager.this.weatherState.setWeatherIcon(R.drawable.err_image);
                        callbackSet.onError(error)
                    }
                })
    }

    /**
     * Only update the state if its necessary - first call or past UPDATE_INTERVAL.
     * Call 'onSucceed' callback if made an update ant it succeed OR an update succeed in the past
     * The last one is useful on screen orientation changes
     */
    fun getCurrentWeather(callbackSet : ICallbackSet) {
        if (SystemClock.elapsedRealtime() - this.elapsedTime < UPDATE_INTERVAL) {
            if (this.updated) {
                callbackSet.onSucceed(this.weatherState!!)
            }

            return
        }

        this.updateCurrentWeather(callbackSet)
    }

    /**
     * Force the update. Normally used when the user explicitly wants to refresh the state
     */
    fun refreshCurrentWeather(callbackSet : ICallbackSet) {
        this.updateCurrentWeather(callbackSet)
    }

    fun cancelAllRequests() {
        this.requester.cancelAllRequests()
    }

    fun searchCityByName(cityName : String, callbackSet : ICallbackSet) {
        val url = URLTranslator.getSearchCitiesURL(this.context,cityName)

        val jsObjRequest = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener<org.json.JSONObject> { response ->
            callbackSet.onSucceed( OpenWeatherParser.parseCitiesList(response) )

        }, Response.ErrorListener { error ->
            // update internal state
            updateInternalState()

            callbackSet.onError(error)
        })

        // add a TAG to easily cancel the request if necessary
        jsObjRequest.tag = WEATHER_SEARCH_CITY_TAG
        //
        this.requester.addRequest(jsObjRequest)
    }

    /**
     * Set the weather to a given arg.
     * Useful when the user select a new city - the search by city already have the weather
     *
     * Note: field weatherIcon may be null
     */
    fun setWeather(weather: WeatherStateDO) {
        weatherState = weather

        if (weather.weatherIcon == null) {
            getWeatherIcon(weather.weatherIconID,
                    object : ICallbackSet {
                        override fun onError(error: VolleyError) {
                        }

                        override fun onSucceed(response: Any) {
                        }
                    }
            )
        }
    }
}
