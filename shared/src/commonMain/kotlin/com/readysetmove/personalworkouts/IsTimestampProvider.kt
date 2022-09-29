package com.readysetmove.personalworkouts

interface IsTimestampProvider {
    fun getTimeMillis(): Long

    fun getCurrentDate(): String
}