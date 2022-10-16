package com.readysetmove.personalworkouts.android

import android.os.Build

object ProfileProvider {
    val isDevMode: Boolean by lazy {
        Build.FINGERPRINT.contains("emulator") || Build.FINGERPRINT.contains("emu64a")
    }
}