/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.expressiveos.updater

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.axion.compose.expressive.ExpressiveAlertDialog
import com.android.axion.compose.theme.AxionTheme
import org.expressiveos.updater.controller.UpdaterController
import org.expressiveos.updater.data.ChangelogState
import org.expressiveos.updater.data.Update
import org.expressiveos.updater.data.UpdateStatus
import org.expressiveos.updater.preferences.PreferencesActivity
import org.expressiveos.updater.ui.SystemUpdateScreen
import org.expressiveos.updater.ui.UpdateErrorBanner
import org.expressiveos.updater.ui.common.ProgressDialog
import org.expressiveos.updater.updates.action.AlertDialogState
import org.expressiveos.updater.updates.action.UpdateAction
import org.expressiveos.updater.updates.action.UpdateActionDialog
import org.expressiveos.updater.updates.action.UpdateActionHandler
import org.expressiveos.updater.updates.action.UpdateActionType
import org.expressiveos.updater.updates.state.UpdateItemStateMapper
import org.expressiveos.updater.updatescheck.UpdatesCheckState
import org.expressiveos.updater.updatescheck.rememberUpdatesCheckUiState

private data class ImportResultDialogState(
    val title: String,
    val message: String,
    val isError: Boolean,
)

private data class ExplicitErrorBannerState(
    val eventId: Long,
    val downloadId: String,
    val title: String,
    val message: String,
    val retryWithResume: Boolean,
)

private data class SnackbarMessageEvent(
    val id: Long,
    val message: String,
)

abstract class UpdatesScaffoldActivity : ComponentActivity() {
    private val viewModel by viewModels<UpdatesViewModel>()
    private var activeUpdaterController: UpdaterController? by mutableStateOf(null)
    private var controllerStateVersion: Int by mutableIntStateOf(0)

    protected var importDialogVisible: Boolean by mutableStateOf(false)
    private var importResultDialogState: ImportResultDialogState? by mutableStateOf(null)
    private var explicitErrorBanner: ExplicitErrorBannerState? by mutableStateOf(null)
    private var snackbarMessageEvent: SnackbarMessageEvent? by mutableStateOf(null)
    private var nextEventId: Long = 1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun setupCompose() {
        setContent {
            AxionTheme {
                val uiState by viewModel.uiState.collectAsState()
                UpdatesScaffoldContent(
                    uiState = uiState,
                    updaterController = activeUpdaterController,
                    controllerStateVersion = controllerStateVersion,
                    explicitErrorBanner = explicitErrorBanner,
                    snackbarEvent = snackbarMessageEvent,
                    onClearExplicitErrorBanner = { explicitErrorBanner = null },
                    onBackClick = { finish() },
                    onRefreshClick = { onRefreshClick() },
                    onLocalUpdateClick = { onLocalUpdateClick() },
                    onPreferencesClick = {
                        startActivity(
                            Intent(
                                this@UpdatesScaffoldActivity,
                                PreferencesActivity::class.java,
                            )
                        )
                    },
                    onControllerStateChanged = { notifyControllerStateChanged() },
                )

                if (importDialogVisible) {
                    ProgressDialog(
                        title = stringResource(R.string.local_update_import),
                        text = stringResource(R.string.local_update_import_progress),
                        onCancel = { onCancelLocalImport() },
                    )
                }

                importResultDialogState?.let { dialog ->
                    ExpressiveAlertDialog(
                        onDismissRequest = { importResultDialogState = null },
                        title = dialog.title,
                        text = dialog.message,
                        confirmText = stringResource(android.R.string.ok),
                        onConfirm = { importResultDialogState = null },
                        icon = if (dialog.isError) {
                            Icons.Outlined.ErrorOutline
                        } else {
                            Icons.Outlined.CheckCircle
                        },
                        destructive = dialog.isError,
                        frosted = true,
                    )
                }
            }
        }
    }

    protected fun setUpdaterController(controller: UpdaterController?) {
        activeUpdaterController = controller
        notifyControllerStateChanged()
    }

    protected fun notifyControllerStateChanged() {
        controllerStateVersion++
    }

    protected fun showImportResultDialog(title: String, message: String, isError: Boolean) {
        importResultDialogState = ImportResultDialogState(
            title = title,
            message = message,
            isError = isError,
        )
    }

    protected fun showUpdateErrorBanner(
        downloadId: String,
        title: String,
        message: String,
        retryWithResume: Boolean,
    ) {
        explicitErrorBanner = ExplicitErrorBannerState(
            eventId = nextEventId++,
            downloadId = downloadId,
            title = title,
            message = message,
            retryWithResume = retryWithResume,
        )
    }

    protected fun clearUpdateErrorBanner(downloadId: String) {
        if (explicitErrorBanner?.downloadId == downloadId) {
            explicitErrorBanner = null
        }
    }

