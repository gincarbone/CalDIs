package com.youandmedia.caldis

import android.app.Application
import com.youandmedia.caldis.data.AppDatabase

class CaldisApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
