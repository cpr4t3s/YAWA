package com.isel.pdm.yawa

import android.app.AlarmManager
import android.app.Application
import android.app.Notification
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
import java.util.*

class YAWA : Application() {
    companion object {
        val YAWA_ERROR_TAG = "!!! YAWA Erro"
        val YAWA_WARN_TAG = "!!! YAWA Wanr"
        val YAWA_INFO_TAG = "> YAWA Info"
        // Actions
        val UPDATE_CURRENT_WEATHER_ACTION = "com.isel.pdm.yawa.UPDATE_CURRENT_WEATHER"
        val AUTO_UPDATE_CURRENT_WEATHER_ACTION = "com.isel.pdm.yawa.AUTO_UPDATE_CURRENT_WEATHER"
        val UPDATE_FORECAST_WEATHER_ACTION = "com.isel.pdm.yawa.UPDATE_FORECAST_WEATHER"
        val REFRESH_WEATHER_DONE_ACTION = "com.isel.pdm.yawa.REFRESH_WEATHER_DONE"
        //
        val ADD_NEW_LOCATION_ACTION = "com.isel.pdm.yawa.ADD_NEW_LOCATION_ACTION"
        val SEARCH_LOCATION_ACTION = "com.isel.pdm.yawa.SEARCH_LOCATION_ACTION"
        // Used to set DB entry as current weather or forecast weather
        val CURRENT_WEATHER_FLAG: Int = 1
        val FORECAST_WEATHER_FLAG: Int = 0
        // Loaders
        val WEATHER_LOADER_ID = 1
        val FORECAST_LOADER_ID = 2
        //
        val NOTIFICATIONS_INTENT_ID = 0
    }

    val weatherManager by lazy { WeatherManager(this, OpenWeatherRequester(this)) }
    //
    val defaultLocation: String by lazy { resources.getString(R.string.default_location) }
    val defaultForecastDays: Int by lazy { resources.getString(R.string.default_forecast_days).toInt() }
    //
    val settingsLocationStr: String by lazy { resources.getString(R.string.settings_location_str) }
    val settingsForecastDaysStr: String by lazy { resources.getString(R.string.settings_forecast_days_str) }

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
        intent.action = YAWA.AUTO_UPDATE_CURRENT_WEATHER_ACTION

        if(PendingIntent.getService(applicationContext, serviceAlarmId, intent, PendingIntent.FLAG_NO_CREATE) == null) {
            val alarmIntent = PendingIntent.getService(applicationContext, serviceAlarmId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
                    refreshRate!! * 1000 * 60, alarmIntent)
        }
    }

    /**
     * Get the configuration of Auto-Refresh on app bootup
     */
    private fun setInitialRefreshPolicy(prefs: SharedPreferences) {
        val settingsAutoRefreshStr = this.resources.getString(R.string.settings_auto_refresh_str)
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
            intent.action = YAWA.AUTO_UPDATE_CURRENT_WEATHER_ACTION
            val alarmIntent = PendingIntent.getService(applicationContext, serviceAlarmId, intent, PendingIntent.FLAG_NO_CREATE)
            if(alarmIntent != null) {
                val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(alarmIntent)
                alarmIntent.cancel()
            }
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
        prefs.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            run {
                val settingsAutoRefreshStr = this.resources?.getString(R.string.settings_auto_refresh_str)
                val settingsNotificationEnabledStr = this.resources?.getString(R.string.settings_notifications_enabled_str)
                val settingsNotificationTimeStr = this.resources?.getString(R.string.settings_notification_time_str)
                // aqui podemos ignorar os default values porque se foi alterado, já existe
                if(key.equals(settingsAutoRefreshStr)) {
                    setAutoRefreshAfterPrefChange(sharedPreferences.getBoolean(key, true))
                }
                else if (key == settingsNotificationEnabledStr){
                    val settingsNotificationTime = sharedPreferences.getString(settingsNotificationTimeStr,"12:0")
                    if(sharedPreferences.getBoolean(key,false)){
                        scheduleNotification(getNotification(),settingsNotificationTime)
                    }
                    else{
                        disableNotifications()
                    }

                }
            }
        }
    }


    private fun  scheduleNotification(notification: Notification, time: String) {
        val splitedTime = time.split(":")
        val hour = splitedTime[0].toInt()
        val minutes = splitedTime[1].toInt()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minutes)

        var notificationIntent : Intent = Intent(this, NotificationGenerator::class.java)
        notificationIntent.putExtra("notification-id",1)
        notificationIntent.putExtra("notification",notification)

        var pendingIntent : PendingIntent = PendingIntent
                .getBroadcast( this, NOTIFICATIONS_INTENT_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        var alarmManager : AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager
                .setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)

    }

    private fun  getNotification(): Notification {
        var builder : Notification.Builder = Notification.Builder(this)
        builder.setContentTitle("Claudio nao pescamos nada disto")
        builder.setContentText("depois aqui temos de meter cenas e tal")
        builder.setSmallIcon(R.drawable.notification_template_icon_bg)

        val resultIntent =  Intent(this, MainActivity::class.java)

        val  resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                )

        builder.setContentIntent(resultPendingIntent)
        return builder.build()
    }

    private fun disableNotifications(){
        var notificationIntent : Intent = Intent(this, NotificationGenerator::class.java)

        var pendingIntent : PendingIntent = PendingIntent
                .getBroadcast( this, NOTIFICATIONS_INTENT_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        var alarmManager : AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}


val Application.weatherManager : WeatherManager
    get() = (this as YAWA).weatherManager

val Application.defaultLocation: String
    get() = (this as YAWA).defaultLocation

val Application.defaultForecastDays: Int
    get() = (this as YAWA).defaultForecastDays

val Application.settingsLocationStr: String
    get() = (this as YAWA).settingsLocationStr

val Application.settingsForecastDaysStr: String
    get() = (this as YAWA).settingsForecastDaysStr

val Application.autoRefreshEnabled: Boolean
    get() = (this as YAWA).autoRefreshEnabled