package com.isel.pdm.yawa.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.SimpleCursorAdapter
import android.widget.Toast
import com.android.volley.VolleyError
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.ICallbackSet
import com.isel.pdm.yawa.R
import com.isel.pdm.yawa.YAWA
import com.isel.pdm.yawa.provider.WeatherContract
import com.isel.pdm.yawa.weatherManager


class BootCompleteReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, p1: Intent?) {
        // we get an ReceiverResctrictredContext in context, so we need a cast
        val app = context?.applicationContext as YAWA
        app.registerServiceOnAlarmManager()
    }
}

/**
 * Service responsible for updating weather state
 */
class WeatherService: Service() {
    private var mServiceLooper: Looper? = null
    private var mServiceHandler: ServiceHandler? = null
    companion object {
        val OPERATION_KEY: String = "operation"
        val OPERATION_DEFAULT: Int = 0
    }

    private val callbackSet : ICallbackSet by lazy {
        object : ICallbackSet {
            override fun onError(error: VolleyError) {
                //
                Toast.makeText(this@WeatherService,
                        resources.getString(R.string.error1001), Toast.LENGTH_SHORT).show()
            }

            override fun onSucceed(response: Any) {
                val weatherState = response as WeatherStateDO
                readData()
                Log.e(">>>", "description: " + weatherState.description)
                Log.e(">>>", "temp: " + weatherState.temp)
                //val uri = insertData(weatherState)
                //Log.e(">>>", "uri: " + uri)
                readData()
            }
        }
    }

    private fun readData() {
        val projection = arrayOf<String>(
                WeatherContract.Weather.DESCRIPTION,
                WeatherContract.Weather.TEMPERATURE,
                WeatherContract.Weather.TEMPERATURE_MAX,
                WeatherContract.Weather.TEMPERATURE_MIN
        )

        val mCursor = contentResolver.query(
                WeatherContract.Weather.CONTENT_URI,
                projection,
                null,
                null,
                WeatherContract.Weather.DEFAULT_SORT_ORDER)

        Log.e("!!!!!!!!!!!!!!!!", "mCursor Count: " + mCursor)
    }

    private fun insertData(weather: WeatherStateDO): Uri {
        val mNewValues = ContentValues()
        val newUri: Uri

        mNewValues.put(WeatherContract.Weather.DESCRIPTION, weather.description)
        mNewValues.put(WeatherContract.Weather.TEMPERATURE, weather.temp)
        mNewValues.put(WeatherContract.Weather.TEMPERATURE_MAX, weather.temp_max)
        mNewValues.put(WeatherContract.Weather.TEMPERATURE_MIN, weather.temp_min)

        newUri = contentResolver.insert(
                WeatherContract.Weather.CONTENT_URI, // the user dictionary content URI
                mNewValues                          // the values to insert
        )

        return newUri
    }


    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            try {
                Log.e("!!!!", "--------------------   start Request")
                application.weatherManager.refreshCurrentWeather(callbackSet)
                Log.e("!!!!", "--------------------   stop Request")
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
        Log.e("!!!", "Service Started")
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
        Log.e("!!!", "New Request")

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        val msg = mServiceHandler!!.obtainMessage()
        msg.arg1 = startId
        if(intent != null) {
            msg.arg2 = intent.getIntExtra(OPERATION_KEY, OPERATION_DEFAULT)
        }
        mServiceHandler!!.sendMessage(msg)

        println("######################################################")

        /*
        mServiceHandler?.post {
            Log.e("!!!!", "Posted Runnable!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            val msg = mServiceHandler?.obtainMessage()
            Log.e("!!!!", msg?.arg2.toString())
        }*/

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }
}