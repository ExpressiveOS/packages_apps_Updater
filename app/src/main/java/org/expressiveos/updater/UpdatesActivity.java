/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */
package org.expressiveos.updater;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import org.expressiveos.updater.controller.UpdaterController;
import org.expressiveos.updater.controller.UpdaterService;
import org.expressiveos.updater.data.Update;
import org.expressiveos.updater.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UpdatesActivity extends UpdatesScaffoldActivity {

    private UpdaterService mUpdaterService;
    private BroadcastReceiver mBroadcastReceiver;

    private UpdatesViewModel mViewModel;

    private Update mToBeExported = null;
    private boolean mPendingImportLaunch = false;
    private final ActivityResultLauncher<Intent> mExportUpdate = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent != null) {
                        Uri uri = intent.getData();
                        exportUpdate(uri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> mImportUpdate = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent != null && intent.getData() != null) {
                        startLocalImport(intent.getData());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupCompose();

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UpdaterService.ACTION_LOCAL_IMPORT_STATE.equals(intent.getAction())) {
                    syncImportStateFromService();
                    notifyControllerStateChanged();
                    return;
                }
                notifyControllerStateChanged();
                if (UpdaterController.ACTION_UPDATE_STATUS.equals(intent.getAction())) {
                    String downloadId = intent.getStringExtra(UpdaterController.EXTRA_DOWNLOAD_ID);
                    handleDownloadStatusChange(downloadId);
                }
            }
        };

        mViewModel = new ViewModelProvider(this).get(UpdatesViewModel.class);

        mViewModel.getUiStateLive().observe(this, state -> {
            if (mUpdaterService != null) {
                syncControllerUpdates(state.getUpdates());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, UpdaterService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_STATUS);
        intentFilter.addAction(UpdaterController.ACTION_DOWNLOAD_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_INSTALL_PROGRESS);
        intentFilter.addAction(UpdaterController.ACTION_UPDATE_REMOVED);
        intentFilter.addAction(UpdaterService.ACTION_LOCAL_IMPORT_STATE);
        registerReceiver(mBroadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        unregisterReceiver(mBroadcastReceiver);
        if (mUpdaterService != null) {
            unbindService(mConnection);
        }
        super.onStop();
    }

    @Override
    public void onRefreshClick() {
        mViewModel.fetchUpdates();
    }

    @Override
    public void onLocalUpdateClick() {
        mImportUpdate.launch(UpdateImporter.createImportPickerIntent());
    }

    @Override
    public void onCancelLocalImport() {
        mPendingImportLaunch = false;
        if (mUpdaterService != null) {
            mUpdaterService.cancelLocalImport();
        } else {
            Intent intent = new Intent(this, UpdaterService.class);
            intent.setAction(UpdaterService.ACTION_IMPORT_CANCEL);
            startService(intent);
        }
    }

    private void startLocalImport(Uri uri) {
        mPendingImportLaunch = true;
        setImportDialogVisible(true);
        Intent intent = new Intent(this, UpdaterService.class);
        intent.setAction(UpdaterService.ACTION_IMPORT_LOCAL);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startService(intent);
    }

    private void syncImportStateFromService() {
        if (mUpdaterService == null) {
            return;
        }
        boolean isImporting = mUpdaterService.isImportingLocalUpdate();
        UpdaterService.LocalImportResult result = mUpdaterService.consumePendingImportResult();
        if (isImporting || result != null) {
            mPendingImportLaunch = false;
        }
        setImportDialogVisible(isImporting || mPendingImportLaunch);
        if (result == null) {
            return;
        }
        switch (result.status) {
            case SUCCESS:
                if (result.update != null) {
                    showImportResultDialog(
                            getString(R.string.local_update_import_success_title),
                            getString(
                                    R.string.local_update_import_success_message,
                                    StringUtil.formatBuildDate(this, result.update.getTimestamp())),
                            false);
                }
                break;
            case FAILURE:
                showImportResultDialog(
                        getString(R.string.local_update_import),
                        getString(R.string.local_update_import_failure),
                        true);
                break;
            case CANCELLED:
                showSnackbarMessage(getString(R.string.local_update_import_cancelled));
                break;
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            UpdaterService.LocalBinder binder = (UpdaterService.LocalBinder) service;
            mUpdaterService = binder.getService();
            setUpdaterController(mUpdaterService.getUpdaterController());
            syncControllerUpdates(
                    Objects.requireNonNull(mViewModel.getUiState().getValue()).getUpdates());
            syncImportStateFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            setUpdaterController(null);
            mUpdaterService = null;
        }
    };

    private void syncControllerUpdates(List<Update> updates) {
        UpdaterController controller = mUpdaterService.getUpdaterController();
        List<String> onlineUpdateIds = new ArrayList<>();
        for (Update update : updates) {
            boolean availableOnline = !Update.LOCAL_ID.equals(update.getDownloadId());
            controller.addUpdate(update, availableOnline);
            if (availableOnline) {
                onlineUpdateIds.add(update.getDownloadId());
            }
        }

        controller.setUpdatesAvailableOnline(onlineUpdateIds, true);
    }

    private void handleDownloadStatusChange(String downloadId) {
        if (Update.LOCAL_ID.equals(downloadId) || mUpdaterService == null) {
            return;
        }

        Update update = mUpdaterService.getUpdaterController().getUpdate(downloadId);
        if (update == null) {
            return;
        }
        switch (update.getStatus()) {
            case PAUSED_ERROR:
                showUpdateErrorBanner(
                        downloadId,
                        getString(R.string.download_paused_error_notification),
                        getString(R.string.snack_download_failed),
                        true);
                showSnackbarMessage(getString(R.string.snack_download_failed));
                break;
            case VERIFICATION_FAILED:
                showUpdateErrorBanner(
                        downloadId,
                        getString(R.string.verification_failed_notification),
                        getString(R.string.snack_download_verification_failed),
                        false);
                showSnackbarMessage(getString(R.string.snack_download_verification_failed));
                break;
            case VERIFIED:
                clearUpdateErrorBanner(downloadId);
                showSnackbarMessage(getString(R.string.snack_download_verified));
                break;
            case STARTING:
            case DOWNLOADING:
            case INSTALLING:
            case DELETED:
                clearUpdateErrorBanner(downloadId);
                break;
            default:
                break;
        }
    }

    @Override
    public void exportUpdate(Update update) {
        mToBeExported = update;

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, update.getName());

        mExportUpdate.launch(intent);
    }

    private void exportUpdate(Uri uri) {
        Intent intent = new Intent(this, ExportUpdateService.class);
        intent.setAction(ExportUpdateService.ACTION_START_EXPORTING);
        intent.putExtra(ExportUpdateService.EXTRA_SOURCE_FILE, mToBeExported.getFile());
        intent.putExtra(ExportUpdateService.EXTRA_DEST_URI, uri);
        startService(intent);
    }
}
