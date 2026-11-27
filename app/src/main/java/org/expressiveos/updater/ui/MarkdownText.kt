/*
 * SPDX-FileCopyrightText: 2026 PixelOS
 * SPDX-FileCopyrightText: 2026 ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.expressiveos.updater.ui

import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

private const val GOOGLE_SANS_FLEX = "google-sans-flex"

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val markwon = remember(context) { Markwon.create(context) }
    val parsedMarkdown = remember(markwon, markdown) { markwon.toMarkdown(markdown) }
    val typeface = remember { Typeface.create(GOOGLE_SANS_FLEX, Typeface.NORMAL) }
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val textSize = MaterialTheme.typography.bodyMedium.fontSize.value

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextView(viewContext).apply {
                movementMethod = LinkMovementMethod.getInstance()
                this.typeface = typeface
                setLineSpacing(0f, 1.18f)
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            textView.textSize = textSize
            if (textView.tag != markdown) {
                markwon.setParsedMarkdown(textView, parsedMarkdown)
                textView.tag = markdown
            }
        },
    )
}
