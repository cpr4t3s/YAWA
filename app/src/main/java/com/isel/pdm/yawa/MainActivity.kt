package com.isel.pdm.yawa

import android.app.LoaderManager
import android.content.*
import android.database.Cursor
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager

import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast

import com.isel.pdm.yawa.fragments.WeatherDetailsFragment
import com.isel.pdm.yawa.openweather_tools.OpenWeatherParser
import com.isel.pdm.yawa.provider.WeatherContract
import com.isel.pdm.yawa.service.WeatherService



class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {
    private val BACK_PRESS_INTERVAL: Long = 2000 // 2 seconds

    private var lastBackTime: Long = 0
    private val txtTitleCity by lazy { findViewById(R.id.txtTitleCity) as TextView }
    private val weatherFragment by lazy { fragmentManager.findFragmentById(R.id.weather_detail)
            as WeatherDetailsFragment }
    private val swR by lazy { findViewById(R.id.current_weather_swiperefresh) as SwipeRefreshLayout }
    //
    private val updateDoneReceiver: BroadcastReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == YAWA.REFRESH_WEATHER_DONE_ACTION) {
                // TODO: possível problema de concorrencia
                this@MainActivity.runOnUiThread {
                    this@MainActivity.swR.isRefreshing = false
                }

                val errTag: String = "errMsg"
                if(intent.hasExtra(errTag)) {
                    Toast.makeText(this@MainActivity, intent.getStringExtra(errTag), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun updateCurrentWeather() {
        val updateWeatherIntent: Intent = Intent(this, WeatherService::class.java)
        updateWeatherIntent.action = YAWA.UPDATE_CURRENT_WEATHER_ACTION
        startService(updateWeatherIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather__main_menu_)

        swR.setOnRefreshListener( {updateCurrentWeather()} )
        // Register a listener for 'REFRESH_WEATHER_DONE_ACTION' broadcasts
        val intentFilter = IntentFilter(YAWA.REFRESH_WEATHER_DONE_ACTION)
        registerReceiver(updateDoneReceiver, intentFilter)

        // initiate our loader
        loaderManager.initLoader(YAWA.WEATHER_LOADER_ID, null, this)
    }

    override fun onStop() {
        super.onStop()

        // on activity's state changes we need to cancel possible requests
        application.weatherManager.cancelAllRequests()
    }

    override fun onDestroy() {
        super.onDestroy()
        // stop litening for broadcasts
        unregisterReceiver(updateDoneReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        println("MainActivity.onSaveInstanceState")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        println("MainActivity.onRestoreInstanceState")
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - this.lastBackTime < this.BACK_PRESS_INTERVAL) {
            super.onBackPressed()
        }

        this.lastBackTime = System.currentTimeMillis()
        Toast.makeText(this, R.string.message_on_exit, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item : MenuItem) : Boolean {
        when(item.itemId) {
            // change city
            R.id.settings_addCity -> startActivity(Intent(this, CitiesActivity::class.java))
            // show forecast weather
            R.id.settings_forecast -> startActivity(Intent(this, ForecastActivity::class.java))
            // refresh weather
            R.id.settings_refresh -> { swR.isRefreshing = true; updateCurrentWeather() }
            // Information about the application
            R.id.settings_about -> startActivity(Intent(this, AboutActivity::class.java))
            // Information about the application
            R.id.settings_settings -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        return true
    }

    override fun onResume() {
        super.onResume()

        // update title
        setCityOnTitle()
        // update the weather. The weather may change when the user select a different city
        loaderManager.restartLoader(YAWA.WEATHER_LOADER_ID, null, this)
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        Log.e(YAWA.YAWA_ERROR_TAG, "---------- MainActivity.onLoaderReset")
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val city = PreferenceManager.getDefaultSharedPreferences(applicationContext).
                getString(application.settingsLocationStr, application.defaultLocation)
        val selectionClause: String =
                WeatherContract.Weather.CITY_ID + " = ? AND " + WeatherContract.Weather.CURRENT + " = ?"

        when (id) {
            YAWA.WEATHER_LOADER_ID -> {
                val selectionArgs = arrayOf(city, YAWA.CURRENT_WEATHER_FLAG.toString())
                return CursorLoader(
                        this,
                        WeatherContract.Weather.CONTENT_URI,
                        WeatherContract.Weather.SELECT_ALL,
                        selectionClause,
                        selectionArgs,
                        null
                )
            }
            else -> {
                Log.w(YAWA.YAWA_WARN_TAG, "Unknown id for loader on onCreateLoader")
                throw IllegalArgumentException("id")
            }
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, cursor: Cursor?) {
        when(loader?.id) {
            YAWA.WEATHER_LOADER_ID -> {
                if (cursor != null) {
                    // updates weather data
                    val weatherState = OpenWeatherParser.parseWeatherState(cursor)
                    // TODO: necessario actualizar na UI Thread?
                    runOnUiThread { weatherFragment.updateUI(weatherState) }
                }
            }
            else -> {
                Log.w(YAWA.YAWA_WARN_TAG, "Unknown id for loader on onLoadFinished")
                throw IllegalArgumentException("id")
            }
        }
    }

    private fun setCityOnTitle() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val title = settings.getString(application.settingsLocationStr, application.defaultLocation)
        txtTitleCity.text = title
    }
}
