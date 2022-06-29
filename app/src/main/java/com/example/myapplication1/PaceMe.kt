package com.example.myapplication1

import android.app.Application

class PaceMe: Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
    }

    companion object{
        lateinit var application: Application
    }

}