package org.mromichov.guessthesong

import android.app.Application
import org.mromichov.guessthesong.core.di.AppComponent
import org.mromichov.guessthesong.core.di.DaggerAppComponent

class App : Application() {
    lateinit var appComponent: AppComponent
    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)
    }
}