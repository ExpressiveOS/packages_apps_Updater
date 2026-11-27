/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)

package org.expressiveos.updater.ui

import android.content.Intent
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.android.axion.compose.expressive.ExpressiveFloatingToolbar
import com.android.axion.compose.expressive.ExpressiveMorphButton
import com.android.axion.compose.expressive.ExpressiveMorphLoadingIndicator
import com.android.axion.compose.expressive.ExpressiveShapePreset
import com.android.axion.compose.expressive.ExpressiveToolbarPrimaryPill
import com.android.axion.compose.expressive.ExpressiveWavyLinearProgress
import com.android.axion.compose.expressive.rememberExpressiveHaptics
import com.android.axion.compose.preferences.BasePreference
import com.android.axion.compose.preferences.ExpressiveShapes
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.preferences.PreferencePosition
import com.android.axion.compose.preferences.PreferenceTokens
import com.android.axion.compose.preferences.preferenceShape
import com.android.axion.compose.scaffold.ExpressiveBackButton
import com.android.axion.compose.theme.AxionTheme
import com.android.axion.compose.theme.withTabularNums
import com.android.settingslib.spa.debug.UiModePreviews
import java.util.Date
import java.util.Locale
import org.expressiveos.updater.R
import org.expressiveos.updater.data.ChangelogState
import org.expressiveos.updater.data.SupportInfo
import org.expressiveos.updater.deviceinfo.DeviceInfoUtils
import org.expressiveos.updater.updates.action.UpdateAction
import org.expressiveos.updater.updates.action.UpdateActionType
import org.expressiveos.updater.updates.state.ProgressState
import org.expressiveos.updater.updates.state.UpdateItemState
import org.expressiveos.updater.util.RootApps
import org.expressiveos.updater.util.StringUtil

private val ContentMaxWidth = 600.dp
private val HorizontalPadding = 20.dp
private val TopLogoHeight = 40.dp

