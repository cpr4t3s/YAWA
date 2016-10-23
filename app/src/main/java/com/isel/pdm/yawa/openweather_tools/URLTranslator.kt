package com.isel.pdm.yawa.openweather_tools

import android.content.Context

import com.isel.pdm.yawa.R

import java.text.MessageFormat
import java.util.Locale

object URLTranslator {
    private val WEATHER_ICON_URL_TEMPLATE_ID = R.string.weather_state_icon_ulr
    //
    private val CURRENT_WEATHER_URL_TEMPLATE_ID = R.string.url_request
    private val API_VERSION_TEMPLATE_ID = R.string.api_version
    private val API_KEY_TEMPLATE_ID = R.string.api_key
    private val SEARCH_BY_CITY_NAME_URL_TEMPLATE_ID = R.string.get_search_cities_url
    private val FORCAST_BY_CITY_ID_URL_TEMPLATE = R.string.get_forcast_city_url
    private val NUMBER_OF_DAYS_TO_FORCAST =  R.string.get_forcast_days

    fun getWeatherIconURL(context: Context, iconID: String): String {
        return MessageFormat.format(
                context.resources.getString(WEATHER_ICON_URL_TEMPLATE_ID),
                *arrayOf(iconID))
    }

     // TODO: remove duplicate values

    fun getCurrentWeatherURL(context: Context): String {
        // TODO: by lazy
        val urlTemplate = context.resources.getString(CURRENT_WEATHER_URL_TEMPLATE_ID)
        val apiVersion = context.resources.getString(API_VERSION_TEMPLATE_ID)
        val apiKey = context.resources.getString(API_KEY_TEMPLATE_ID)
        val lang = Locale.getDefault().language

        // get location
        val settingsFileName = context.resources.getString(R.string.settings_filename)
        val defaultLocation = context.resources.getString(R.string.default_location)
        val settingsLocationStr = context.resources.getString(R.string.settings_location_str)
        val settings = context.getSharedPreferences(settingsFileName, Context.MODE_PRIVATE)
        val location = settings.getString(settingsLocationStr, defaultLocation)

        // set units
        val settingsUnitsStr = context.resources.getString(R.string.settings_units_str)
        val defaultUnits = context.resources.getString(R.string.default_units)
        val units = settings.getString(settingsUnitsStr, defaultUnits)

        return MessageFormat.format(urlTemplate, *arrayOf(apiVersion, location, apiKey, lang, units))
    }

    fun getSearchCitiesURL(context: Context, searchCity: String): String {

        val urlTemplate = context.resources.getString(SEARCH_BY_CITY_NAME_URL_TEMPLATE_ID)
        val apiVersion = context.resources.getString(API_VERSION_TEMPLATE_ID)
        val apiKey = context.resources.getString(API_KEY_TEMPLATE_ID)
        val lang = Locale.getDefault().language

        //
        val settingsFileName = context.resources.getString(R.string.settings_filename)
        val settings = context.getSharedPreferences(settingsFileName, Context.MODE_PRIVATE)

        // set units
        val settingsUnitsStr = context.resources.getString(R.string.settings_units_str)
        val defaultUnits = context.resources.getString(R.string.default_units)
        val units = settings.getString(settingsUnitsStr, defaultUnits)

        return MessageFormat.format(urlTemplate, *arrayOf(apiVersion, searchCity, apiKey, lang, units))
    }

    fun getForecastCityById(context: Context, searchCityId: String): String {

        val urlTemplate = context.resources.getString(FORCAST_BY_CITY_ID_URL_TEMPLATE)
        val forcastDays = context.resources.getString(NUMBER_OF_DAYS_TO_FORCAST)
        val apiVersion = context.resources.getString(API_VERSION_TEMPLATE_ID)
        val apiKey = context.resources.getString(API_KEY_TEMPLATE_ID)
        val lang = Locale.getDefault().language

        //
        val settingsFileName = context.resources.getString(R.string.settings_filename)
        val settings = context.getSharedPreferences(settingsFileName, Context.MODE_PRIVATE)

        // set units
        val settingsUnitsStr = context.resources.getString(R.string.settings_units_str)
        val defaultUnits = context.resources.getString(R.string.default_units)
        val units = settings.getString(settingsUnitsStr, defaultUnits)

        return MessageFormat.format(urlTemplate, *arrayOf(apiVersion, searchCityId,forcastDays, apiKey, lang, units))
    }
}

