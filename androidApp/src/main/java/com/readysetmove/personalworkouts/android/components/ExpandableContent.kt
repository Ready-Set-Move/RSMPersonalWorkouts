package com.readysetmove.personalworkouts.android.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable

@Composable
fun ExpandableContent(
    expanded: Boolean,
    additionalContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        content()
        AnimatedVisibility(visible = expanded) {
            additionalContent()
        }
    }
}
