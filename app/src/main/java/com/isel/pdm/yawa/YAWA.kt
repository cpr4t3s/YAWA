package com.isel.pdm.yawa

import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.SystemClock
import android.preference.PreferenceManager
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.service.WeatherService
import com.isel.pdm.yawa.openweather_tools.OpenWeatherRequester
import com.isel.pdm.yawa.provider.IconCacheContract
import com.isel.pdm.yawa.service.BootCompleteReceiver
import com.isel.pdm.yawa.tools.CacheResolver
import com.isel.pdm.yawa.tools.MetricsResolver
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
        val UPDATE_COORD_WEATHER_ACTION = "com.isel.pdm.yawa.UPDATE_COORD_WEATHER"
        val REFRESH_WEATHER_DONE_ACTION = "com.isel.pdm.yawa.REFRESH_WEATHER_DONE"
        //
        val ADD_NEW_LOCATION_ACTION = "com.isel.pdm.yawa.ADD_NEW_LOCATION_ACTION"
        val SEARCH_LOCATION_ACTION = "com.isel.pdm.yawa.SEARCH_LOCATION_ACTION"
        // Used to set DB entry as current weather or forecast weather
        val COORD_WEATHER_FLAG: Int = 2
        val CURRENT_WEATHER_FLAG: Int = 1
        val FORECAST_WEATHER_FLAG: Int = 0
        // Loaders
        val WEATHER_LOADER_ID = 1
        val FORECAST_LOADER_ID = 2
        val WEATHER_ICON_LOADER_ID = 4
        val COORD_WEATHER_LOADER_ID = 8
        //
        val NOTIFICATIONS_INTENT_ID = 0
        //
        val DEFAULT_ALARM_TIME = "12:0"
        // Cache in MB
        val CACHE_MAX_SIZE: Int = 2
    }

    // defines a notification time
    class GenericTime(val hour: Int, val minutes: Int) {}

    val weatherManager by lazy { WeatherManager(this, OpenWeatherRequester(this, cacheResolver)) }
    val cacheResolver by lazy { CacheResolver<Bitmap>(CACHE_MAX_SIZE, contentResolver, IconCacheContract.Icon.CONTENT_URI, applicationContext) }
    //
    val defaultLocation: String by lazy { resources.getString(R.string.default_location) }
    val defaultForecastDays: Int by lazy { resources.getString(R.string.default_forecast_days).toInt() }
    val defaultMetric: String by lazy { resources.getString(R.string.default_units) }
    //
    val settingsLocationStr: String by lazy { resources.getString(R.string.settings_location_str) }
    val settingsForecastDaysStr: String by lazy { resources.getString(R.string.settings_forecast_days_str) }
    //
    val settingsAutoRefreshStr: String by lazy { resources.getString(R.string.settings_auto_refresh_str) }
    val settingsNotificationEnabledStr: String by lazy { resources.getString(R.string.settings_notifications_enabled_str) }
    val settingsNotificationTimeStr: String by lazy { resources.getString(R.string.settings_notification_time_str) }
    val settingsMetricStr: String by lazy { resources.getString(R.string.settings_units_str) }

    //
    var autoRefreshEnabled: Boolean = true
    //
    //val alarmManager by lazy {applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager}
    val serviceAlarmId = 0


    fun onSystemBootCompleted() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val autoRefreshPolicyEnabled: Boolean = prefs.getBoolean(settingsAutoRefreshStr, true)
        val notificationEnabled: Boolean = prefs.getBoolean(settingsNotificationEnabledStr, false)

        if(autoRefreshPolicyEnabled) tryRegisterServiceOnAlarmManager()
        //
        if(notificationEnabled) startNotificationsAfterBoot()
    }

    private fun startNotificationsAfterBoot() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val settingsNotificationTime = prefs.getString(settingsNotificationTimeStr, DEFAULT_ALARM_TIME)
        scheduleNotification(getNotification(), convertTimeFromString(settingsNotificationTime))
    }

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

    private fun setRefreshPolicyOnBoot(enabled: Boolean) {
        if(enabled) {
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
            setRefreshPolicyOnBoot(enabled)
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
                // aqui podemos ignorar os default values porque se foi alterado, já existe
                if(key == settingsAutoRefreshStr) {
                    // TODO: é preciso alterar o alarme tambem quando a preferencia do tempo é alterada
                    setAutoRefreshAfterPrefChange(sharedPreferences.getBoolean(key, true))
                }
                else if (key == settingsNotificationEnabledStr) {
                    val settingsNotificationTime = sharedPreferences.getString(settingsNotificationTimeStr, DEFAULT_ALARM_TIME)
                    if(sharedPreferences.getBoolean(key, false)){
                        scheduleNotification(getNotification(), convertTimeFromString(settingsNotificationTime))
                    }
                    else{
                        disableNotifications()
                    }

                }
                else if (key == settingsNotificationTimeStr) {
                    val settingsNotificationTime = sharedPreferences.getString(settingsNotificationTimeStr, DEFAULT_ALARM_TIME)
                    updateNotification(getNotification(), convertTimeFromString(settingsNotificationTime))
                }
            }
        }
    }

    private fun convertTimeFromString(time: String): GenericTime {
        val splitedTime = time.split(":")
        val hour = splitedTime[0].toInt()
        val minutes = splitedTime[1].toInt()

        return GenericTime(hour, minutes)
    }

    private fun updateNotification(notification: Notification, time: GenericTime) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, time.hour)
        calendar.set(Calendar.MINUTE, time.minutes)

        // TODO: Colocar o pendingIntent numa var de instância
        val notificationIntent : Intent = Intent(this, NotificationGenerator::class.java)
        notificationIntent.putExtra("notification-id", 1)
        notificationIntent.putExtra("notification", notification)
        val pendingIntent : PendingIntent? = PendingIntent.getBroadcast(
                this, NOTIFICATIONS_INTENT_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if(pendingIntent != null) {
            val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
        }
    }

    private fun  scheduleNotification(notification: Notification, time: GenericTime) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, time.hour)
        calendar.set(Calendar.MINUTE, time.minutes)

        val notificationIntent : Intent = Intent(this, NotificationGenerator::class.java)
        notificationIntent.putExtra("notification-id", 1)
        notificationIntent.putExtra("notification", notification)

        val pendingIntent : PendingIntent? = PendingIntent.getBroadcast(
                this, NOTIFICATIONS_INTENT_ID, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        if(pendingIntent != null) {
            val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
        }
    }

    private fun  getNotification(): Notification {
        val builder : Notification.Builder = Notification.Builder(this)
        val currentWeather: WeatherStateDO = weatherManager.getCurrentWeather(cacheResolver)
        val city = PreferenceManager.getDefaultSharedPreferences(applicationContext).
                getString(settingsLocationStr, defaultLocation)
        val unit = PreferenceManager.getDefaultSharedPreferences(applicationContext).
                getString(settingsMetricStr, defaultMetric)

        builder.setContentTitle(city)
        builder.setContentText("${currentWeather.description}  -  ${currentWeather.temp} ${MetricsResolver.getMetricSymbol(unit)}")
        builder.setSmallIcon(R.drawable.notification_icon)
        builder.setAutoCancel(true)

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
        val notificationIntent : Intent = Intent(this, NotificationGenerator::class.java)

        val pendingIntent : PendingIntent? =
                PendingIntent.getBroadcast( this, NOTIFICATIONS_INTENT_ID, notificationIntent, PendingIntent.FLAG_NO_CREATE)
        if(pendingIntent != null) {
            val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}


val Application.weatherManager : WeatherManager
    get() = (this as YAWA).weatherManager

val Application.cacheResolver: CacheResolver<Bitmap>
    get() = (this as YAWA).cacheResolver

val Application.defaultLocation: String
    get() = (this as YAWA).defaultLocation

val Application.defaultForecastDays: Int
    get() = (this as YAWA).defaultForecastDays

val Application.defaultMetric: String
    get() = (this as YAWA).defaultMetric

val Application.settingsLocationStr: String
    get() = (this as YAWA).settingsLocationStr

val Application.settingsForecastDaysStr: String
    get() = (this as YAWA).settingsForecastDaysStr

val Application.settingsMetricStr: String
    get() = (this as YAWA).settingsMetricStr