/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.expressiveos.updater.updates.action

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.android.axion.compose.expressive.ExpressiveMorphButton
import com.android.axion.compose.expressive.ExpressiveMorphContainer
import com.android.axion.compose.expressive.ExpressiveShapePreset
import com.android.axion.compose.expressive.rememberExpressiveHaptics
import com.android.axion.compose.preferences.ExpressiveShapes
import com.android.axion.compose.preferences.dialogWidth
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
)

@Composable
fun UpdateActionDialog(
    dialog: AlertDialogState,
    onDismiss: () -> Unit,
) {
    val haptics = rememberExpressiveHaptics()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveShapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.width(dialogWidth()),
        icon = {
            ExpressiveMorphContainer(
                active = true,
                inactivePreset = ExpressiveShapePreset.COOKIE_7,
                activePreset = ExpressiveShapePreset.COOKIE_7,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_notification),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Text(
                text = dialog.title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = dialog.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            ExpressiveMorphButton(
                onClick = {
                    onDismiss()
                    dialog.onConfirm()
                },
                selected = true,
                restingPreset = ExpressiveShapePreset.PILL,
                pressedPreset = ExpressiveShapePreset.SQUIRCLE,
                selectedPreset = ExpressiveShapePreset.PILL,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(text = dialog.confirmText ?: stringResource(android.R.string.ok))
            }
        },
        dismissButton = if (dialog.showDismiss) {
            {
                OutlinedButton(
                    onClick = {
                        haptics.performStepTick()
                        onDismiss()
                        dialog.onDismissButton()
                    },
                    shape = CircleShape,
                ) {
                    Text(
                        text = dialog.dismissText ?: stringResource(android.R.string.cancel),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                    )
                }
            }
        } else {
            null
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    )
}
