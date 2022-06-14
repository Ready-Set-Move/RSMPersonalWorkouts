package com.readysetmove.personalworkouts

class MockTimestampProvider(val timestamps: ArrayDeque<Long> = ArrayDeque()): IsTimestampProvider {

    override fun getTimeMillis(): Long {
        return timestamps.removeFirst()
    }
}