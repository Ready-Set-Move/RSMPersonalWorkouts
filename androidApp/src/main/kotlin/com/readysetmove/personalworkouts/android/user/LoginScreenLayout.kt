package com.readysetmove.personalworkouts.android.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.readysetmove.personalworkouts.android.R
import com.readysetmove.personalworkouts.android.theme.AppTheme
import com.readysetmove.personalworkouts.app.User

@Composable
fun LoginScreenLayout(user: User?, onLogoutClicked: () -> Unit, onLoginClicked: () -> Unit) {
    val scrollState = rememberScrollState()
    val title = stringResource(R.string.login__title)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                modifier = Modifier.semantics { contentDescription = title }
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier
            .verticalScroll(scrollState)
            .padding(innerPadding)
            .padding(AppTheme.spacings.md)
        ) {
            if (user == null) {
                Button(onClick = onLoginClicked) {
                    Text(text = "Log in")
                }
            } else {
                Text(text = "Logged in as: ${user.displayName}")
                Button(onClick = onLogoutClicked) {
                    Text(text = "Log out")
                }
            }
        }
    }
}

// TODO: Preview