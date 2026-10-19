package com.nirmaankarta.priority

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip as clipShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------- theme ----------

@Composable
fun PriorityTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        background = Color(0xFF0B0F19),
        surface = Color(0xFF151B2B),
        surfaceVariant = Color(0xFF1E2638),
        onBackground = Color(0xFFE5E7EB),
        onSurface = Color(0xFFE5E7EB),
        onSurfaceVariant = Color(0xFF9CA3AF),
        primary = Color(0xFF4A6CF7),
        outline = Color(0xFF334155),
        outlineVariant = Color(0xFF273244),
    ) else lightColorScheme(
        background = Color(0xFFF1F3FA),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE9EEF5),
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF0F172A),
        onSurfaceVariant = Color(0xFF64748B),
        primary = Color(0xFF4A6CF7),
        outline = Color(0xFFCBD5E1),
        outlineVariant = Color(0xFFE2E8F0),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun muted(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

// ---------- quadrant icons (drawn, no icon libraries) ----------

@Composable
fun QuadrantIcon(q: Quadrant, tint: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        val stroke = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        when (q) {
            Quadrant.Q1 -> { // lightning bolt
                val path = Path().apply {
                    moveTo(13 * s, 2 * s); lineTo(4 * s, 14 * s); lineTo(11 * s, 14 * s)
                    lineTo(10 * s, 22 * s); lineTo(20 * s, 9 * s); lineTo(13 * s, 9 * s); close()
                }
                drawPath(path, tint)
            }
            Quadrant.Q2 -> { // calendar / focus
                drawRoundRect(tint, p(4f, 5f), Size(16 * s, 16 * s), CornerRadius(3 * s, 3 * s), style = stroke)
                drawLine(tint, p(4f, 10f), p(20f, 10f), 2 * s, StrokeCap.Round)
                drawLine(tint, p(8f, 3f), p(8f, 7f), 2 * s, StrokeCap.Round)
                drawLine(tint, p(16f, 3f), p(16f, 7f), 2 * s, StrokeCap.Round)
                drawCircle(tint, 1.6f * s, p(12f, 15.5f))
            }
            Quadrant.Q3 -> { // users / delegate
                drawCircle(tint, 3.2f * s, p(9f, 8f), style = stroke)
                drawPath(Path().apply { moveTo(3 * s, 20 * s); cubicTo(3 * s, 15 * s, 15 * s, 15 * s, 15 * s, 20 * s) }, tint, style = stroke)
                drawCircle(tint, 2.5f * s, p(17f, 9f), style = stroke)
                drawPath(Path().apply { moveTo(17 * s, 14.5f * s); cubicTo(20 * s, 14.5f * s, 21.5f * s, 16.5f * s, 21.5f * s, 19.5f * s) }, tint, style = stroke)
            }
            Quadrant.Q4 -> { // ban / eliminate
                drawCircle(tint, 9f * s, p(12f, 12f), style = stroke)
                drawLine(tint, p(5.6f, 5.6f), p(18.4f, 18.4f), 2 * s, StrokeCap.Round)
            }
        }
    }
}

// ---------- avatars ----------

private val presetColors = listOf(
    Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF10B981),
    Color(0xFFF59E0B), Color(0xFF0EA5E9), Color(0xFF8B5CF6),
)
const val PRESET_AVATARS = 6

@Composable
fun PresetAvatar(index: Int, dp: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dp).clipShape(CircleShape)) {
        val w = this.size.width
        val skin = Color.White.copy(alpha = 0.92f)
        drawRect(presetColors[index % presetColors.size])
        drawCircle(skin, w * 0.38f, Offset(w * 0.5f, w * 1.0f))      // shoulders
        drawCircle(skin, w * 0.2f, Offset(w * 0.5f, w * 0.4f))       // head
        when (index % 3) {
            1 -> drawArc(                                             // hair
                Color.Black.copy(alpha = 0.35f), 180f, 180f, true,
                Offset(w * 0.3f, w * 0.2f), Size(w * 0.4f, w * 0.3f),
            )
            2 -> drawCircle(                                          // halo ring
                Color.White, w * 0.26f, Offset(w * 0.5f, w * 0.4f),
                style = Stroke(width = w * 0.04f),
            )
        }
    }
}

@Composable
fun Avatar(profile: Profile, dp: Dp, modifier: Modifier = Modifier) {
    val bmp = remember(profile.customAvatar) {
        profile.customAvatar?.let {
            runCatching {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
            }.getOrNull()
        }
    }
    if (bmp != null) {
        Image(bmp, null, modifier.size(dp).clipShape(CircleShape), contentScale = ContentScale.Crop)
    } else {
        PresetAvatar(profile.avatar, dp, modifier)
    }
}

// ---------- Roman-numeral badge (I / II / III / IV) ----------

@Composable
fun RomanBadge(q: Quadrant, dp: Dp = 22.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(dp).clipShape(CircleShape).background(q.accent), contentAlignment = Alignment.Center) {
        Text(q.label, color = Color.White, fontSize = (dp.value * 0.45f).sp, fontWeight = FontWeight.Bold)
    }
}

// ---------- bottom-navigation icons: 0 tasks, 1 recent, 2 calendar, 3 matrix, 4 more ----------

@Composable
fun NavIcon(kind: Int, tint: Color, cutout: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(28.dp)) {
            val s = this.size.width / 24f
            fun p(x: Float, y: Float) = Offset(x * s, y * s)
            val cut = Stroke(width = 2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (kind) {
                0 -> {
                    drawRoundRect(tint, p(3f, 3f), Size(18 * s, 18 * s), CornerRadius(5 * s, 5 * s))
                    drawPath(Path().apply {
                        moveTo(7.5f * s, 12.5f * s); lineTo(10.7f * s, 15.7f * s); lineTo(16.5f * s, 8.8f * s)
                    }, cutout, style = cut)
                }
                1 -> {
                    drawCircle(tint, 10f * s, p(12f, 12f))
                    drawPath(Path().apply {
                        moveTo(12f * s, 6.5f * s); lineTo(12f * s, 12f * s); lineTo(15.5f * s, 14f * s)
                    }, cutout, style = cut)
                }
                2 -> {
                    drawRoundRect(tint, p(3f, 4.5f), Size(18 * s, 17 * s), CornerRadius(4 * s, 4 * s))
                    drawRoundRect(tint, p(7f, 2f), Size(2.4f * s, 4.5f * s), CornerRadius(1.2f * s, 1.2f * s))
                    drawRoundRect(tint, p(14.6f, 2f), Size(2.4f * s, 4.5f * s), CornerRadius(1.2f * s, 1.2f * s))
                }
                3 -> for (r in 0..1) for (c in 0..1) {
                    drawRoundRect(tint, p(3f + c * 9.5f, 3f + r * 9.5f), Size(8.5f * s, 8.5f * s), CornerRadius(2.5f * s, 2.5f * s))
                }
                else -> for (i in 0..2) drawCircle(tint, 2.2f * s, p(4.5f + i * 7.5f, 12f))
            }
        }
        if (kind == 2) Text("20", color = cutout, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}
