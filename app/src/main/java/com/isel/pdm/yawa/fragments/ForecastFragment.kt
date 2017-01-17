package com.isel.pdm.yawa.fragments

import android.app.ListFragment
import android.app.LoaderManager
import android.content.*
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import com.isel.pdm.yawa.*
import com.isel.pdm.yawa.UI.ForecastCursorAdapter
import com.isel.pdm.yawa.provider.IconCacheContract
import com.isel.pdm.yawa.provider.WeatherContract
import com.isel.pdm.yawa.service.WeatherService



class ForecastFragment : ListFragment(), LoaderManager.LoaderCallbacks<Cursor> {
    // Can't be initialized here because for each transaction between fragments, the view will be
    // infalted in onCreateView, and the SwipeRefreshLayout will be a new object
    private var swR: SwipeRefreshLayout? = null
    private val weatherAdapter: ForecastCursorAdapter by lazy {
        ForecastCursorAdapter(activity, null, 0, activity.application.cacheResolver)
    }

    //
    private val updateDoneReceiver: BroadcastReceiver = object: BroadcastReceiver() {
        private val hnd: Handler = Handler(Looper.getMainLooper())

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == YAWA.REFRESH_WEATHER_DONE_ACTION) {
                this@ForecastFragment.swR?.isRefreshing = false

                hnd.post {
                    val errTag: String = "errMsg"
                    if(intent.hasExtra(errTag)) {
                        Toast.makeText(activity, intent.getStringExtra(errTag), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // retain the fragment on screen orientation changes
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.forecast_fragment_layout, container, false)
    }

    private fun updateForecastWeather() {
        val intent = Intent(activity.applicationContext, WeatherService::class.java)
        intent.action = YAWA.UPDATE_FORECAST_WEATHER_ACTION
        activity.startService(intent)
    }

    override fun onResume() {
        super.onResume()
        // Register a listener for 'REFRESH_WEATHER_DONE_ACTION' broadcasts
        val intentFilter = IntentFilter(YAWA.REFRESH_WEATHER_DONE_ACTION)
        activity.registerReceiver(updateDoneReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        swR?.isRefreshing = false
        // stop litening for broadcasts
        activity.unregisterReceiver(updateDoneReceiver)
    }

    override fun onActivityCreated (savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // sets our adapter
        listView.adapter = weatherAdapter

        // Set click listener
        listView.setOnItemClickListener { adapterView, view, position, id ->
            onListViewItemClicked(adapterView, view, position, id)
        }
        //
        swR = activity.findViewById(R.id.forecast_weather_swiperefresh) as SwipeRefreshLayout
        swR?.setOnRefreshListener({ updateForecastWeather() })

        // initiate our loader
        loaderManager.initLoader(YAWA.FORECAST_LOADER_ID, null, this)
        loaderManager.initLoader(YAWA.WEATHER_ICON_LOADER_ID, null, this)
    }

    /**
     * Called when the user press an item of ListView
     */
    private fun onListViewItemClicked(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        val newFragment = WeatherDetailsFragment()
        fragmentManager.beginTransaction().replace(R.id.forecast_data_holder, newFragment)
                .addToBackStack(null)
                .commit()

        //
        val bundle = Bundle()
        bundle.putInt(WeatherDetailsFragment.POSITION_TAG, position)
        newFragment.arguments = bundle
    }



    /**
     * LoaderCallbacks
     */

    override fun onCreateLoader(loaderId: Int, bundle: Bundle?): Loader<Cursor> {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
        val city = sharedPrefs.getString(activity.application.settingsLocationStr, activity.application.defaultLocation)
        val forecastDays = sharedPrefs.getInt(activity.application.settingsForecastDaysStr, activity.application.defaultForecastDays)
        val selectionClause: String =
                WeatherContract.Weather.CITY_ID + " = ? AND " + WeatherContract.Weather.CURRENT + " = ?"
        val order = WeatherContract.Weather.DEFAULT_SORT_ORDER + " LIMIT " + forecastDays

        when (loaderId) {
            YAWA.FORECAST_LOADER_ID -> {
                val selectionArgs = arrayOf(city, YAWA.FORECAST_WEATHER_FLAG.toString())
                return CursorLoader(
                        activity,
                        WeatherContract.Weather.CONTENT_URI,
                        WeatherContract.Weather.SELECT_ALL,
                        selectionClause,
                        selectionArgs,
                        order
                )
            }
            YAWA.WEATHER_ICON_LOADER_ID -> {
                return CursorLoader(
                        activity,
                        IconCacheContract.Icon.CONTENT_URI,
                        IconCacheContract.Icon.SELECT_ALL,
                        null,
                        null,
                        IconCacheContract.Icon.DEFAULT_SORT_ORDER
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
            YAWA.FORECAST_LOADER_ID -> {
                weatherAdapter.changeCursor(cursor)
            }
            YAWA.WEATHER_ICON_LOADER_ID -> {
                if (cursor != null) {
                    //
                    activity.contentResolver.notifyChange(WeatherContract.Weather.CONTENT_URI, null)
                }
            }
            else -> {
                Log.w(YAWA.YAWA_WARN_TAG, "Unknown id for loader on onLoadFinished")
                throw IllegalArgumentException("id")
            }
        }
    }

    override fun onLoaderReset(p0: Loader<Cursor>?) {
        weatherAdapter.changeCursor(null)
    }
}