    protected fun showSnackbarMessage(message: String) {
        snackbarMessageEvent = SnackbarMessageEvent(
            id = nextEventId++,
            message = message,
        )
    }

    open fun onRefreshClick() {}
    open fun onLocalUpdateClick() {}
    open fun onCancelLocalImport() {}
    open fun exportUpdate(update: Update) {}
}

@Composable
private fun UpdatesScaffoldContent(
    uiState: UpdatesViewModel.UiState,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    explicitErrorBanner: ExplicitErrorBannerState?,
    snackbarEvent: SnackbarMessageEvent?,
    onClearExplicitErrorBanner: () -> Unit,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onControllerStateChanged: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as UpdatesScaffoldActivity
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarEvent?.id) {
        val event = snackbarEvent ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = event.message,
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
    }

    var dismissedErrorKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(explicitErrorBanner?.eventId) {
        if (explicitErrorBanner != null) {
            dismissedErrorKey = null
        }
    }

    val networkMonitor =
        remember { (context.applicationContext as UpdaterApplication).networkMonitor }
    val networkState by networkMonitor.networkState.collectAsState(
        initial = networkMonitor.currentNetworkState,
    )
    val userPreferencesRepository =
        remember { (context.applicationContext as UpdaterApplication).userPreferencesRepository }
    val streamUpdatesEnabled by userPreferencesRepository.streamUpdatesFlow.collectAsState(
        initial = true,
    )
    val showLogo by userPreferencesRepository.showLogoFlow.collectAsState(
        initial = true,
    )
    val showUpdaterIcon by userPreferencesRepository.showUpdaterIconFlow.collectAsState(
        initial = false,
    )

    // Room only keeps persistent statuses; use the controller's live ones.
    val liveUpdates = remember(uiState.updates, updaterController, controllerStateVersion) {
        val controller = updaterController ?: return@remember uiState.updates
        uiState.updates.mapNotNull { controller.getUpdate(it.downloadId) }
    }

    val updateItems = remember(
        uiState.updates,
        updaterController,
        networkState,
        streamUpdatesEnabled,
        controllerStateVersion,
    ) {
        val controller = updaterController ?: return@remember emptyList()
        val mapper = UpdateItemStateMapper(context, controller, streamUpdatesEnabled)
        uiState.updates.mapNotNull { update ->
            controller.getUpdate(update.downloadId)?.let {
                mapper.map(it, networkState)
            }
        }
    }

    val actionDialogState = remember { mutableStateOf<AlertDialogState?>(null) }
    actionDialogState.value?.let { dialog ->
        UpdateActionDialog(
            dialog = dialog,
            onDismiss = { actionDialogState.value = null },
        )
    }

    val actionHandler = remember(updaterController) {
        updaterController?.let { controller ->
            UpdateActionHandler(
                activity = activity,
                updaterController = controller,
                exportUpdate = { update -> activity.exportUpdate(update) },
                showDialog = { actionDialogState.value = it },
            )
        }
    }

    val model = uiState.updatesCheckModel
    val checkUiState = rememberUpdatesCheckUiState(model.state)
    val isChecking = checkUiState.displayedState is UpdatesCheckState.Checking
    val isPreparing = liveUpdates.any { it.status == UpdateStatus.STARTING }
    val isBusy = isChecking || isPreparing
    val isIdleAndEmpty = updateItems.isEmpty() && !isBusy

    val activeItem = updateItems.firstOrNull { it.progress != null } ?: updateItems.firstOrNull()

    val persistedErrorUpdate = liveUpdates.firstOrNull {
        it.status == UpdateStatus.PAUSED_ERROR || it.status == UpdateStatus.VERIFICATION_FAILED
    }

    val activeErrorBanner: Pair<UpdateErrorBanner, Boolean>? = when {
        explicitErrorBanner != null -> {
            val key = "explicit:${explicitErrorBanner.eventId}"
            if (dismissedErrorKey == key) {
                null
            } else {
                UpdateErrorBanner(
                    downloadId = explicitErrorBanner.downloadId,
                    title = explicitErrorBanner.title,
                    message = explicitErrorBanner.message,
                    canRetry = networkState.isOnline,
                ) to explicitErrorBanner.retryWithResume
            }
        }

        persistedErrorUpdate != null -> {
            val key = "status:${persistedErrorUpdate.downloadId}:${persistedErrorUpdate.status.name}"
            if (dismissedErrorKey == key) {
                null
            } else if (persistedErrorUpdate.status == UpdateStatus.PAUSED_ERROR) {
                UpdateErrorBanner(
                    downloadId = persistedErrorUpdate.downloadId,
                    title = stringResource(R.string.download_paused_error_notification),
                    message = stringResource(R.string.snack_download_failed),
                    canRetry = networkState.isOnline,
                ) to true
            } else {
                UpdateErrorBanner(
                    downloadId = persistedErrorUpdate.downloadId,
                    title = stringResource(R.string.verification_failed_notification),
                    message = stringResource(R.string.snack_download_verification_failed),
                    canRetry = networkState.isOnline,
                ) to false
            }
        }

        else -> null
    }

    SystemUpdateScreen(
        headline = getHeadline(
            updates = liveUpdates,
            displayedCheckState = checkUiState.displayedState,
            isPreparing = isPreparing,
            hasUpdateItems = updateItems.isNotEmpty(),
        ),
        supportingText = when (checkUiState.displayedState) {
            UpdatesCheckState.NoInternet ->
                stringResource(R.string.check_your_internet_connection)

            UpdatesCheckState.Error -> stringResource(R.string.updates_check_failed)
            else -> null
        },
        supportingTextIsError = checkUiState.displayedState is UpdatesCheckState.NoInternet ||
                checkUiState.displayedState is UpdatesCheckState.Error,
        isBusy = isBusy,
        canCheckForUpdates = model.canCheckForUpdates,
        showDeviceInfo = isIdleAndEmpty,
        showLogo = showLogo,
        showUpdaterIcon = showUpdaterIcon,
        lastCheckedTimestamp = if (isIdleAndEmpty) model.lastCheckedTimestamp else 0L,
        onBackClick = onBackClick,
        onCheckClick = onRefreshClick,
        onLocalUpdateClick = onLocalUpdateClick,
        onPreferencesClick = onPreferencesClick,
        updateItem = if (isBusy) null else activeItem,
        supportInfo = uiState.supportInfo,
        changelogState = if (activeItem?.downloadId == uiState.changelogUpdateId) {
            uiState.changelogState
        } else {
            ChangelogState.Idle
        },
        errorBanner = activeErrorBanner?.first,
        onRetryErrorBanner = {
            val (banner, retryWithResume) = activeErrorBanner ?: return@SystemUpdateScreen
            dismissedErrorKey = null
            onClearExplicitErrorBanner()
            val controller = updaterController
            val update = controller?.getUpdate(banner.downloadId)
            if (update != null && actionHandler != null) {
                val retryAction = if (retryWithResume) {
                    UpdateActionType.RESUME_DOWNLOAD
                } else {
                    UpdateActionType.START_DOWNLOAD
                }
                actionHandler.perform(UpdateAction(retryAction), update)
                onControllerStateChanged()
            } else {
                onRefreshClick()
            }
        },
        onDismissErrorBanner = {
            dismissedErrorKey = when {
                explicitErrorBanner != null -> "explicit:${explicitErrorBanner.eventId}"
                persistedErrorUpdate != null ->
                    "status:${persistedErrorUpdate.downloadId}:${persistedErrorUpdate.status.name}"
                else -> null
            }
            onClearExplicitErrorBanner()
        },
        snackbarHostState = snackbarHostState,
        onUpdateAction = { action ->
            val item = activeItem
            val controller = updaterController
            if (item != null && controller != null) {
                controller.getUpdate(item.downloadId)?.let { update ->
                    actionHandler?.perform(action, update)
                    onControllerStateChanged()
                }
            }
        },
    )
}

