/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.expressiveos.updater.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stream
import androidx.compose.material.icons.outlined.SystemSecurityUpdate
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.axion.compose.preferences.ListPreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.preferences.SwitchPreference
import com.android.axion.compose.scaffold.AxionScaffold
import com.android.settingslib.spa.framework.compose.LocalNavController
import java.io.File
import kotlinx.coroutines.launch
import org.expressiveos.updater.R
import org.expressiveos.updater.UpdaterApplication
import org.expressiveos.updater.data.CheckInterval
import org.expressiveos.updater.data.UserPreferencesRepository
import org.expressiveos.updater.deviceinfo.DeviceInfoUtils
import org.expressiveos.updater.util.BatteryMonitor

@Composable
fun PreferencesScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val application = remember(context) { context.applicationContext as UpdaterApplication }
    val repository = application.userPreferencesRepository
    val batteryMonitor = application.batteryMonitor
    val isABDevice = remember { DeviceInfoUtils.isABDevice }
    val showRecoveryUpdate = remember { installRecoveryScriptExists() }

    AxionScaffold(
        title = stringResource(R.string.display_name),
        onBackClick = { navController.navigateBack() },
        collapsedByDefault = false,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        PreferencesContent(
            repository = repository,
            batteryMonitor = batteryMonitor,
            isABDevice = isABDevice,
            showRecoveryUpdate = showRecoveryUpdate,
            contentPadding = paddingValues,
        )
    }
}

@Composable
private fun PreferencesContent(
    repository: UserPreferencesRepository,
    batteryMonitor: BatteryMonitor,
    isABDevice: Boolean,
    showRecoveryUpdate: Boolean,
    contentPadding: PaddingValues,
) {
    val coroutineScope = rememberCoroutineScope()
    val abPerfMode by repository.abPerfModeFlow.collectAsStateWithLifecycle(false)
    val batteryState by batteryMonitor.batteryState.collectAsStateWithLifecycle(
        batteryMonitor.currentBatteryState
    )
    val autoDelete by repository.autoDeleteFlow.collectAsStateWithLifecycle(true)
    val streamUpdates by repository.streamUpdatesFlow.collectAsStateWithLifecycle(true)
    val checkInterval by repository.checkIntervalFlow.collectAsStateWithLifecycle(CheckInterval.default)
    val meteredNetworkWarning by repository.meteredNetworkWarningFlow.collectAsStateWithLifecycle(
        true
    )
    val periodicCheckEnabled by repository.periodicCheckEnabledFlow.collectAsStateWithLifecycle(true)
    val showLogo by repository.showLogoFlow.collectAsStateWithLifecycle(true)
    val showUpdaterIcon by repository.showUpdaterIconFlow.collectAsStateWithLifecycle(false)
    var recoveryUpdateEnabled by remember { mutableStateOf(repository.getRecoveryUpdateEnabled()) }

    val intervalOptions = listOf(
        CheckInterval.DAILY.name to stringResource(R.string.time_unit_day),
        CheckInterval.WEEKLY.name to stringResource(R.string.time_unit_week),
        CheckInterval.MONTHLY.name to stringResource(R.string.time_unit_month),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            PreferenceGroup(title = stringResource(R.string.pref_category_background_sync)) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.menu_auto_updates_check),
                        summary = stringResource(R.string.menu_auto_updates_check_summary),
                        checked = periodicCheckEnabled,
                        icon = Icons.Outlined.CloudSync,
                        onCheckedChange = { value ->
                            coroutineScope.launch { repository.setPeriodicCheckEnabled(value) }
                        },
                    )
                }
                item {
                    ListPreference(
                        title = stringResource(R.string.menu_auto_updates_check_interval),
                        options = intervalOptions,
                        value = checkInterval.name,
                        enabled = periodicCheckEnabled,
                        icon = Icons.Outlined.Schedule,
                        onValueChange = { selectedName ->
                            val interval = runCatching { CheckInterval.valueOf(selectedName) }
                                .getOrDefault(CheckInterval.default)
                            coroutineScope.launch { repository.setCheckInterval(interval) }
                        },
                    )
                }
            }
        }

        item {
            PreferenceGroup(title = stringResource(R.string.pref_category_download_install)) {
                if (isABDevice) {
                    item {
                        SwitchPreference(
                            title = stringResource(R.string.menu_stream_updates),
                            summary = stringResource(R.string.menu_stream_updates_summary),
                            checked = streamUpdates,
                            icon = Icons.Outlined.Stream,
                            onCheckedChange = { value ->
                                coroutineScope.launch { repository.setStreamUpdates(value) }
                            },
                        )
                    }
                } else {
                    item {
                        SwitchPreference(
                            title = stringResource(R.string.menu_auto_delete_updates),
                            summary = stringResource(R.string.menu_auto_delete_updates_summary),
                            checked = autoDelete,
                            icon = Icons.Outlined.DeleteSweep,
                            onCheckedChange = { value ->
                                coroutineScope.launch { repository.setAutoDelete(value) }
                            },
                        )
                    }
                }

                item {
                    SwitchPreference(
                        title = stringResource(R.string.menu_metered_network_warning),
                        summary = stringResource(R.string.menu_metered_network_warning_summary),
                        checked = meteredNetworkWarning,
                        icon = Icons.Outlined.NetworkCheck,
                        onCheckedChange = { value ->
                            coroutineScope.launch { repository.setMeteredNetworkWarning(value) }
                        },
                    )
                }

                if (isABDevice) {
                    item {
                        val acCharging = batteryState.isAcCharging
                        SwitchPreference(
                            title = stringResource(R.string.menu_ab_perf_mode),
                            summary = if (acCharging) {
                                stringResource(R.string.menu_ab_perf_mode_summary_charging)
                            } else {
                                stringResource(R.string.menu_ab_perf_mode_summary)
                            },
                            checked = acCharging || abPerfMode,
                            enabled = !acCharging,
                            icon = Icons.Outlined.Speed,
                            onCheckedChange = { value ->
                                coroutineScope.launch { repository.setAbPerfMode(value) }
                            },
                        )
                    }
                }

                if (showRecoveryUpdate) {
                    item {
                        SwitchPreference(
                            title = stringResource(R.string.menu_update_recovery),
                            summary = stringResource(R.string.menu_update_recovery_summary),
                            checked = recoveryUpdateEnabled,
                            icon = Icons.Outlined.SystemSecurityUpdate,
                            onCheckedChange = { value ->
                                recoveryUpdateEnabled = value
                                repository.setRecoveryUpdateEnabled(value)
                            },
                        )
                    }
                }
            }
        }

        item {
            PreferenceGroup(title = stringResource(R.string.pref_category_appearance)) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.menu_show_logo),
                        summary = stringResource(R.string.menu_show_logo_summary),
                        checked = showLogo,
                        icon = Icons.Outlined.AutoAwesome,
                        onCheckedChange = { value ->
                            coroutineScope.launch { repository.setShowLogo(value) }
                        },
                    )
                }
                item {
                    SwitchPreference(
                        title = stringResource(R.string.menu_show_updater_icon),
                        summary = stringResource(R.string.menu_show_updater_icon_summary),
                        checked = showUpdaterIcon,
                        icon = Icons.Outlined.SystemUpdate,
                        onCheckedChange = { value ->
                            coroutineScope.launch { repository.setShowUpdaterIcon(value) }
                        },
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun installRecoveryScriptExists() = File("/vendor/bin/install-recovery.sh").exists()
