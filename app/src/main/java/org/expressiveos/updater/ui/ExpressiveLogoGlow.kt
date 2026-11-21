/*
 * SPDX-FileCopyrightText: Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.expressiveos.updater.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.graphics.PathParser
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.expressiveos.updater.R

private const val VIEWPORT_W = 1080f
private const val VIEWPORT_H = 240f

// The Bloom mark's petals in the boot animation wordmark's order, the first
// repeated so the sweep loops: mid tones for dark surfaces, deep tones for light ones.
private val BLOOM_ON_DARK = intArrayOf(
    0xFFAE69FC.toInt(), 0xFF417BFC.toInt(), 0xFF4AD29D.toInt(), 0xFFFD7771.toInt(), 0xFFAE69FC.toInt(),
)
private val BLOOM_ON_LIGHT = intArrayOf(
    0xFF6B2BBB.toInt(), 0xFF1842D1.toInt(), 0xFF18A07C.toInt(), 0xFFD33952.toInt(), 0xFF6B2BBB.toInt(),
)
private val BLOOM_STOPS = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)

// Fringe: the blue petal left, the violet petal right (light tones on dark
// surfaces, deep tones on light ones)
private val FRINGE_LEFT_ON_DARK = 0xFF76BCFB.toInt()
private val FRINGE_RIGHT_ON_DARK = 0xFFF3B1FC.toInt()
private val FRINGE_LEFT_ON_LIGHT = 0xFF1842D1.toInt()
private val FRINGE_RIGHT_ON_LIGHT = 0xFF6B2BBB.toInt()

/**
 * The wordmark's colours on dark and on light surfaces: the sweep's five stops (the first colour
 * repeated last, for [BLOOM_STOPS]) and the fringe's left and right colours.
 */
private class GlowPalette(
    val onDark: IntArray,
    val onLight: IntArray,
    val fringeLeftOnDark: Int,
    val fringeRightOnDark: Int,
    val fringeLeftOnLight: Int,
    val fringeRightOnLight: Int,
)

private val BLOOM = GlowPalette(
    onDark = BLOOM_ON_DARK,
    onLight = BLOOM_ON_LIGHT,
    fringeLeftOnDark = FRINGE_LEFT_ON_DARK,
    fringeRightOnDark = FRINGE_RIGHT_ON_DARK,
    fringeLeftOnLight = FRINGE_LEFT_ON_LIGHT,
    fringeRightOnLight = FRINGE_RIGHT_ON_LIGHT,
)

/**
 * Settings.Secure key with the banner colours of the holiday theme that is on, written by
 * Settings' holiday themes (Settings' GlowView reads it too):
 * "<id>;<sweep on dark>;<sweep on light>;<fringe on dark>;<fringe on light>", a sweep being 4
 * RRGGBB colours and a fringe 2 (left, right), split by ','. Unset, empty or anything else means
 * the Bloom colours.
 */
private const val KEY_HOLIDAY_PALETTE = "expressiveos_holiday_palette"

// Gradient vector (viewport units). One sweep moves it by its own length,
// so the repeating gradient loops seamlessly.
private const val GRAD_X1 = 190f
private const val GRAD_Y1 = 60f
private const val GRAD_X2 = 890f
private const val GRAD_Y2 = 180f
private const val SWEEP_MS = 5000L

private const val MAIN_STROKE = 2.4f
private const val FILL_ALPHA = 38 // 15%
private const val GLOW_STROKE = 9f
private const val GLOW_SIGMA = 7f
private const val GLOW_ALPHA = 140 // 55%
private const val FRINGE_STROKE = 3f
private const val FRINGE_SIGMA = 2.5f
private const val FRINGE_SHIFT = 2f
private const val FRINGE_ALPHA = 128 // 50%

// Shine: a skewed band crossing the logo, then a pause.
private const val SHINE_MS = 5000L
private const val SHINE_ACTIVE = 0.3f // fraction of the period spent moving
private const val SHINE_FROM = 120f
private const val SHINE_TO = 900f
private const val SHINE_WIDTH = 90f
private const val SHINE_SKEW = -0.36397f // tan(-20 deg)
private const val SHINE_REACH = 5000f
private const val SHINE_FILL_ALPHA = 64 // 25%

