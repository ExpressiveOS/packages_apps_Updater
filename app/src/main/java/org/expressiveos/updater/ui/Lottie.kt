/*
 * SPDX-FileCopyrightText: The Android Open Source Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.expressiveos.updater.ui

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.android.axion.compose.expressive.rememberExpressiveReducedMotion
import kotlinx.coroutines.isActive

@Composable
fun Lottie(
    resId: Int,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
) {
    Lottie(
        spec = LottieCompositionSpec.RawRes(resId),
        modifier = modifier,
        iterations = iterations,
    )
}

@Composable
fun Lottie(
    spec: LottieCompositionSpec,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
) {
    val reducedMotion = rememberExpressiveReducedMotion()
    val animationsDisabled = reducedMotion || !ValueAnimator.areAnimatorsEnabled()
    val composition by rememberLottieComposition(spec)
    var progress by rememberSaveable(spec) { mutableStateOf(0f) }

    LaunchedEffect(composition, iterations, animationsDisabled) {
        if (animationsDisabled) {
            progress = 0f
            return@LaunchedEffect
        }
        val comp = composition ?: return@LaunchedEffect
        val duration = comp.duration.coerceAtLeast(1f)
        var anchorNanos = -1L

        while (isActive && ValueAnimator.areAnimatorsEnabled()) {
            val elapsed = withFrameNanos { now ->
                if (anchorNanos < 0) {
                    anchorNanos = now - (progress * duration * 1_000_000f).toLong()
                }
                (now - anchorNanos) / 1_000_000f
            }

            progress = (elapsed % duration) / duration

            if (iterations != LottieConstants.IterateForever &&
                elapsed / duration >= iterations
            ) {
                progress = 1f
                break
            }
        }
        if (!ValueAnimator.areAnimatorsEnabled()) {
            progress = 0f
        }
    }

    LottieAnimation(
        composition = composition,
        modifier = modifier,
        dynamicProperties = LottieColorUtils.getDefaultDynamicProperties(),
        progress = { if (animationsDisabled) 0f else progress },
    )
}
