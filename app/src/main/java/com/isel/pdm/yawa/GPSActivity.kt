package com.isel.pdm.yawa

import android.app.AlertDialog
import android.app.LoaderManager
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.isel.pdm.yawa.DataContainers.WeatherStateDO
import com.isel.pdm.yawa.openweather_tools.OpenWeatherParser
import com.isel.pdm.yawa.provider.WeatherContract
import com.isel.pdm.yawa.service.WeatherService
import com.isel.pdm.yawa.tools.DateConverter
import com.isel.pdm.yawa.tools.MetricsResolver
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


class GPSActivity : AppCompatActivity(), LocationListener, LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        val MSG_WAITING_FOR_SERVICE: String = "Waiting for Service..."
        val MSG_NO_SIGNAL: String = "No Signal..."
        val MSG_NO_PROVIDER: String = "No Provider..."
        val MIN_DISTANCE_TO_UPDATE: Float = 10000f // 10km
        val MIN_TIME_TO_UPDATE: Long = 15 * 60 * 1000 // 15 minutos
        //
        val TITLE_KEY = "title_key"
        val DATE_KEY = "date_key"
        val DESCRIPTION_KEY = "desc_key"
        val HUMIDITY_KEY = "humidity_key"
        val TEMPCURR_KEY = "tcurr_key"
        val TEMPMAX_KEY = "tmax_key"
        val TEMPMIN_KEY = "tmin_key"
        val WMAIN_KEY = "wmain_key"
        val IMG_KEY = "img_key"
    }
    //
    private val updateDoneReceiver: BroadcastReceiver = object: BroadcastReceiver() {
        private val hnd: Handler = Handler(Looper.getMainLooper())

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == YAWA.REFRESH_WEATHER_DONE_ACTION) {
                this@GPSActivity.swR.isRefreshing = false
                this@GPSActivity.updating = false

                hnd.post {
                    val errTag: String = "errMsg"
                    if(intent.hasExtra(errTag)) {
                        Toast.makeText(this@GPSActivity, intent.getStringExtra(errTag), Toast.LENGTH_SHORT).show()
                    } else {
                        this@GPSActivity.setTitle(cityName)
                    }
                }
            }
        }
    }
    private val swR by lazy { findViewById(R.id.coord_weather_swiperefresh) as SwipeRefreshLayout }
    var locationManager: LocationManager? = null
    var cityName: String = ""
    // GPS by default
    private var provider: String = LocationManager.GPS_PROVIDER
    private var updating: Boolean = false
    private var afterRotate: Boolean = false

    private fun setTitle(title: String) {
        (findViewById(R.id.txtGPSTitle) as TextView).text = title
    }

    fun updateUI(weatherState: WeatherStateDO) {
        val unit = PreferenceManager.getDefaultSharedPreferences(applicationContext).
                getString(application.settingsMetricStr, application.defaultMetric)
        val unitSymbol = MetricsResolver.getMetricSymbol(unit)

        // Title
        cityName = weatherState.cityName
        // Main state
        var tmpTextView = findViewById(R.id.GPSWeatherMain) as TextView
        tmpTextView.text = weatherState.mainState
        // Description
        tmpTextView = findViewById(R.id.GPSWeatherDescription) as TextView
        tmpTextView.text = weatherState.description
        tmpTextView = findViewById(R.id.GPSWeatherhumidity) as TextView
        tmpTextView.text = weatherState.humidity.toString()
        // we dont have Current Temp when in forecast
        tmpTextView = findViewById(R.id.GPSWeatherTempCurrent) as TextView
        tmpTextView.text = "${weatherState.temp} $unitSymbol"
        //
        tmpTextView = findViewById(R.id.GPSWeatherTempMax) as TextView
        tmpTextView.text = "${weatherState.temp_max} $unitSymbol"
        tmpTextView = findViewById(R.id.GPSWeatherTempMin) as TextView
        tmpTextView.text = "${weatherState.temp_min} $unitSymbol"
        tmpTextView = findViewById(R.id.GPSLastUpdateTextView) as TextView
        val date: String
        if(weatherState.updateDate > 0)
            date = DateConverter.unixSecondsToDateString(weatherState.updateDate,
                    TimeZone.getDefault(), SimpleDateFormat("yyyy-MM-dd, HH:mm"))
        else
            date = "--"
        tmpTextView.text = date

        weatherState.weatherIcon.let {
            val tmpImageView = findViewById(R.id.GPSImageViewWeatherState) as ImageView
            tmpImageView.setImageBitmap(weatherState.weatherIcon)
        }
    }

    private fun updateLocation(location: Location?) {
        if(location == null) return

        updating = true
        //
        val intent = Intent(this, WeatherService::class.java)
        intent.action = YAWA.UPDATE_COORD_WEATHER_ACTION
        intent.putExtra(WeatherService.COORD_LATITUDE_KEY, location.latitude)
        intent.putExtra(WeatherService.COORD_LONGITUDE_KEY, location.longitude)
        startService(intent)
    }

    private fun updateProvider(showAlert: Boolean): Boolean {
        val isGPSEnabled: Boolean = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)!!
        val isNetworkEnabled: Boolean = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)!!

        if(isGPSEnabled) return true

        if(showAlert) showSettingsAlert()

        if(!isNetworkEnabled) return false
        provider = LocationManager.NETWORK_PROVIDER

        return true
    }

    fun showSettingsAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("GPS Settings")

        alertDialog.setMessage("GPS is not enabled. Do you want to enable it?")

        // Settings button
        alertDialog.setPositiveButton("Ok", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                this@GPSActivity.startActivity(intent)

                this@GPSActivity.provider = LocationManager.GPS_PROVIDER
            }
        })

        // Cancel button
        alertDialog.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.cancel()
            }
        })

        alertDialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gps_activity_layout)
        // initiate our loader
        loaderManager.initLoader(YAWA.COORD_WEATHER_LOADER_ID, null, this)
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //
        if(savedInstanceState == null) {
            // check
            updateProvider(true)
        } else {
            afterRotate = true
        }
    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(updateDoneReceiver)

        locationManager?.removeUpdates(this)
    }

    override fun onResume() {
        super.onResume()

        val intentFilter = IntentFilter(YAWA.REFRESH_WEATHER_DONE_ACTION)
        registerReceiver(updateDoneReceiver, intentFilter)

        if(updateProvider(false) && !afterRotate && !updating) {
            locationManager?.requestLocationUpdates(
                    provider, MIN_TIME_TO_UPDATE, MIN_DISTANCE_TO_UPDATE, this)
            setTitle(MSG_WAITING_FOR_SERVICE)
        } else {
            setTitle(MSG_NO_PROVIDER)
        }

        if(updating) swR.isRefreshing = true
        if(afterRotate) setTitle(cityName)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(TITLE_KEY, cityName)
        // Main state
        var tmpTextView = findViewById(R.id.GPSWeatherMain) as TextView
        outState.putString(WMAIN_KEY, tmpTextView.text.toString())
        // Description
        tmpTextView = findViewById(R.id.GPSWeatherDescription) as TextView
        outState.putString(DESCRIPTION_KEY, tmpTextView.text.toString())
        tmpTextView = findViewById(R.id.GPSWeatherhumidity) as TextView
        outState.putString(HUMIDITY_KEY, tmpTextView.text.toString())
        // we dont have Current Temp when in forecast
        tmpTextView = findViewById(R.id.GPSWeatherTempCurrent) as TextView
        outState.putString(TEMPCURR_KEY, tmpTextView.text.toString())
        //
        tmpTextView = findViewById(R.id.GPSWeatherTempMax) as TextView
        outState.putString(TEMPMAX_KEY, tmpTextView.text.toString())
        tmpTextView = findViewById(R.id.GPSWeatherTempMin) as TextView
        outState.putString(TEMPMIN_KEY, tmpTextView.text.toString())
        tmpTextView = findViewById(R.id.GPSLastUpdateTextView) as TextView
        outState.putString(DATE_KEY, tmpTextView.text.toString())
        //
        val tmpImageView = findViewById(R.id.GPSImageViewWeatherState) as ImageView
        val stream = ByteArrayOutputStream()
        val bitMap = (tmpImageView.drawable as BitmapDrawable).bitmap
        bitMap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        outState.putByteArray(IMG_KEY, byteArray)


//        weatherState.weatherIcon.let {
//            val tmpImageView = findViewById(R.id.GPSImageViewWeatherState) as ImageView
//            tmpImageView.setImageBitmap(weatherState.weatherIcon)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        cityName = savedInstanceState.getString(TITLE_KEY)
        // Main state
        var tmpTextView = findViewById(R.id.GPSWeatherMain) as TextView
        tmpTextView.text = savedInstanceState.getString(WMAIN_KEY)
        // Description
        tmpTextView = findViewById(R.id.GPSWeatherDescription) as TextView
        tmpTextView.text = savedInstanceState.getString(DESCRIPTION_KEY)
        tmpTextView = findViewById(R.id.GPSWeatherhumidity) as TextView
        tmpTextView.text = savedInstanceState.getString(HUMIDITY_KEY)
        // we dont have Current Temp when in forecast
        tmpTextView = findViewById(R.id.GPSWeatherTempCurrent) as TextView
        tmpTextView.text = savedInstanceState.getString(TEMPCURR_KEY)
        //
        tmpTextView = findViewById(R.id.GPSWeatherTempMax) as TextView
        tmpTextView.text = savedInstanceState.getString(TEMPMAX_KEY)
        tmpTextView = findViewById(R.id.GPSWeatherTempMin) as TextView
        tmpTextView.text = savedInstanceState.getString(TEMPMIN_KEY)
        tmpTextView = findViewById(R.id.GPSLastUpdateTextView) as TextView
        tmpTextView.text = savedInstanceState.getString(DATE_KEY)
        //
        val tmpImageView = findViewById(R.id.GPSImageViewWeatherState) as ImageView
        val byteArray = savedInstanceState.getByteArray(IMG_KEY)
        tmpImageView.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
    }

    override fun onLocationChanged(location: Location) {
        swR.isRefreshing = true
        updateLocation(location)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        Log.e("!!!", "onStatusChanged")
        println("Provider: $provider")
        println("Status: $status")
    }

    override fun onProviderEnabled(provider: String) {
        setTitle(MSG_WAITING_FOR_SERVICE)
    }

    override fun onProviderDisabled(provider: String) {
        setTitle(MSG_NO_SIGNAL)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val projection: Array<String>
        val selectionArgs: Array<String>?
        val selectionClause: String?
        val uri: Uri
        when (id) {
            YAWA.COORD_WEATHER_LOADER_ID -> {
                projection = WeatherContract.Weather.SELECT_ALL
                selectionArgs = arrayOf(YAWA.COORD_WEATHER_FLAG.toString())
                selectionClause = WeatherContract.Weather.CURRENT + " = ?"
                uri = WeatherContract.Weather.CONTENT_URI
            }
            else -> {
                Log.w(YAWA.YAWA_WARN_TAG, "Unknown id for loader on onCreateLoader")
                throw IllegalArgumentException("id")
            }
        }


        return CursorLoader(
                this,
                uri,
                projection,
                selectionClause,
                selectionArgs,
                null
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, cursor: Cursor?) {
        when(loader?.id) {
            YAWA.COORD_WEATHER_LOADER_ID -> {
                if (cursor != null) {
                    // updates weather data
                    val weatherState = OpenWeatherParser.parseWeatherState(cursor, application.cacheResolver, applicationContext)
                    runOnUiThread { updateUI(weatherState) }
                }
            }
            else -> {
                Log.w(YAWA.YAWA_WARN_TAG, "Unknown id for loader on onLoadFinished")
                throw IllegalArgumentException("id")
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        Log.i(YAWA.YAWA_INFO_TAG, "GPSActivity.onLoaderReset")
    }
}