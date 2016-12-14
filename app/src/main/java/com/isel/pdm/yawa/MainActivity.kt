package com.isel.pdm.yawa

import android.app.LoaderManager
import android.content.*
import android.database.Cursor
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.widget.DrawerLayout

import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.*
import android.widget.*

import com.isel.pdm.yawa.fragments.WeatherDetailsFragment
import com.isel.pdm.yawa.openweather_tools.OpenWeatherParser
import com.isel.pdm.yawa.provider.WeatherContract
import com.isel.pdm.yawa.service.WeatherService



class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor>, NavigationView.OnNavigationItemSelectedListener {
    private val BACK_PRESS_INTERVAL: Long = 2000 // 2 seconds

    private var lastBackTime: Long = 0
    private val txtTitleCity by lazy { findViewById(R.id.txtTitleCity) as TextView }
    private val weatherFragment by lazy { fragmentManager.findFragmentById(R.id.weather_detail)
            as WeatherDetailsFragment }
    private val navigationView by lazy { findViewById(R.id.navigation_main_view) as NavigationView? }
    private val drawerLayout by lazy { findViewById(R.id.drawer_layout) as DrawerLayout }
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

        // setting up selected item listener
        navigationView?.setNavigationItemSelectedListener(this)

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
        // close navigation bar if its opened
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView)
            return
        }

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

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            R.id.navigation_add -> {
                val intent = Intent(this, CitiesActivity::class.java)
                intent.action = YAWA.ADD_NEW_LOCATION_ACTION
                startActivity(intent)
            }
            R.id.navigation_edit -> {println("edittttttttt")}
            else -> {
                selectCity(menuItem.title.toString())
                setCityOnTitle()
                loaderManager.restartLoader(YAWA.WEATHER_LOADER_ID, null, this)
            }
        }

        drawerLayout.closeDrawers()
        return true
    }

    override fun onOptionsItemSelected(item : MenuItem) : Boolean {
        when(item.itemId) {
            // change city
            R.id.settings_addCity -> {
                val intent = Intent(this, CitiesActivity::class.java)
                intent.action = YAWA.SEARCH_LOCATION_ACTION
                startActivity(intent)
            }
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

        // it can be null in landscape view
        if(navigationView != null)
            updateActionbarItems()
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        Log.e(YAWA.YAWA_ERROR_TAG, "---------- MainActivity.onLoaderReset")
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        Log.e(YAWA.YAWA_ERROR_TAG, "---------- MainActivity.onCreateLoader")
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
        Log.e(YAWA.YAWA_ERROR_TAG, "---------- MainActivity.onLoadFinished")
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

    private fun updateActionbarItems() {
        val menu = navigationView?.menu
        val selectedCity = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(application.settingsLocationStr, application.defaultLocation)

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        // TODO: meter o zero com constante nas resourses
        val citiesCounter = sharedPref.getInt("citiesCounter", 0)

        val subMenu = menu?.addSubMenu(R.string.actionbar_label_cities)

        for (i in 0 until citiesCounter) {
            val city = sharedPref.getString("city" + i, "--")
            val menuItem = subMenu?.add(Menu.NONE, i, Menu.NONE, city)
            menuItem?.isCheckable = true
            if(city == selectedCity) navigationView?.setCheckedItem(i)
        }

//        for (i in 0 until navigationView.childCount) {
//            val child = navigationView.getChildAt(i)
//            if (child != null && child is ListView) {
//                val menuView = child
//                val adapter = menuView.adapter as HeaderViewListAdapter
//                val wrapped = adapter.wrappedAdapter as BaseAdapter
//                wrapped.notifyDataSetChanged()
//            }
//        }
    }

    /**
     * Configure the new city with Shared Preferences
     */
    private fun selectCity(cityDescriptionId: String) {
        val settingsLocationStr = resources.getString(R.string.settings_location_str)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPref.edit()

        editor.putString(settingsLocationStr, cityDescriptionId)
        editor.apply()
    }

    private fun setCityOnTitle() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val title = settings.getString(application.settingsLocationStr, application.defaultLocation)
        txtTitleCity.text = title
    }
}
