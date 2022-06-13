package com.readysetmove.personalworkouts.android

import android.os.Build

object ProfileProvider {
    val isDevMode: Boolean by lazy {
        return@lazy Build.FINGERPRINT.contains("emulator")
    }
}