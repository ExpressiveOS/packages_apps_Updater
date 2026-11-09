/*
 * SPDX-FileCopyrightText: The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.updater.util

import android.content.Context
import android.content.Intent

/**
 * Root apps that can root the updated slot before the reboot, with their
 * "Install to inactive slot (After OTA)" option. ExpressiveOS ships no root: this only lets the
 * Updater remind someone who rooted their own phone to use that option before rebooting, or the
 * updated system starts without root. Each package is also listed under <queries> in the
 * manifest, or the app can't see it.
 */
object RootApps {
    private val PACKAGES = listOf(
        "com.topjohnwu.magisk", // Magisk
        "com.rifsxd.ksunext", // KernelSU Next
        "com.sukisu.ultra", // SukiSU Ultra
        "me.weishu.kernelsu", // KernelSU
        "me.bmax.apatch", // APatch
    )

    data class RootApp(val label: String, val launchIntent: Intent)

    /** The first root app installed, or null (also when a root app hides under another name) */
    @JvmStatic
    fun find(context: Context): RootApp? {
        val pm = context.packageManager
        for (pkg in PACKAGES) {
            val intent = pm.getLaunchIntentForPackage(pkg) ?: continue
            val label = runCatching {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrDefault(pkg)
            return RootApp(label, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return null
    }
}
