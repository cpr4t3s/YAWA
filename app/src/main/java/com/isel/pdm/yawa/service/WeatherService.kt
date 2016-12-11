package com.isel.pdm.yawa.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import com.android.volley.VolleyError
import com.isel.pdm.yawa.*


class BootCompleteReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, p1: Intent?) {
        // we get an ReceiverResctrictredContext in context, so we need a cast
        val app = context?.applicationContext as YAWA
        app.tryRegisterServiceOnAlarmManager()
    }
}

/**
 * Service responsible for updating weather state
 */
class WeatherService: Service() {
    private var mServiceLooper: Looper? = null
    private var mServiceHandler: ServiceHandler? = null
    private var updatingCurrentWeather: Boolean = false
    private var updatingForecastWeather: Boolean = false

    private val currentWeatherCallbackSet : ICallbackSet by lazy {
        object : ICallbackSet {
            override fun onError(error: VolleyError) {
                val errorStr: String = resources.getString(R.string.error1001)
                Log.e(YAWA.YAWA_ERROR_TAG, errorStr)
                Log.e(YAWA.YAWA_ERROR_TAG, error.message)
                // Notify activitys waiting for update completion
                val intent = Intent(YAWA.REFRESH_WEATHER_DONE_ACTION)
                intent.putExtra("errMsg", errorStr)
                sendBroadcast(intent)
            }

            override fun onSucceed(response: Any?) {
                // TODO: esta variavel é alterada por varias threads...
                updatingCurrentWeather = false
                // Notify activitys waiting for update completion
                sendBroadcast(Intent(YAWA.REFRESH_WEATHER_DONE_ACTION))
            }
        }
    }

    private val forecastWeatherCallbackSet : ICallbackSet by lazy {
        object : ICallbackSet {
            override fun onError(error: VolleyError) {
                Log.e(YAWA.YAWA_ERROR_TAG, resources.getString(R.string.error1001))
                Log.e(YAWA.YAWA_ERROR_TAG, error.message)
                // Notify activities waiting for update completion
                val intent = Intent(YAWA.REFRESH_WEATHER_DONE_ACTION)
                intent.putExtra("errMsg", resources.getString(R.string.error1004))
                sendBroadcast(intent)
            }

            override fun onSucceed(response: Any?) {
                Log.e(YAWA.YAWA_INFO_TAG, "------ forecastWeatherCallbackSet")
                // TODO: esta variavel é alterada por varias threads... Perguntar o que se deve fazer
                updatingForecastWeather = false
                // Notify activities waiting for update completion
                sendBroadcast(Intent(YAWA.REFRESH_WEATHER_DONE_ACTION))
            }
        }
    }

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            try {
                application.weatherManager.updateCurrentWeather(currentWeatherCallbackSet)
            } catch (e: InterruptedException) {
                // Restore interrupt status.
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }
    }

    override fun onCreate() {
        Log.e(YAWA.YAWA_INFO_TAG, "Service Started")
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        val thread = HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.looper
        mServiceHandler = ServiceHandler(mServiceLooper!!)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(YAWA.YAWA_INFO_TAG, "onStartCommand")
        Log.i(YAWA.YAWA_INFO_TAG, "onStartCommand intent: " + intent)
        Log.i(YAWA.YAWA_INFO_TAG, "onStartCommand intent: " + intent?.action)
        if(intent?.action == YAWA.UPDATE_CURRENT_WEATHER_ACTION && !updatingCurrentWeather) {
            updatingCurrentWeather = true
            // For each start request, send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            val msg = mServiceHandler!!.obtainMessage()
            msg.arg1 = startId
            mServiceHandler!!.sendMessage(msg)
        }
        else if(intent?.action == YAWA.UPDATE_FORECAST_WEATHER_ACTION && !updatingForecastWeather) {
            updatingForecastWeather = true

            mServiceHandler?.post {
                val _startId = startId
                try {
                    application.weatherManager.updateForecastWeather(forecastWeatherCallbackSet)
                } catch (e: InterruptedException) {
                    // Restore interrupt status.
                    Thread.currentThread().interrupt()
                }
                stopSelf(_startId)
            }
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }
}