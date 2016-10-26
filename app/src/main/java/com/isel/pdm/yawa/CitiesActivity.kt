package com.isel.pdm.yawa

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import com.isel.pdm.yawa.fragments.CitiesListFragment

class CitiesActivity : AppCompatActivity() {
    private val searchButton by lazy { findViewById(R.id.btnSearchCity) as ImageButton }
    private val listFragment by lazy { fragmentManager.findFragmentById(R.id.cities_list_frag) as CitiesListFragment }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cities)

        // Search Button
        searchButton.setOnClickListener {
            listFragment.doSearch()
        }

    }

    override fun onStop() {
        super.onStop()

        // on activity's state changes we need to cancel possible requests
        application.weatherManager.cancelAllRequests()
    }

    override fun onSaveInstanceState(outState : Bundle) {
        outState.putString(CitiesListFragment.SEARCH_TEXT_KEY, listFragment.txtSearchStr.text.toString())
    }

    override fun onRestoreInstanceState(restoredState : Bundle) {
        listFragment.txtSearchStr.setText(restoredState.getString(CitiesListFragment.SEARCH_TEXT_KEY))
    }

}
