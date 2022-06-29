package com.readysetmove.personalworkouts.android

import com.google.firebase.auth.FirebaseUser
import com.readysetmove.personalworkouts.app.User

fun FirebaseUser?.toUser(): User? {
    if (this == null) return null
    val userName = when {
        !displayName.isNullOrBlank() -> displayName
        !email.isNullOrBlank() -> email
        !phoneNumber.isNullOrBlank() -> phoneNumber
        else -> uid
    }
    return User(
        displayName = userName ?: this.uid,
        id = this.uid,
    )
}
