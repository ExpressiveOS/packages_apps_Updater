/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.expressiveos.updater.util

import android.ota.nano.OtaPackageMetadata.OtaMetadata
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

class OtaMetadataParser @Throws(IOException::class) constructor(file: File) {
    val sdkLevel: Int
    val securityPatchLevel: String
    val timestamp: Long
    val isABUpdate: Boolean
    /** The devices the package is built for (precondition.device); empty if it names none. */
    val devices: List<String>

    init {
        val metadata = ZipFile(file).use { readMetadata(it) }
        isABUpdate = when (metadata.type) {
            OtaMetadata.AB -> true
            OtaMetadata.BLOCK -> false
            // OtaMetadata.BRICK -> false
            else -> throw IOException("Unsupported OTA type: ${metadata.type}")
        }

        val postcondition = metadata.postcondition
        sdkLevel = postcondition.sdkLevel.toInt()
        securityPatchLevel = postcondition.securityPatchLevel
        timestamp = postcondition.timestamp
        devices = metadata.precondition?.device?.toList().orEmpty()
    }

    companion object {
        private const val METADATA_PROTO_NAME = "META-INF/com/android/metadata.pb"

        @Throws(IOException::class)
        private fun readMetadata(zipFile: ZipFile): OtaMetadata {
            val entry = zipFile.getEntry(METADATA_PROTO_NAME)
                ?: throw IOException("Couldn't find $METADATA_PROTO_NAME in ${zipFile.name}")

            return zipFile.getInputStream(entry).use { input ->
                OtaMetadata.parseFrom(input.readBytes())
            }
        }
    }
}
