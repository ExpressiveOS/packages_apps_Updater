/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.expressiveos.updater

import android.app.Application
import java.io.File
import com.android.settingslib.spa.framework.common.SettingsPageProviderRepository
import com.android.settingslib.spa.framework.common.SpaEnvironment
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import kotlinx.coroutines.MainScope
import org.expressiveos.updater.data.AppStateRepository
import org.expressiveos.updater.data.ChangelogRepository
import org.expressiveos.updater.data.UpdatesRepository
import org.expressiveos.updater.data.UserPreferencesRepository
import org.expressiveos.updater.data.source.local.UpdatesDatabase
import org.expressiveos.updater.data.source.local.UpdatesLocalDataSource
import org.expressiveos.updater.data.source.network.UpdatesNetworkDataSource
import org.expressiveos.updater.notifications.NotificationHelper
import org.expressiveos.updater.util.BatteryMonitor
import org.expressiveos.updater.util.NetworkMonitor
import org.expressiveos.updater.util.SystemUpdateInfoPublisher

class UpdaterApplication : Application() {
    private val coroutineScope = MainScope()
    private val database by lazy { UpdatesDatabase.getInstance(applicationContext) }
    private val networkDataSource by lazy { UpdatesNetworkDataSource(applicationContext) }
    private val localDataSource by lazy { UpdatesLocalDataSource(database.updateDao()) }


    val batteryMonitor by lazy {
        BatteryMonitor(applicationContext, coroutineScope, userPreferencesRepository)
    }
    val networkMonitor by lazy { NetworkMonitor(applicationContext, coroutineScope) }
    val notificationHelper by lazy { NotificationHelper(applicationContext) }
    val appStateRepository by lazy { AppStateRepository(applicationContext) }
    val changelogRepository by lazy { ChangelogRepository(applicationContext) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(applicationContext) }
    val systemUpdateInfoPublisher by lazy {
        SystemUpdateInfoPublisher(applicationContext, localDataSource)
    }
    val updatesRepository by lazy {
        UpdatesRepository(
            networkMonitor = networkMonitor,
            notificationHelper = notificationHelper,
            networkDataSource = networkDataSource,
            localDataSource = localDataSource,
            systemUpdateInfoPublisher = systemUpdateInfoPublisher,
            appStateRepository = appStateRepository,
        )
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper.setUpNotificationChannels()
        SpaEnvironmentFactory.reset(object : SpaEnvironment(applicationContext) {
            override val pageProviderRepository = lazy {
                SettingsPageProviderRepository(emptyList())
            }

            override val isSpaExpressiveEnabled = true
        })
        sweepLegacyDownloads()
    }

    /**
     * Through 1.2 this app was org.evolution.updater and kept its packages in
     * /data/evolution_updates. Nothing reads that folder any more, so empty it; init removes the
     * empty folder on the next boot. A missing folder is the normal case and costs nothing.
     */
    private fun sweepLegacyDownloads() {
        Thread {
            runCatching {
                File(LEGACY_DOWNLOAD_PATH).listFiles()?.forEach { it.deleteRecursively() }
            }
        }.start()
    }

    private companion object {
        const val LEGACY_DOWNLOAD_PATH = "/data/evolution_updates"
    }
}
