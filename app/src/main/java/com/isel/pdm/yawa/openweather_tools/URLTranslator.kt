package com.isel.pdm.yawa.openweather_tools

import android.content.Context
import android.preference.PreferenceManager

import com.isel.pdm.yawa.R
import java.net.URLEncoder

import java.text.MessageFormat
import java.util.Locale

object URLTranslator {
    private val WEATHER_ICON_URL_TEMPLATE_ID = R.string.weather_state_icon_ulr
    private val CURRENT_WEATHER_URL_TEMPLATE_ID = R.string.url_request
    private val API_VERSION_TEMPLATE_ID = R.string.api_version
    private val API_KEY_TEMPLATE_ID = R.string.api_key
    private val SEARCH_BY_CITY_NAME_URL_TEMPLATE_ID = R.string.get_search_cities_url
    private val FORCAST_BY_CITY_ID_URL_TEMPLATE = R.string.get_forcast_city_url
    private val WEATHER_BY_COORD_ID_URL_TEMPLATE = R.string.get_weather_by_coord_url

    fun getWeatherIconURL(context: Context, iconID: String): String {
        return MessageFormat.format(
                context.resources.getString(WEATHER_ICON_URL_TEMPLATE_ID),
                *arrayOf(iconID))
    }

    fun getCurrentWeatherURL(context: Context): String {
        val urlTemplate = context.resources.getString(CURRENT_WEATHER_URL_TEMPLATE_ID)
        val apiVersion = context.resources.getString(API_VERSION_TEMPLATE_ID)
        val apiKey = context.resources.getString(API_KEY_TEMPLATE_ID)
        val lang = Locale.getDefault().language
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)


        // get location
        val defaultLocation = context.resources.getString(R.string.default_location)
        val settingsLocationStr = context.resources.getString(R.string.settings_location_str)
        val location = URLEncoder.encode(sharedPref.getString(settingsLocationStr, defaultLocation),"UTF-8")

        // set units
        val settingsUnitsStr = context.resources.getString(R.string.settings_units_str)
        val defaultUnits = context.resources.getString(R.string.default_units)
        val units = sharedPref.getString(settingsUnitsStr, defaultUnits)

        return MessageFormat.format(urlTemplate, *arrayOf(apiVersion, location, apiKey, lang, units))
    }

    fun getSearchCitiesURL(context: Context, searchCity: String): String {

        val urlTemplate = context.resources.getString(SEARCH_BY_CITY_NAME_URL_TEMPLATE_ID)
        val apiVersion = context.resources.getString(API_VERSION_TEMPLATE_ID)
        val apiKey = context.resources.getString(API_KEY_TEMPLATE_ID)
        val lang = Locale.getDefault().language
        //
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)

        // set units
        val settingsUnitsStr = context.resources.getString(R.string.settings_units_str)
        val defaultUnits = context.resources.getString(R.string.default_units)
        val units = sharedPref.getString(settingsUnitsStr, defaultUnits)

        return MessageFormat.format(urlTemplate, *arrayOf(apiVersion, searchCity, apiKey, lang, units))
    }

    fun getForecastWeather(context: Context): String {
        val urlTemplate = context.resources.getString(FORCAST_BY_CITY_ID_URL_TEMPLATE)

        val apiVersion = context.resources.getString(API_VERSION_TEMPLATE_ID)
        val apiKey = context.resources.getString(API_KEY_TEMPLATE_ID)
        val lang = Locale.getDefault().language

        //
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)

        // set units
        val settingsUnitsStr = context.resources.getString(R.string.settings_units_str)
        val defaultUnits = context.resources.getString(R.string.default_units)
        val units = sharedPref.getString(settingsUnitsStr, defaultUnits)

        // location
        val defaultLocation = context.resources.getString(R.string.default_location)
        val settingsLocationStr = context.resources.getString(R.string.settings_location_str)
        val location = URLEncoder.encode(sharedPref.getString(settingsLocationStr, defaultLocation),"UTF-8")

        // set days
        val settingsForecastDays = context.resources.getString(R.string.settings_forecast_days_str)
        val defaultForecastDays = context.resources.getString(R.string.default_forecast_days).toInt()
        val settingsDays = sharedPref.getInt(settingsForecastDays,defaultForecastDays)

        return MessageFormat.format(urlTemplate, *arrayOf(apiVersion, location, settingsDays, apiKey, lang, units))
    }

    fun getWeatherByCoordURL(lat: Double, lon: Double, context: Context): String {
        val urlTemplate: String = context.resources.getString(WEATHER_BY_COORD_ID_URL_TEMPLATE)

        val apiVersion = context.resources.getString(API_VERSION_TEMPLATE_ID)
        val apiKey = context.resources.getString(API_KEY_TEMPLATE_ID)
        val lang = Locale.getDefault().language

        //
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        // set units
        val settingsUnitsStr = context.resources.getString(R.string.settings_units_str)
        val defaultUnits = context.resources.getString(R.string.default_units)
        val units = sharedPref.getString(settingsUnitsStr, defaultUnits)

        return MessageFormat.format(urlTemplate, *arrayOf(apiVersion, lat, lon, apiKey, lang, units)).replace(",", ".")
    }
}

