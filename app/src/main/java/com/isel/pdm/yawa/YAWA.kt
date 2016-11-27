package com.isel.pdm.yawa

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.preference.PreferenceManager
import com.isel.pdm.yawa.service.WeatherService
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
    //
    var autoRefreshEnabled: Boolean = true
    var amIntent: PendingIntent? = null

    fun registerServiceOnAlarmManager() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val settingsRefreshRateStr = this.resources?.getString(R.string.settings_refresh_rate_str)
        val defaultRefreshRate = this.resources?.getString(R.string.default_refresh_rate)
        val refreshRate = sharedPref.getString(settingsRefreshRateStr, defaultRefreshRate!!)?.toLong()

        val intent = Intent(this, WeatherService::class.java)
        amIntent = PendingIntent.getService(this, 0, intent, 0)
        val alarm = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
                //refreshRate!! * 1000 * 60, amIntent)
                10 * 1000, amIntent)
    }

    /**
     * Get the configuration of Auto-Refresh on app bootup
     */
    private fun setInitialRefreshPolicy(prefs: SharedPreferences) {
        val settingsAutoRefreshStr = this.resources.getString(R.string.settings_auto_refresh)
        val autoRefreshPolicy = prefs.getBoolean(settingsAutoRefreshStr, true)

        autoRefreshEnabled = autoRefreshPolicy
        if(autoRefreshEnabled) registerServiceOnAlarmManager()
    }

    private fun setAutoRefreshAfterPrefChange(enabled: Boolean) {
        autoRefreshEnabled = enabled
        if(enabled) {
            registerServiceOnAlarmManager()
        } else {
            val alarm = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.cancel(amIntent)
        }

        println("Auto Refresh enabled: " + enabled)
        // TODO: meter aqui o codigo para fazer reset as definicoes do manifest para ignorar o
        // TODO: evento do boot completed caso as actualizações automaticas nao estejam habilitadas
    }

    override fun onCreate() {
        super.onCreate()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setInitialRefreshPolicy(prefs)
        prefs.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            run {
                val settingsAutoRefreshStr = this.resources?.getString(R.string.settings_auto_refresh)

                // aqui podemos ignorar os default values porque se foi alterado, já existe
                if(key.equals(settingsAutoRefreshStr)) setAutoRefreshAfterPrefChange(sharedPreferences.
                        getBoolean(key, true))
            }
        }

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

val Application.autoRefreshEnabled: Boolean
    get() = (this as YAWA).autoRefreshEnabled