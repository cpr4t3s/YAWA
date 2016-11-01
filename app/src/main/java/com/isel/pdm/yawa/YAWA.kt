package com.isel.pdm.yawa

import android.app.Application
import com.isel.pdm.yawa.openweather_tools.OpenWeatherRequester

class YAWA : Application() {
    val weatherManager by lazy { WeatherManager(this, OpenWeatherRequester(this)) }
    // default locations when there is no settings file
    val defaultLocation: String by lazy { resources.getString(R.string.default_location) }
    // key for locations on settings file
    val settingsLocationStr: String by lazy { resources.getString(R.string.settings_location_str) }
    //
    val defaultCityId: String by lazy { resources.getString(R.string.default_cityID) }
    // key for locations on settings file
    val settingsCityIDStr: String by lazy { resources.getString(R.string.settings_city_id_str) }


    override fun onCreate() {
        super.onCreate()
    }
}

val Application.weatherManager : WeatherManager
    get() = (this as YAWA).weatherManager


val Application.defaultLocation: String
    get() = (this as YAWA).defaultLocation

val Application.settingsLocationStr: String
    get() = (this as YAWA).settingsLocationStr

val Application.settingsCityIDStr: String
    get() = (this as YAWA).settingsCityIDStr

val Application.defaultCityId: String
    get() = (this as YAWA).defaultCityId