package com.silicio.copi

import android.app.Application
import com.silicio.copi.nav.Copi

class CopiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Copi.init(this)
    }
}
