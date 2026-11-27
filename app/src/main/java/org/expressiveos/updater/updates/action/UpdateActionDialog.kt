/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.expressiveos.updater.updates.action

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import com.android.axion.compose.expressive.ExpressiveAlertDialog
import com.android.axion.compose.expressive.rememberExpressiveHaptics
import org.expressiveos.updater.R

data class AlertDialogState(
    val onConfirm: () -> Unit = {},
    val showDismiss: Boolean = false,
    val title: String,
    val text: AnnotatedString,
    // Button labels: null keeps OK and Cancel
    val confirmText: String? = null,
    val dismissText: String? = null,
    // Run by the dismiss button only, not by tapping outside the dialog
    val onDismissButton: () -> Unit = {},
    val destructive: Boolean = false,
)

@Composable
fun UpdateActionDialog(
    dialog: AlertDialogState,
    onDismiss: () -> Unit,
) {
    val haptics = rememberExpressiveHaptics()

    ExpressiveAlertDialog(
        onDismissRequest = onDismiss,
        title = dialog.title,
        confirmText = dialog.confirmText ?: stringResource(android.R.string.ok),
        onConfirm = {
            onDismiss()
            dialog.onConfirm()
        },
        dismissText = if (dialog.showDismiss) {
            dialog.dismissText ?: stringResource(android.R.string.cancel)
        } else {
            null
        },
        onDismiss = if (dialog.showDismiss) {
            {
                haptics.performStepTick()
                onDismiss()
                dialog.onDismissButton()
            }
        } else {
            null
        },
        icon = ImageVector.vectorResource(R.drawable.ic_notification),
        destructive = dialog.destructive,
        frosted = true,
    ) {
        Text(
            text = dialog.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