@Composable
fun SystemUpdateScreen(
    headline: String,
    supportingText: String?,
    supportingTextIsError: Boolean = false,
    isBusy: Boolean,
    canCheckForUpdates: Boolean,
    showDeviceInfo: Boolean,
    lastCheckedTimestamp: Long,
    onBackClick: () -> Unit,
    onCheckClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    modifier: Modifier = Modifier,
    updateItem: UpdateItemState? = null,
    supportInfo: SupportInfo = SupportInfo(),
    showLogo: Boolean = true,
    showUpdaterIcon: Boolean = false,
    changelogState: ChangelogState = ChangelogState.Idle,
    onUpdateAction: (UpdateAction) -> Unit = {},
) {
    val showCheckButton = !isBusy && updateItem == null
    val isActivelyWorking = isBusy || updateItem?.progress != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SystemUpdateTopBar(
                onBackClick = onBackClick,
                showCheckAction = !showCheckButton,
                canCheckForUpdates = canCheckForUpdates,
                onCheckClick = onCheckClick,
                onLocalUpdateClick = onLocalUpdateClick,
                onPreferencesClick = onPreferencesClick,
                showUpdaterIcon = showUpdaterIcon,
                showLogo = showLogo,
                activePulse = isActivelyWorking,
                supportInfo = supportInfo,
                updateOverflowActions = updateItem?.actions?.overflow ?: emptyList(),
                onUpdateAction = onUpdateAction,
            )
        },
        bottomBar = {
            if (showCheckButton) {
                CheckForUpdateFloatingBar(
                    enabled = canCheckForUpdates,
                    onClick = onCheckClick,
                )
            } else if (!isBusy && updateItem != null) {
                UpdateActionFloatingBar(
                    item = updateItem,
                    onAction = onUpdateAction,
                )
            }
        },
    ) { paddingValues ->
        if (isBusy) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = HorizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = ContentMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ExpressiveHeroEmblem(
                        active = true,
                        icon = Icons.Outlined.Refresh,
                        size = 76.dp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ScreenHeader(
                        headline = headline,
                        centered = true,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ExpressiveWavyLinearProgress(
                        progress = null,
                        modifier = Modifier.fillMaxWidth(),
                        strokeWidth = 5.dp,
                        amplitudeDp = 3.dp,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    UpdateCheckAnimation()
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = HorizontalPadding),
            ) {
                HeroHeaderSection(
                    headline = headline,
                    supportingText = supportingText,
                    supportingTextIsError = supportingTextIsError,
                    showLogoBanner = showDeviceInfo && showLogo && showUpdaterIcon,
                    updateItem = updateItem,
                    active = isActivelyWorking,
                )

                if (showDeviceInfo || lastCheckedTimestamp > 0) {
                    Spacer(modifier = Modifier.height(24.dp))
                    DeviceStatusSegmentedGroup(
                        showDeviceInfo = showDeviceInfo,
                        maintainer = supportInfo.maintainer,
                        lastCheckedTimestamp = lastCheckedTimestamp,
                    )
                }

                updateItem?.let { item ->
                    Spacer(modifier = Modifier.height(20.dp))
                    UpdateDetailsSection(
                        item = item,
                        changelogState = changelogState,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun HeroHeaderSection(
    headline: String,
    supportingText: String?,
    supportingTextIsError: Boolean,
    showLogoBanner: Boolean,
    updateItem: UpdateItemState?,
    active: Boolean,
) {
    val heroIcon = when {
        supportingTextIsError -> Icons.Outlined.ErrorOutline
        updateItem?.actions?.primary?.type == UpdateActionType.REBOOT -> Icons.Outlined.RestartAlt
        updateItem?.progress != null -> Icons.Outlined.SystemUpdateAlt
        updateItem != null -> Icons.Outlined.Download
        else -> Icons.Outlined.CheckCircle
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(PreferenceTokens.CornerRadiusOuter),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showLogoBanner) {
                ExpressiveLogoGlow(
                    contentDescription = stringResource(R.string.brand_name),
                    activePulse = active,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
            }

            ExpressiveHeroEmblem(
                active = active,
                icon = heroIcon,
                size = 72.dp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            ScreenHeader(
                headline = headline,
                centered = true,
            )

            val statusBadgeText = updateItem?.status?.takeIf { it.isNotEmpty() }
            if (statusBadgeText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                MetricPillChip(
                    text = statusBadgeText,
                    icon = Icons.Outlined.Verified,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            supportingText?.let { text ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = CircleShape,
                    color = if (supportingTextIsError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = if (supportingTextIsError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateDetailsSection(
    item: UpdateItemState,
    changelogState: ChangelogState,
) {
    val context = LocalContext.current
    val rootApp = remember(context, item.downloadId, item.actions.primary.type) {
        RootApps.find(context)
    }
    val showRootRetentionCard = rootApp != null && (
        item.actions.primary.type == UpdateActionType.REBOOT ||
            item.actions.primary.type == UpdateActionType.PAUSE_INSTALL ||
            item.actions.primary.type == UpdateActionType.RESUME_INSTALL
        )

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item.progress?.let { progress ->
            WavyProgressCard(progress = progress)
        }

        if (showRootRetentionCard && rootApp != null) {
            RootRetentionReminderCard(rootApp = rootApp)
        }

        BuildMetadataSegmentedGroup(item = item)

        ChangelogSegmentedSection(changelogState = changelogState)
    }
}

@Composable
private fun WavyProgressCard(progress: ProgressState) {
    val configuration = LocalConfiguration.current
    val locale = remember(configuration.locales) {
        configuration.locales.get(0) ?: Locale.getDefault()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PreferenceTokens.CornerRadiusOuter),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            when (progress) {
                is ProgressState.Determinate -> {
                    val clampedPercent = progress.percent.coerceIn(0f, 100f)
                    val formattedPercent = remember(clampedPercent, locale) {
                        String.format(locale, "%.0f%%", clampedPercent)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formattedPercent,
                            style = updaterTabularHeadlineStyle(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (progress.speed.isNotEmpty()) {
                            MetricPillChip(
                                text = progress.speed,
                                icon = Icons.Outlined.Speed,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                tabular = true,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ExpressiveWavyLinearProgress(
                        progress = clampedPercent / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        amplitudeDp = 3.dp,
                        strokeWidth = 6.dp,
                    )

                    val hasMetaChips = progress.downloadedSize.isNotEmpty() || progress.eta.isNotEmpty()
                    if (hasMetaChips) {
                        Spacer(modifier = Modifier.height(14.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (progress.downloadedSize.isNotEmpty()) {
                                MetricPillChip(
                                    text = progress.downloadedSize,
                                    icon = Icons.Outlined.FolderZip,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    tabular = true,
                                )
                            }
                            if (progress.eta.isNotEmpty()) {
                                MetricPillChip(
                                    text = progress.eta,
                                    icon = Icons.Outlined.Timer,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    tabular = true,
                                )
                            }
                        }
                    }
                }

                ProgressState.Indeterminate -> {
                    ExpressiveWavyLinearProgress(
                        progress = null,
                        modifier = Modifier.fillMaxWidth(),
                        amplitudeDp = 3.dp,
                        strokeWidth = 6.dp,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    MetricPillChip(
                        text = stringResource(R.string.downloading_installing_caption),
                        icon = Icons.Outlined.SystemUpdateAlt,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPillChip(
    text: String,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    tabular: Boolean = false,
) {
    val labelStyle = if (tabular) {
        updaterTabularLabelStyle()
    } else {
        MaterialTheme.typography.labelLargeEmphasized
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            style = labelStyle,
            color = contentColor,
        )
    }
}

@Composable
private fun RootRetentionReminderCard(rootApp: RootApps.RootApp) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PreferenceTokens.CornerRadiusOuter),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.keep_root_dialog_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.keep_root_notification, rootApp.label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.height(14.dp))
            ExpressiveMorphButton(
                onClick = {
                    runCatching { context.startActivity(rootApp.launchIntent) }
                },
                selected = false,
                restingPreset = ExpressiveShapePreset.PILL,
                pressedPreset = ExpressiveShapePreset.SQUIRCLE,
                containerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                contentColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            ) {
                Text(text = stringResource(R.string.keep_root_open_app, rootApp.label))
            }
        }
    }
}

@Composable
private fun BuildMetadataSegmentedGroup(item: UpdateItemState) {
    val showsInstallInfo = item.progress == null &&
        item.actions.primary.type in setOf(
            UpdateActionType.START_DOWNLOAD,
            UpdateActionType.START_INSTALL,
        )

    val sourceChannelLabel = when {
        item.isImportedLocal -> stringResource(R.string.local_update_name)
        item.channel.isNotEmpty() -> item.channel.uppercase(Locale.ROOT)
        item.isLocal -> stringResource(R.string.download_completed_notification)
        else -> "OTA"
    }

    val packageSummary = buildList {
        add(sourceChannelLabel)
        if (item.fileSize.isNotEmpty()) {
            add(stringResource(R.string.list_update_size, item.fileSize))
        }
        if (item.androidUpdateInfo.isNotEmpty()) {
            add(item.androidUpdateInfo)
        }
    }.joinToString(" • ")

    PreferenceGroup {
        item {
            BasePreference(
                title = item.buildVersion,
                summary = item.buildDate,
                icon = SystemUpdateIcon,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
            )
        }

        item {
            BasePreference(
                title = sourceChannelLabel,
                summary = packageSummary,
                icon = Icons.Outlined.FolderZip,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                widget = if (item.fileSize.isNotEmpty()) {
                    {
                        MetricPillChip(
                            text = item.fileSize,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tabular = true,
                        )
                    }
                } else {
                    null
                },
            )
        }

        if (item.securityUpdate.isNotEmpty()) {
            item {
                BasePreference(
                    title = stringResource(R.string.security_update),
                    summary = item.securityUpdate,
                    icon = Icons.Outlined.Security,
                )
            }
        }

        item {
            val installNoteText = if (item.installNote == R.string.list_major_upgrade_recovery_install) {
                stringResource(item.installNote, stringResource(R.string.brand_name))
            } else {
                stringResource(item.installNote)
            }
            BasePreference(
                title = installNoteText,
                summary = if (showsInstallInfo) {
                    stringResource(R.string.metered_warning_footnote)
                } else {
                    null
                },
                icon = if (showsInstallInfo) Icons.Outlined.NetworkCheck else Icons.Outlined.Info,
            )
        }
    }
}

@Composable
private fun ChangelogSegmentedSection(changelogState: ChangelogState) {
    if (changelogState is ChangelogState.Idle) return

    Column(verticalArrangement = Arrangement.spacedBy(PreferenceTokens.ItemSpacing)) {
        BasePreference(
            title = stringResource(R.string.changelog_view_all),
            icon = Icons.Outlined.Description,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
            position = PreferencePosition.Top,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = preferenceShape(PreferencePosition.Bottom),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                when (changelogState) {
                    ChangelogState.Idle -> Unit
                    ChangelogState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ExpressiveMorphLoadingIndicator(
                                size = 32.dp,
                                contained = true,
                            )
                            Text(
                                text = stringResource(R.string.changelog_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    ChangelogState.Error -> Text(
                        text = stringResource(R.string.changelog_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )

                    is ChangelogState.Loaded -> if (changelogState.markdown.isEmpty()) {
                        Text(
                            text = stringResource(R.string.changelog_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        MarkdownText(
                            markdown = changelogState.markdown,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceStatusSegmentedGroup(
    showDeviceInfo: Boolean,
    maintainer: String?,
    lastCheckedTimestamp: Long,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = remember(context, configuration.locales) { StringUtil.getCurrentLocale(context) }
    val securityPatch = remember(locale) {
        runCatching {
            StringUtil.formatSecurityPatchFullDate(context, DeviceInfoUtils.buildSecurityPatch)
        }.getOrDefault(DeviceInfoUtils.buildSecurityPatch)
    }
    val buildVersion = remember { DeviceInfoUtils.buildVersion }
    val lastCheckedText = remember(lastCheckedTimestamp) {
        if (lastCheckedTimestamp <= 0L) {
            ""
        } else {
            val date = DateUtils.formatDateTime(
                context,
                lastCheckedTimestamp,
                DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_ABBREV_MONTH or
                    DateUtils.FORMAT_NO_YEAR,
            )
            val time = DateFormat.getTimeFormat(context).format(Date(lastCheckedTimestamp))
            "$date ($time)"
        }
    }

    PreferenceGroup {
        if (showDeviceInfo) {
            if (buildVersion.isNotEmpty()) {
                item {
                    BasePreference(
                        title = stringResource(R.string.brand_name),
                        summary = buildVersion,
                        icon = SystemUpdateIcon,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                }
            }
            item {
                BasePreference(
                    title = stringResource(R.string.header_android_version, DeviceInfoUtils.androidVersion),
                    summary = stringResource(R.string.header_security_update_full, securityPatch),
                    icon = Icons.Outlined.Android,
                )
            }
            if (!maintainer.isNullOrBlank()) {
                item {
                    BasePreference(
                        title = stringResource(R.string.maintainer_name, maintainer),
                        icon = Icons.Outlined.Person,
                    )
                }
            }
        }

        if (lastCheckedText.isNotEmpty()) {
            item {
                BasePreference(
                    title = stringResource(R.string.header_last_check),
                    summary = lastCheckedText,
                    icon = Icons.Outlined.History,
                    widget = {
                        Text(
                            text = DateFormat.getTimeFormat(context).format(Date(lastCheckedTimestamp)),
                            style = MaterialTheme.typography.labelMediumEmphasized.withTabularNums(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun CheckForUpdateFloatingBar(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = HorizontalPadding, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        ExpressiveFloatingToolbar(
            frosted = true,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
            ExpressiveToolbarPrimaryPill(
                label = stringResource(R.string.check_for_update),
                icon = Icons.Outlined.Refresh,
                enabled = enabled,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun UpdateActionFloatingBar(
    item: UpdateItemState,
    onAction: (UpdateAction) -> Unit,
) {
    val context = LocalContext.current
    val primary = item.actions.primary
    val primaryLabel = when (primary.type) {
        UpdateActionType.START_DOWNLOAD -> stringResource(R.string.action_download_install)
        else -> primary.type.title(context)
    }
    val primaryIcon = when (primary.type) {
        UpdateActionType.START_DOWNLOAD -> Icons.Outlined.Download
        UpdateActionType.PAUSE_DOWNLOAD,
        UpdateActionType.PAUSE_INSTALL -> Icons.Outlined.Pause
        UpdateActionType.RESUME_DOWNLOAD,
        UpdateActionType.RESUME_INSTALL -> Icons.Outlined.PlayArrow
        UpdateActionType.START_INSTALL -> Icons.Outlined.SystemUpdateAlt
        UpdateActionType.REBOOT -> Icons.Outlined.RestartAlt
        UpdateActionType.OPEN_GUIDE -> Icons.Outlined.OpenInNew
        UpdateActionType.CANCEL_INSTALL,
        UpdateActionType.CANCEL_DOWNLOAD -> Icons.Outlined.Close
        else -> Icons.Outlined.Info
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = HorizontalPadding, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        ExpressiveFloatingToolbar(
            frosted = true,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
            item.actions.secondary?.let { secondary ->
                ExpressiveMorphButton(
                    onClick = { onAction(secondary) },
                    enabled = secondary.enabled,
                    selected = false,
                    restingPreset = ExpressiveShapePreset.PILL,
                    pressedPreset = ExpressiveShapePreset.SQUIRCLE,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(text = secondary.type.title(context))
                }
            }

            ExpressiveToolbarPrimaryPill(
                label = primaryLabel,
                icon = primaryIcon,
                enabled = primary.enabled,
                onClick = { onAction(primary) },
            )
        }
    }
}

@Composable
private fun ScreenHeader(
    headline: String,
    centered: Boolean = false,
) {
    Text(
        text = headline,
        style = MaterialTheme.typography.displaySmallEmphasized,
        fontFamily = DisplaySmallEmphasizedFontFamily,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SystemUpdateTopBar(
    onBackClick: () -> Unit,
    showCheckAction: Boolean,
    canCheckForUpdates: Boolean,
    onCheckClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    showUpdaterIcon: Boolean,
    showLogo: Boolean,
    activePulse: Boolean = false,
    supportInfo: SupportInfo = SupportInfo(),
    updateOverflowActions: List<UpdateAction> = emptyList(),
    onUpdateAction: (UpdateAction) -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = rememberExpressiveHaptics()
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = HorizontalPadding, vertical = 8.dp),
    ) {
        ExpressiveBackButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        if (showUpdaterIcon) {
            Icon(
                imageVector = SystemUpdateIcon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(TopLogoHeight),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else if (showLogo) {
            ExpressiveLogoGlow(
                contentDescription = stringResource(R.string.brand_name),
                activePulse = activePulse,
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(TopLogoHeight),
            )
        }

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            FilledTonalIconButton(
                onClick = {
                    haptics.performStepTick()
                    menuExpanded = true
                },
                shape = IconButtonDefaults.smallRoundShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.menu_more_options),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = ExpressiveShapes.large,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                if (showCheckAction) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.check_for_update)) },
                        enabled = canCheckForUpdates,
                        onClick = {
                            haptics.performConfirm()
                            menuExpanded = false
                            onCheckClick()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.local_update_import)) },
                    onClick = {
                        haptics.performStepTick()
                        menuExpanded = false
                        onLocalUpdateClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_preferences)) },
                    onClick = {
                        haptics.performStepTick()
                        menuExpanded = false
                        onPreferencesClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.report_issues)) },
                    onClick = {
                        haptics.performStepTick()
                        menuExpanded = false
                        val url = context.getString(R.string.report_issue_url)
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    },
                )
                listOf(
                    R.string.support_forum to supportInfo.forum,
                    R.string.support_firmware to supportInfo.firmware,
                    R.string.support_donate to supportInfo.paypal,
                ).forEach { (titleRes, url) ->
                    if (url == null) return@forEach
                    DropdownMenuItem(
                        text = { Text(stringResource(titleRes)) },
                        onClick = {
                            haptics.performStepTick()
                            menuExpanded = false
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        },
                    )
                }
                updateOverflowActions.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.type.title(context)) },
                        enabled = action.enabled,
                        onClick = {
                            haptics.performConfirm()
                            menuExpanded = false
                            onUpdateAction(action)
                        },
                    )
                }
            }
        }
    }
}

@UiModePreviews
@Composable
private fun SystemUpdateScreenUpToDatePreview() {
    AxionTheme {
        SystemUpdateScreen(
            headline = "Your system is up to date",
            supportingText = null,
            isBusy = false,
            canCheckForUpdates = true,
            showDeviceInfo = true,
            lastCheckedTimestamp = 1_754_000_000_000L,
            onBackClick = {},
            onCheckClick = {},
            onLocalUpdateClick = {},
            onPreferencesClick = {},
        )
    }
}

@UiModePreviews
@Composable
private fun SystemUpdateScreenCheckingPreview() {
    AxionTheme {
        SystemUpdateScreen(
            headline = "Checking for update…",
            supportingText = null,
            isBusy = true,
            canCheckForUpdates = false,
            showDeviceInfo = false,
            lastCheckedTimestamp = 0L,
            onBackClick = {},
            onCheckClick = {},
            onLocalUpdateClick = {},
            onPreferencesClick = {},
        )
    }
}
