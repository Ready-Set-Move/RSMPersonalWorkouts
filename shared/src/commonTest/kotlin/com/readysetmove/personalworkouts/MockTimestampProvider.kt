package com.readysetmove.personalworkouts

class MockTimestampProvider(timestamps: LongProgression = 0..1000L step 100): IsTimestampProvider {
    private val _timestamps = timestamps.iterator()

    override fun getTimeMillis(): Long {
        return _timestamps.next()
    }
}