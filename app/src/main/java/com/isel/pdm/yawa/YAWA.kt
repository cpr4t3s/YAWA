package com.isel.pdm.yawa

import android.app.Application
import com.isel.pdm.yawa.openweather_tools.OpenWeatherRequester

class YAWA : Application() {
    val weatherManager by lazy { WeatherManager(this, OpenWeatherRequester(this)) }
    // the name for shared preferences for app settings
    val settingsFileName: String by lazy { resources.getString(R.string.settings_filename) }
    // default locations when there is no settings file
    val defaultLocation: String by lazy { resources.getString(R.string.default_location) }
    // key for locations on settings file
    val settingsLocationStr: String by lazy { resources.getString(R.string.settings_location_str) }

    val settingsCityId: String by lazy { resources.getString(R.string.default_cityID) }


    override fun onCreate() {
        super.onCreate()
    }
}

val Application.weatherManager : WeatherManager
    get() = (this as YAWA).weatherManager

val Application.settingsFileName: String
    get() = (this as YAWA).settingsFileName

val Application.defaultLocation: String
    get() = (this as YAWA).defaultLocation

val Application.settingsLocationStr: String
    get() = (this as YAWA).settingsLocationStr

val Application.settingsCityId: String
    get() = (this as YAWA).settingsCityId