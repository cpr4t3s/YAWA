package com.isel.pdm.yawa

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity


class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val web = findViewById(R.id.web)
        web.setOnClickListener {startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openweathermap.org/")))
        }
    }
}