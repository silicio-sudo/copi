package com.silicio.copi

import android.app.Application
import com.silicio.copi.nav.Copi

class CopiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        org.osmdroid.config.Configuration.getInstance().apply {
            load(this@CopiApp, this@CopiApp.getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = this@CopiApp.packageName
        }
        Copi.init(this)
    }
}
