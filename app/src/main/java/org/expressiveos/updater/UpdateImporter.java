/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2020-2022 SHIFT GmbH
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */
package org.expressiveos.updater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.util.Log;

import org.expressiveos.updater.controller.UpdaterController;
import org.expressiveos.updater.data.Update;
import org.expressiveos.updater.data.UpdateStatus;
import org.expressiveos.updater.misc.Utils;
import org.expressiveos.updater.util.OtaMetadataParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateImporter {
    private static final String TAG = "UpdateImporter";
    private static final String MIME_ZIP = "application/zip";
    private static final String FILE_NAME = "localUpdate.zip";
    private static final int COPY_BUFFER_BYTES = 64 * 1024;

    private final Context context;
    private final Callbacks callbacks;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private volatile Thread workingThread;

    public UpdateImporter(Context context, Callbacks callbacks) {
        this.context = context.getApplicationContext();
        this.callbacks = callbacks;
    }

    public static Intent createImportPickerIntent() {
        return new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(MIME_ZIP);
    }

    public boolean isImporting() {
        Thread thread = workingThread;
        return thread != null && thread.isAlive();
    }

    public void stopImport() {
        cancelled.set(true);
        Thread thread = workingThread;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    public boolean startImport(Uri uri) {
        if (uri == null || isImporting()) {
            return false;
        }
        cancelled.set(false);
        callbacks.onImportStarted();

        Thread thread = new Thread(() -> {
            File importedFile = null;
            try {
                importedFile = importFile(uri);
                if (isCancelled()) {
                    deleteQuietly(importedFile);
                    callbacks.onImportCancelled();
                    return;
                }

                verifyPackage(importedFile);
                if (isCancelled()) {
                    deleteQuietly(importedFile);
                    callbacks.onImportCancelled();
                    return;
                }

                final Update update = buildLocalUpdate(importedFile);
                addUpdate(update);
                callbacks.onImportCompleted(update);
            } catch (InterruptedIOException e) {
                Log.i(TAG, "Local update import cancelled");
                deleteQuietly(importedFile);
                callbacks.onImportCancelled();
            } catch (Exception e) {
                deleteQuietly(importedFile);
                if (isCancelled()) {
                    Log.i(TAG, "Local update import cancelled");
                    callbacks.onImportCancelled();
                } else {
                    Log.e(TAG, "Failed to import update package", e);
                    callbacks.onImportCompleted(null);
                }
            } finally {
                workingThread = null;
            }
        }, "LocalUpdateImport");
        workingThread = thread;
        thread.start();
        return true;
    }

    private boolean isCancelled() {
        return cancelled.get() || Thread.currentThread().isInterrupted();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    @SuppressLint("SetWorldReadable")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File importFile(Uri uri) throws IOException {
        final File downloadDir = Utils.getDownloadPath(context);
        final File outFile = new File(downloadDir, FILE_NAME);
        if (outFile.exists()) {
            outFile.delete();
        }

        try (ParcelFileDescriptor parcelDescriptor =
                     context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (parcelDescriptor == null) {
                throw new IOException("Failed to obtain fileDescriptor");
            }
            try (FileInputStream iStream =
                         new FileInputStream(parcelDescriptor.getFileDescriptor());
                 FileOutputStream oStream = new FileOutputStream(outFile)) {
                int read;
                final byte[] buffer = new byte[COPY_BUFFER_BYTES];
                while ((read = iStream.read(buffer)) > 0) {
                    if (isCancelled()) {
                        throw new InterruptedIOException("Local update import cancelled");
                    }
                    oStream.write(buffer, 0, read);
                }
                oStream.flush();
            }
        } catch (IOException e) {
            deleteQuietly(outFile);
            throw e;
        }

        outFile.setReadable(true, false);
        return outFile;
    }

    private Update buildLocalUpdate(File file) throws IOException {
        final OtaMetadataParser metadata = new OtaMetadataParser(file);
        // Every ExpressiveOS phone shares one OTA key, so the signature check alone accepts
        // another phone's package. Recovery checks the device; update_engine does not.
        final String device = SystemProperties.get("ro.product.device");
        if (!metadata.getDevices().isEmpty() && !metadata.getDevices().contains(device)) {
            throw new IOException("Package is for " + metadata.getDevices() + ", not " + device);
        }
        final String name = context.getString(R.string.local_update_name);
        return new Update.Builder()
                .setName(name)
                .setFile(file)
                .setFileSize(file.length())
                .setTimestamp(metadata.getTimestamp())
                .setOsPatchLevel(metadata.getSecurityPatchLevel())
                .setOsSdkLevel(metadata.getSdkLevel())
                .setStatus(UpdateStatus.VERIFIED)
                .setVersion(name)
                .build();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void verifyPackage(File file) throws Exception {
        try {
            android.os.RecoverySystem.verifyPackage(file, null, null);
        } catch (Exception e) {
            if (file.exists()) {
                file.delete();
                throw new Exception("Verification failed, file has been deleted", e);
            } else {
                throw e;
            }
        }
    }

    private void addUpdate(Update update) {
        UpdaterController controller = UpdaterController.getInstance(context);
        controller.addLocalUpdate(update);
    }

    public interface Callbacks {
        void onImportStarted();

        void onImportCompleted(Update update);

        void onImportCancelled();
    }
}
