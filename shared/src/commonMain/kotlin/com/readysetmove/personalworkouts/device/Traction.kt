package com.readysetmove.personalworkouts.device

data class Traction(val timestamp: Long, val value: Float)

fun List<Traction>.getMedianTraction(): Float {
    return sortedBy {
        it.value
    }.let {
        if (it.size % 2 == 0) {
            val left = it[it.size/2]
            val right = it[it.size/2 + 1]
            (left.value + right.value)/2
        } else {
            it[(it.size/2) + 1].value
        }
    }
}