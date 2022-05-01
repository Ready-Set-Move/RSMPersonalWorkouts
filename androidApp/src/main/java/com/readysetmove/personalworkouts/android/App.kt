package com.readysetmove.personalworkouts.android

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import com.readysetmove.personalworkouts.bluetooth.AndroidBluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        insertKoin()
    }

    private val appModule = module {
        single {
            val bluetoothManager =
                androidContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            BluetoothStore(AndroidBluetoothService(bluetoothManager.adapter))
        }
    }

    private fun insertKoin() {
        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.ERROR)

            androidContext(this@App)
            modules(appModule)
        }
    }
}