/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.expressiveos.updater.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.axion.compose.expressive.ExpressiveMorphLoadingIndicator
import com.android.axion.compose.expressive.rememberExpressiveReducedMotion
import com.android.axion.compose.theme.AxionTheme
import com.android.settingslib.spa.debug.UiModePreviews
import org.expressiveos.updater.R

private val AnimationSize = 280.dp

@Composable
fun UpdateCheckAnimation(modifier: Modifier = Modifier) {
    val reducedMotion = rememberExpressiveReducedMotion()

    if (reducedMotion) {
        Box(
            modifier = modifier.size(AnimationSize),
            contentAlignment = Alignment.Center,
        ) {
            ExpressiveMorphLoadingIndicator(
                size = 96.dp,
                contained = true,
            )
        }
    } else {
        Lottie(
            resId = R.raw.loading_shapes_expressive,
            modifier = modifier.size(AnimationSize),
        )
    }
}

@UiModePreviews
@Composable
private fun UpdateCheckAnimationPreview() {
    AxionTheme {
        UpdateCheckAnimation()
    }
}
