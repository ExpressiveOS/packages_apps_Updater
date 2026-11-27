/*
 * SPDX-FileCopyrightText: 2026 PixelOS
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.expressiveos.updater.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.android.axion.compose.theme.EmphasizedTabularStyle
import com.android.axion.compose.theme.GoogleSansFlexFontFamily
import com.android.axion.compose.theme.withTabularNums

internal val DisplaySmallEmphasizedFontFamily: FontFamily = GoogleSansFlexFontFamily

@Composable
internal fun updaterTabularLabelStyle(): TextStyle =
    EmphasizedTabularStyle(MaterialTheme.typography.labelLargeEmphasized)

@Composable
internal fun updaterTabularHeadlineStyle(): TextStyle =
    MaterialTheme.typography.headlineSmallEmphasized.withTabularNums()
