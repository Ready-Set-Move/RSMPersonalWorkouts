package com.readysetmove.personalworkouts.android.user

import android.app.Activity.RESULT_OK
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import com.readysetmove.personalworkouts.android.toUser
import com.readysetmove.personalworkouts.app.AppAction
import com.readysetmove.personalworkouts.app.AppStore
import io.github.aakira.napier.Napier
import org.koin.androidx.compose.get

object LoginScreen {
    const val ROUTE = "login"
}

@Composable
fun LoginScreen(appStore: AppStore = get()) {
    val context = LocalContext.current
    val state = appStore.observeState().collectAsState()

    // * https://firebase.google.com/docs/auth/android/firebaseui#kotlin+ktx
    // * delete account?
    val signInLauncher = rememberLauncherForActivityResult(contract = FirebaseAuthUIActivityResultContract()) { result ->
        when(result.resultCode) {
            RESULT_OK -> {
                Toast.makeText(context, "Sign in successful", Toast.LENGTH_SHORT).show()
                FirebaseAuth.getInstance().currentUser?.toUser()?.let {
                    Napier.d("Sign in successful: $it")
                    appStore.dispatch(AppAction.SetUser(it))
                }
            }
            else -> {
                val response = result.idpResponse
                if (response != null) {
                    // TODO: sign in error handling
                    Toast.makeText(context, "Sign in failed", Toast.LENGTH_LONG).show()
                    Napier.e("Could not sign in: code - ${result.resultCode} | error - ${response.error}")
                }
            }
        }
    }

    LoginScreenLayout(user = state.value.user, onLogoutClicked = {
        AuthUI.getInstance().signOut(context).addOnCompleteListener {
            when (true) {
                it.isSuccessful -> {
                    Toast.makeText(context, "Sign out successful", Toast.LENGTH_LONG).show()
                    appStore.dispatch(AppAction.UnsetUser)
                }
                else -> {
                    Toast.makeText(context, "Sign out failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }) {
        signInLauncher.launch(AuthUI.getInstance()
            .createSignInIntentBuilder()
            // TODO: set TOS and privacy
            .setAvailableProviders(arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
            ))
            .build())
    }
}