// Sparkles: x, y, size (viewport units), then twinkle start/peak/end as a
// fraction of SPARKLE_MS.
private const val SPARKLE_MS = 3200L
private val SPARKLES = arrayOf(
    floatArrayOf(880f, 66f, 9f, 0.00f, 0.20f, 0.40f),
    floatArrayOf(792f, 176f, 7f, 0.35f, 0.50f, 0.65f),
    floatArrayOf(548f, 80f, 6f, 0.60f, 0.75f, 0.90f),
)

/**
 * The ExpressiveOS wordmark in the Bloom mark's colours: its four petals
 * (violet, blue, green, coral) as a gradient sweeping across the outline, a
 * soft glow with a blue and violet fringe, a periodic shine streak and a few
 * twinkling sparkles. Plays one seamless [SWEEP_MS] sweep and holds the still
 * frame (replaying on tap) so the screen can idle at LTPO minimum refresh rate.
 * Mirrors GlowView in Settings.
 */
@Composable
fun ExpressiveLogoGlow(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val renderer = remember {
        GlowRenderer(context.resources.getStringArray(R.array.expressiveos_logo_paths))
    }
    val ink = MaterialTheme.colorScheme.onSurface.toArgb() or 0xFF000000.toInt()
    // A holiday theme's colours while its banner is on, else Bloom's
    val palette = remember { readHolidayPalette(context) ?: BLOOM }

    // Respect "remove animations": draw one still frame instead of sweeping.
    val animate = remember { ValueAnimator.areAnimatorsEnabled() }
    var replayCount by remember { mutableIntStateOf(0) }
    var time by remember { mutableLongStateOf(SWEEP_MS) }
    if (animate) {
        LaunchedEffect(replayCount) {
            time = 0L
            val start = withFrameMillis { it }
            while (time < SWEEP_MS) {
                withFrameMillis { now ->
                    time = (now - start).coerceAtMost(SWEEP_MS)
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .aspectRatio(VIEWPORT_W / VIEWPORT_H)
            .pointerInput(animate) {
                if (animate) {
                    detectTapGestures { replayCount++ }
                }
            }
            .semantics { contentDescription?.let { this.contentDescription = it } },
    ) {
        val t = time
        drawIntoCanvas { canvas ->
            renderer.draw(canvas.nativeCanvas, size.width, size.height, t, ink, palette)
        }
    }
}

private class GlowRenderer(pathData: Array<String>) {
    private val logoUnits = Path().apply {
        pathData.forEach { addPath(PathParser.createPathFromPathData(it)) }
    }
    private val logo = Path()
    private val star = Path().apply {
        moveTo(0f, -1f)
        quadTo(0f, 0f, 1f, 0f)
        quadTo(0f, 0f, 0f, 1f)
        quadTo(0f, 0f, -1f, 0f)
        quadTo(0f, 0f, 0f, -1f)
        close()
    }
    private val matrix = Matrix()
    private val shaderMatrix = Matrix()

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = GLOW_ALPHA
    }
    private val leftFringePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rightFringePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = FILL_ALPHA
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val shineFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val shineStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val band = LinearGradient(
        0f, 0f, SHINE_WIDTH, 0f,
        intArrayOf(0x00FFFFFF, 0xFFFFFFFF.toInt(), 0x00FFFFFF), null,
        Shader.TileMode.CLAMP,
    )
    private var holo: LinearGradient? = null
    private var holoColors = BLOOM_ON_DARK
    private var glowMask: Bitmap? = null
    private var fringeMask: Bitmap? = null
    private var maskW = 0
    private var maskH = 0

    private var width = 0f
    private var height = 0f
    private var scale = 0f
    private var ox = 0f
    private var oy = 0f
    private var ink = 0
    private var palette = BLOOM

    private fun buildAlphaMask(bw: Int, bh: Int, strokeWidth: Float, radius: Float): Bitmap {
        val mask = Bitmap.createBitmap(bw, bh, Bitmap.Config.ALPHA_8)
        maskPaint.strokeWidth = strokeWidth
        maskPaint.maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        android.graphics.Canvas(mask).drawPath(logo, maskPaint)
        maskPaint.maskFilter = null
        mask.prepareToDraw()
        return mask
    }

    private fun rebuildHolo() {
        if (scale <= 0f) return
        holo = LinearGradient(
            ox + GRAD_X1 * scale, oy + GRAD_Y1 * scale,
            ox + GRAD_X2 * scale, oy + GRAD_Y2 * scale,
            holoColors,
            BLOOM_STOPS,
            Shader.TileMode.REPEAT,
        )
    }

    private fun layout(w: Float, h: Float) {
        width = w
        height = h
        scale = minOf(w / VIEWPORT_W, h / VIEWPORT_H)
        ox = (w - VIEWPORT_W * scale) / 2f
        oy = (h - VIEWPORT_H * scale) / 2f

        matrix.setScale(scale, scale)
        matrix.postTranslate(ox, oy)
        logoUnits.transform(matrix, logo)

        strokePaint.strokeWidth = MAIN_STROKE * scale
        shineStroke.strokeWidth = MAIN_STROKE * scale

        val bw = w.toInt()
        val bh = h.toInt()
        if (bw <= 0 || bh <= 0 || scale <= 0f) {
            glowMask = null
            fringeMask = null
            maskW = 0
            maskH = 0
            holo = null
            return
        }
        if (bw != maskW || bh != maskH || glowMask == null || fringeMask == null) {
            maskW = bw
            maskH = bh
            glowMask = buildAlphaMask(bw, bh, GLOW_STROKE * scale, blurRadius(GLOW_SIGMA * scale))
            fringeMask = buildAlphaMask(bw, bh, FRINGE_STROKE * scale, blurRadius(FRINGE_SIGMA * scale))
        }

        rebuildHolo()
    }

    private fun applyInk(color: Int, colors: GlowPalette) {
        ink = color
        palette = colors
        shineFill.color = color
        shineFill.alpha = SHINE_FILL_ALPHA
        shineStroke.color = color
        sparklePaint.color = color

        // Light ink means a dark surface
        val onDark = Color.luminance(color) > 0.5f
        holoColors = if (onDark) colors.onDark else colors.onLight
        leftFringePaint.color = if (onDark) colors.fringeLeftOnDark else colors.fringeLeftOnLight
        leftFringePaint.alpha = FRINGE_ALPHA
        rightFringePaint.color = if (onDark) colors.fringeRightOnDark else colors.fringeRightOnLight
        rightFringePaint.alpha = FRINGE_ALPHA
        holo = null
    }

    fun draw(
        canvas: android.graphics.Canvas,
        w: Float,
        h: Float,
        t: Long,
        inkColor: Int,
        colors: GlowPalette,
    ) {
        if (inkColor != ink || colors !== palette) applyInk(inkColor, colors)
        if (w != width || h != height) {
            layout(w, h)
        } else if (holo == null) {
            rebuildHolo()
        }
        val holo = holo ?: return
        if (scale <= 0f) return

        // Sweep the gradient along its own vector, which loops seamlessly.
        val sweep = (t % SWEEP_MS) / SWEEP_MS.toFloat()
        shaderMatrix.setTranslate(
            sweep * (GRAD_X2 - GRAD_X1) * scale,
            sweep * (GRAD_Y2 - GRAD_Y1) * scale,
        )
        holo.setLocalMatrix(shaderMatrix)
        glowPaint.shader = holo
        fillPaint.shader = holo
        strokePaint.shader = holo

        // Cached alpha masks for soft glow and left/right fringes
        glowMask?.let { canvas.drawBitmap(it, 0f, 0f, glowPaint) }
        fringeMask?.let { mask ->
            canvas.drawBitmap(mask, -FRINGE_SHIFT * scale, 0f, leftFringePaint)
            canvas.drawBitmap(mask, FRINGE_SHIFT * scale, 0f, rightFringePaint)
        }

        // Glassy fill and gradient outline
        canvas.drawPath(logo, fillPaint)
        canvas.drawPath(logo, strokePaint)

        drawShine(canvas, t)
        drawSparkles(canvas, t)
    }

    private fun drawShine(canvas: android.graphics.Canvas, t: Long) {
        val p = (t % SHINE_MS) / SHINE_MS.toFloat()
        if (p >= SHINE_ACTIVE || !endsInTime(t, SHINE_MS, SHINE_ACTIVE)) return

        val bx = SHINE_FROM + (SHINE_TO - SHINE_FROM) * (p / SHINE_ACTIVE)

        val layer = canvas.saveLayer(0f, 0f, width, height, null)
        canvas.drawPath(logo, shineFill)
        canvas.drawPath(logo, shineStroke)

        // Keep only the part of the logo that sits under the band
        canvas.save()
        canvas.translate(ox, oy)
        canvas.scale(scale, scale)
        canvas.skew(SHINE_SKEW, 0f)
        shaderMatrix.setTranslate(bx, 0f)
        band.setLocalMatrix(shaderMatrix)
        bandPaint.shader = band
        canvas.drawRect(-SHINE_REACH, -SHINE_REACH, SHINE_REACH, SHINE_REACH, bandPaint)
        canvas.restore()
        canvas.restoreToCount(layer)
    }

    private fun drawSparkles(canvas: android.graphics.Canvas, t: Long) {
        val p = (t % SPARKLE_MS) / SPARKLE_MS.toFloat()
        for (s in SPARKLES) {
            val k = pulse(p, s[3], s[4], s[5]) * s[2] * scale
            if (k <= 0f || !endsInTime(t, SPARKLE_MS, s[5])) continue
            canvas.save()
            canvas.translate(ox + s[0] * scale, oy + s[1] * scale)
            canvas.scale(k, k)
            canvas.drawPath(star, sparklePaint)
            canvas.restore()
        }
    }

    // Whether what ends at end (a fraction of period) in t's period is over by SWEEP_MS,
    // so the animation never stops on a half-lit shine or sparkle
    private fun endsInTime(t: Long, period: Long, end: Float): Boolean =
        t - t % period + (end * period).toLong() <= SWEEP_MS

    // 0 at start and end, 1 at peak, linear in between
    private fun pulse(p: Float, start: Float, peak: Float, end: Float): Float {
        if (p <= start || p >= end) return 0f
        return if (p < peak) (p - start) / (peak - start) else (end - p) / (end - peak)
    }

    // BlurMaskFilter takes a radius, Skia converts it with sigma = 0.57735 * r + 0.5
    private fun blurRadius(sigma: Float): Float = maxOf(1f, (sigma - 0.5f) / 0.57735f)
}

/**
 * The [KEY_HOLIDAY_PALETTE] colours, or null (Bloom) when the key is unset, can't be read or
 * isn't exactly the five groups it should be.
 */
private fun readHolidayPalette(context: Context): GlowPalette? {
    val value = runCatching {
        Settings.Secure.getString(context.contentResolver, KEY_HOLIDAY_PALETTE)
    }.getOrNull() ?: return null

    val groups = value.trim().split(';')
    if (groups.size != 5 || groups[0].isEmpty() || ',' in groups[0]) return null
    val onDark = parseColors(groups[1], 4) ?: return null
    val onLight = parseColors(groups[2], 4) ?: return null
    val fringeOnDark = parseColors(groups[3], 2) ?: return null
    val fringeOnLight = parseColors(groups[4], 2) ?: return null
    return GlowPalette(
        // The first colour repeated, so the sweep loops
        onDark = onDark + onDark[0],
        onLight = onLight + onLight[0],
        fringeLeftOnDark = fringeOnDark[0],
        fringeRightOnDark = fringeOnDark[1],
        fringeLeftOnLight = fringeOnLight[0],
        fringeRightOnLight = fringeOnLight[1],
    )
}

/** Exactly [count] opaque colours from RRGGBB hex split by ',', else null. */
private fun parseColors(group: String, count: Int): IntArray? {
    val parts = group.split(',')
    if (parts.size != count) return null
    val colors = IntArray(count)
    for (i in 0 until count) {
        val part = parts[i]
        // ASCII hex only: toInt(16) alone would also take a sign and other scripts' digits
        if (part.length != 6 || !part.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }) {
            return null
        }
        colors[i] = part.toInt(16) or 0xFF000000.toInt()
    }
    return colors
}

@UiModePreviews
@Composable
private fun ExpressiveLogoGlowPreview() {
    SettingsTheme {
        ExpressiveLogoGlow()
    }
}
