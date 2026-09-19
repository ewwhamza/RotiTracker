package com.ansar.rotitrack

import android.app.Application
import com.ansar.rotitrack.data.RotiDatabase
import com.ansar.rotitrack.data.RotiRepository

class RotiApp : Application() {
    val repository: RotiRepository by lazy { RotiRepository(RotiDatabase.get(this).rotiDao()) }
}
