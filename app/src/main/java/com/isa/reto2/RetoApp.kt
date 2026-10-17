package com.isa.reto2

import android.app.Application
import com.isa.reto2.data.AppDatabase

class RetoApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
}
