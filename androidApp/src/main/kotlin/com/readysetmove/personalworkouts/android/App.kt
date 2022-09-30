package com.readysetmove.personalworkouts.android

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.readysetmove.personalworkouts.IsTimestampProvider
import com.readysetmove.personalworkouts.app.AppState
import com.readysetmove.personalworkouts.app.AppStore
import com.readysetmove.personalworkouts.bluetooth.AndroidBluetoothService
import com.readysetmove.personalworkouts.bluetooth.BluetoothState
import com.readysetmove.personalworkouts.bluetooth.BluetoothStore
import com.readysetmove.personalworkouts.device.DeviceStore
import com.readysetmove.personalworkouts.device.IsDeviceStore
import com.readysetmove.personalworkouts.device.MockDeviceStore
import com.readysetmove.personalworkouts.workout.IsWorkoutRepository
import com.readysetmove.personalworkouts.workout.WorkoutRepository
import com.readysetmove.personalworkouts.workout.progress.WorkoutProgressStore
import com.readysetmove.personalworkouts.workout.results.IsWorkoutResultsRepository
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsRepository
import com.readysetmove.personalworkouts.workout.results.WorkoutResultsStore
import com.readysetmove.personalworkouts.workout.tracking.TractionTrackingStore
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import java.time.Instant

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        insertKoin()
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        } else {
            // TODO: prod logging
        }
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
            object : IsTimestampProvider {
                override fun getTimeMillis(): Long {
                    return System.currentTimeMillis()
                }

                override fun getCurrentDate(): String {
                    return Instant.now().toString()
                }
            }
        }
        single<IsWorkoutRepository> {
            WorkoutRepository(timestampProvider = get())
        }
        single<IsWorkoutResultsRepository> {
            WorkoutResultsRepository()
        }
        if (ProfileProvider.isDevMode) {
            single<IsDeviceStore> {
                MockDeviceStore(mainDispatcher = Dispatchers.Main)
            }
        } else {
            single<IsDeviceStore> {
                DeviceStore(
                    bluetoothStore = get(),
                    mainDispatcher = Dispatchers.Main,
                )
            }
        }
        single {
            WorkoutResultsStore(
                workoutResultsRepository = get(),
                mainDispatcher = Dispatchers.Main,
            )
        }
        single {
            WorkoutProgressStore(
                timestampProvider = get(),
                mainDispatcher = Dispatchers.Main,
            )
        }
        single(createdAtStart = true) {
            TractionTrackingStore(
                deviceStore = get(),
                timestampProvider = get(),
                workoutResultsStore = get(),
                mainDispatcher = Dispatchers.Main,
                workoutProgressStore = get(),
            )
        }
        single {
//            FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            AppStore(
                workoutRepository = get(),
                mainDispatcher = Dispatchers.Main,
                initialState = AppState(
                    user = FirebaseAuth.getInstance().currentUser.toUser()
                )
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