/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.updater.deviceinfo

import android.os.Build
import android.os.SystemProperties
import java.io.File
import com.android.settingslib.DeviceInfoUtils as SettingsLibDeviceInfoUtils

object DeviceInfoUtils : SettingsLibDeviceInfoUtils() {

    private const val PROP_AB_DEVICE = "ro.build.ab_update"
    private const val PROP_ALLOW_MAJOR_UPGRADES = "expressiveos.updater.allow_major_upgrades"
    private const val PROP_BUILD_DATE = "ro.build.date.utc"
    private const val PROP_BUILD_VERSION = "ro.modversion"
    private const val PROP_LEGACY_DEVICE = "ro.evolution.device"
    private const val PROP_LINEAGE_DEVICE = "ro.lineage.device"
    private const val PROP_NEXT_DEVICE = "ro.updater.next_device"
    private const val PROP_PRODUCT_DEVICE = "ro.product.device"
    private const val PROP_UPDATER_ALLOW_DOWNGRADING = "expressiveos.updater.allow_downgrading"
    private const val PROP_UPDATER_URI = "expressiveos.updater.uri"

    // The names these three override props had through 1.2. Still read when the new name is
    // unset, so a test feed set up under the old name keeps working across the update.
    private const val LEGACY_OVERRIDE_PREFIX = "evolution.updater."
    private const val OVERRIDE_PREFIX = "expressiveos.updater."
    private const val PROP_UPDATE_RECOVERY = "persist.vendor.recovery_update"

    private val BUILD_PROP_FILES = listOf(
        "/system/build.prop",
        "/product/etc/build.prop",
        "/system_ext/etc/build.prop",
        "/vendor/build.prop",
    )

    // Read-only
    val androidVersion: String = Build.VERSION.RELEASE

    val buildSecurityPatch: String = Build.VERSION.SECURITY_PATCH

    val sdkLevel: Int = Build.VERSION.SDK_INT

    @JvmStatic
    val buildDateTimestamp: Long = SystemProperties.getLong(PROP_BUILD_DATE, 0)

    @JvmStatic
    val buildVersion: String = romProp(PROP_BUILD_VERSION)

    @JvmStatic
    val device: String = SystemProperties.get(PROP_NEXT_DEVICE)
        .ifEmpty { romProp(PROP_LINEAGE_DEVICE) }
        .ifEmpty { romProp(PROP_LEGACY_DEVICE) }
        .ifEmpty { Build.DEVICE.takeIf { it.isNotBlank() && it != Build.UNKNOWN }.orEmpty() }
        .ifEmpty { romProp(PROP_PRODUCT_DEVICE) }

    @JvmStatic
    val isABDevice: Boolean = SystemProperties.getBoolean(PROP_AB_DEVICE, false)

    // Mutable at runtime
    @JvmStatic
    val isDowngradingAllowed: Boolean
        get() = overrideFlag(PROP_UPDATER_ALLOW_DOWNGRADING)

    @JvmStatic
    val isMajorUpdateAllowed: Boolean
        get() = overrideFlag(PROP_ALLOW_MAJOR_UPGRADES)

    @JvmStatic
    var isRecoveryUpdateEnabled: Boolean
        get() = SystemProperties.getBoolean(PROP_UPDATE_RECOVERY, false)
        set(value) = SystemProperties.set(PROP_UPDATE_RECOVERY, value.toString())

    @JvmStatic
    val updaterUri: String
        get() = overrideProp(PROP_UPDATER_URI)

    // The same spellings SystemProperties.getBoolean takes as true.
    private fun overrideFlag(name: String): Boolean =
        overrideProp(name) in setOf("1", "y", "yes", "on", "true")

    private fun overrideProp(name: String): String =
        SystemProperties.get(name).ifEmpty {
            SystemProperties.get(LEGACY_OVERRIDE_PREFIX + name.removePrefix(OVERRIDE_PREFIX))
        }

    /**
     * Integrity modules (Integrity Box's custom-ROM hiding) delete the ROM's own props at runtime,
     * which left update checks pointing at builds/.json. The build.prop files they came from are
     * untouched, so read the value from there when the runtime prop is gone.
     */
    private fun romProp(name: String): String =
        SystemProperties.get(name).ifEmpty {
            BUILD_PROP_FILES.firstNotNullOfOrNull { path ->
                runCatching {
                    File(path).useLines { lines ->
                        lines.firstOrNull { it.startsWith("$name=") }?.substringAfter('=')?.trim()
                    }
                }.getOrNull()
            }.orEmpty()
        }
}
