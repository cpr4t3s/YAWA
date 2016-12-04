package com.isel.pdm.yawa

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.SystemClock
import android.preference.PreferenceManager
import android.util.Log
import com.isel.pdm.yawa.service.WeatherService
import com.isel.pdm.yawa.openweather_tools.OpenWeatherRequester
import com.isel.pdm.yawa.service.BootCompleteReceiver

class YAWA : Application() {
    companion object {
        val YAWA_ERROR_TAG = "!!! YAWA Erro"
        val YAWA_WARN_TAG = "!!! YAWA Wanr"
        val YAWA_INFO_TAG = "> YAWA Info"
        //
        val UPDATE_CURRENT_WEATHER_ACTION = "com.isel.pdm.yawa.UPDATE_CURRENT_WEATHER"
    }

    val weatherManager by lazy { WeatherManager(this, OpenWeatherRequester(this)) }
    // default locations when there is no settings file
    val defaultLocation: String by lazy { resources.getString(R.string.default_location) }
    // key for locations on settings file
    val settingsLocationStr: String by lazy { resources.getString(R.string.settings_location_str) }
    //
    var autoRefreshEnabled: Boolean = true
    //
    //val alarmManager by lazy {applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager}
    val serviceAlarmId = 0


    fun tryRegisterServiceOnAlarmManager() {
        val settingsRefreshRateStr = this.resources?.getString(R.string.settings_refresh_rate_str)
        val defaultRefreshRate = this.resources?.getString(R.string.default_refresh_rate)
        val refreshRate = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(settingsRefreshRateStr, defaultRefreshRate!!)?.toLong()


        // don't duplicate the alarm if it already exists
        val intent = Intent(applicationContext, WeatherService::class.java)
        intent.action = YAWA.UPDATE_CURRENT_WEATHER_ACTION
        Log.w("!!!", "////////////////////////////////////////// PendingIntent: " +
                PendingIntent.getService(applicationContext, serviceAlarmId, intent, PendingIntent.FLAG_NO_CREATE))
        if(PendingIntent.getService(applicationContext, serviceAlarmId, intent, PendingIntent.FLAG_NO_CREATE) == null) {
            val alarmIntent = PendingIntent.getService(applicationContext, serviceAlarmId, intent, 0)
            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
                    refreshRate!! * 1000 * 60, alarmIntent)
        }
    }

    /**
     * Get the configuration of Auto-Refresh on app bootup
     */
    private fun setInitialRefreshPolicy(prefs: SharedPreferences) {
        val settingsAutoRefreshStr = this.resources.getString(R.string.settings_auto_refresh)
        val autoRefreshPolicy = prefs.getBoolean(settingsAutoRefreshStr, true)

        autoRefreshEnabled = autoRefreshPolicy
        if(autoRefreshEnabled) {
            tryRegisterServiceOnAlarmManager()
            overrideBootIntentConfig(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        }
        else {
            overrideBootIntentConfig(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
        }
    }

    private fun setAutoRefreshAfterPrefChange(enabled: Boolean) {
        autoRefreshEnabled = enabled
        val state: Int
        if(enabled) {
            tryRegisterServiceOnAlarmManager()
            state = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            val intent = Intent(applicationContext, WeatherService::class.java)
            intent.action = YAWA.UPDATE_CURRENT_WEATHER_ACTION
            val alarmIntent = PendingIntent.getService(applicationContext, serviceAlarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(alarmIntent)
            state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        overrideBootIntentConfig(state)
    }

    private fun overrideBootIntentConfig(state: Int) {
        // overrides BootCompleteReceiver configurations in manifest file
        // we don't want to receive BOOT_COMPLETED intent when we have auto-refresh disabled
        val receiver = ComponentName(this, BootCompleteReceiver::class.java)
        packageManager.setComponentEnabledSetting(receiver, state, PackageManager.DONT_KILL_APP)
    }

    override fun onCreate() {
        super.onCreate()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        //////////////////////////////setInitialRefreshPolicy(prefs)
        prefs.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            run {
                val settingsAutoRefreshStr = this.resources?.getString(R.string.settings_auto_refresh)
                // aqui podemos ignorar os default values porque se foi alterado, já existe
                if(key.equals(settingsAutoRefreshStr))
                    setAutoRefreshAfterPrefChange(sharedPreferences.getBoolean(key, true))
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

val Application.autoRefreshEnabled: Boolean
    get() = (this as YAWA).autoRefreshEnabled