@Composable
private fun getHeadline(
    updates: List<Update>,
    displayedCheckState: UpdatesCheckState,
    isPreparing: Boolean,
    hasUpdateItems: Boolean,
): String = when {
    updates.any { it.status == UpdateStatus.UPDATED_NEED_REBOOT } ->
        stringResource(R.string.installing_update_finished)

    updates.any { it.status == UpdateStatus.INSTALLATION_FAILED } ->
        stringResource(R.string.installing_update_error)

    updates.any { it.status == UpdateStatus.INSTALLING } ->
        stringResource(R.string.installing_update_title)

    isPreparing -> stringResource(R.string.preparing_update_title)

    displayedCheckState is UpdatesCheckState.Checking ->
        stringResource(R.string.checking_for_update_title)

    displayedCheckState is UpdatesCheckState.NoInternet ||
            displayedCheckState is UpdatesCheckState.Error ->
        stringResource(R.string.updates_check_failed_title)

    updates.any { it.status == UpdateStatus.DOWNLOADING } ->
        stringResource(R.string.downloading_update_title)

    updates.any { it.status == UpdateStatus.VERIFYING } ->
        stringResource(R.string.verifying_update_title)

    updates.any {
        it.status == UpdateStatus.PAUSED ||
                it.status == UpdateStatus.PAUSED_ERROR ||
                it.status == UpdateStatus.INSTALLATION_SUSPENDED
    } -> stringResource(R.string.update_paused_title)

    hasUpdateItems -> stringResource(R.string.update_available_title)

    else -> stringResource(R.string.system_up_to_date_title)
}
