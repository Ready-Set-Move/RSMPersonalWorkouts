package com.readysetmove.personalworkouts.android

import android.app.Application
import com.readysetmove.personalworkouts.IsTimestampProvider
import com.readysetmove.personalworkouts.app.AppStore
import com.readysetmove.personalworkouts.bluetooth.AndroidBluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothState
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import com.readysetmove.personalworkouts.device.DeviceStore
import com.readysetmove.personalworkouts.device.IsDeviceStore
import com.readysetmove.personalworkouts.device.MockDeviceStore
import com.readysetmove.personalworkouts.workout.WorkoutStore
import kotlinx.coroutines.Dispatchers
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
            val bluetoothService = AndroidBluetoothService(androidContext())
            BluetoothStore(
                bluetoothService = bluetoothService,
                initialState = BluetoothState(
                    deviceName = "Roberts Waage",
                    bluetoothEnabled = bluetoothService.getBluetoothEnabled()),
                ioDispatcher = Dispatchers.IO,
                mainDispatcher = Dispatchers.Main,
            )
        }
        single<IsTimestampProvider> {
            object: IsTimestampProvider {
                override fun getTimeMillis(): Long {
                    return System.currentTimeMillis()
                }
            }
        }
        if (ProfileProvider.isDevMode){
            single<IsDeviceStore> {
                MockDeviceStore()
            }
        } else {
            single<IsDeviceStore> {
                DeviceStore(
                    bluetoothStore = get(),
                    mainDispatcher = Dispatchers.Main,
                    timestampProvider = get(),
                )
            }
        }
        single {
            WorkoutStore(
                timestampProvider = get(),
                mainDispatcher = Dispatchers.Main,
            )
        }
        single {
            AppStore(
                workoutStore = get(),
                deviceStore = get(),
                mainDispatcher = Dispatchers.Main
            )
        }
    }

    private fun insertKoin() {
        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.ERROR)

            androidContext(this@App)
            modules(appModule)
        }
    }
    companion object
}