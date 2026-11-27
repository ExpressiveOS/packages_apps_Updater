/*
 * SPDX-FileCopyrightText: 2026 PixelOS
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.expressiveos.updater.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.axion.compose.expressive.ExpressiveBlurSurface
import com.android.axion.compose.expressive.ExpressiveWavyCircularProgress
import com.android.axion.compose.preferences.ExpressiveShapes
import com.android.axion.compose.theme.AxionTheme
import org.expressiveos.updater.R

@Composable
fun ProgressDialog(
    title: String,
    text: String,
) {
    BasicAlertDialog(onDismissRequest = {}) {
        ExpressiveBlurSurface(
            shape = ExpressiveShapes.extraLarge,
            tintColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            applyDialogWindowBlur = true,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExpressiveWavyCircularProgress(
                        size = 44.dp,
                        strokeWidth = 4.dp,
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressDialogPreview() {
    AxionTheme {
        ProgressDialog(
            title = stringResource(R.string.local_update_import),
            text = stringResource(R.string.local_update_import_progress),
        )
    }
}
