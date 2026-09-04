package com.blahmonster.prooflab.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.blahmonster.prooflab.core.StageKind
import com.blahmonster.prooflab.core.StageStatus

/**
 * Editorial, monospaced, hairline rules — the same visual language as blahmonster.com and the
 * iOS app, so the two platforms read as one product.
 */
data class ProofPalette(
	val paper: Color,
	val card: Color,
	val ink: Color,
	val inkSoft: Color,
	val inkMute: Color,
	val hairline: Color,
	val hot: Color,
	val cold: Color,
	val go: Color,
)

private val lightPalette = ProofPalette(
	paper = Color(0xFFF4F4EE),
	card = Color(0xFFFFFFFF),
	ink = Color(0xFF0A0A0A),
	inkSoft = Color(0xFF2A2A26),
	inkMute = Color(0xFF7A7A72),
	hairline = Color(0xFFD8D8CE),
	hot = Color(0xFFFF2D00),
	cold = Color(0xFF1D6FB8),
	go = Color(0xFF1F8C2E),
)

private val darkPalette = ProofPalette(
	paper = Color(0xFF121210),
	card = Color(0xFF1B1B18),
	ink = Color(0xFFF2F2EC),
	inkSoft = Color(0xFFCFCFC7),
	inkMute = Color(0xFF8B8B82),
	hairline = Color(0xFF2E2E29),
	hot = Color(0xFFFF5630),
	cold = Color(0xFF63A8E8),
	go = Color(0xFF4FBF60),
)

val LocalPalette = staticCompositionLocalOf { lightPalette }

private fun mono(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
	fontFamily = FontFamily.Monospace,
	fontWeight = weight,
	fontSize = size.sp,
)

object ProofType {
	val label = mono(11, FontWeight.Medium)
	val small = mono(11)
	val body = mono(14)
	val strong = mono(14, FontWeight.SemiBold)
	val heading = mono(17, FontWeight.SemiBold)
	val title = mono(20, FontWeight.SemiBold)
	val clock = mono(30, FontWeight.Medium)
}

@Composable
fun ProofLabTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit,
) {
	val palette = if (darkTheme) darkPalette else lightPalette
	val colors = if (darkTheme) {
		darkColorScheme(
			primary = palette.hot,
			background = palette.paper,
			surface = palette.card,
			onBackground = palette.ink,
			onSurface = palette.ink,
		)
	} else {
		lightColorScheme(
			primary = palette.hot,
			background = palette.paper,
			surface = palette.card,
			onBackground = palette.ink,
			onSurface = palette.ink,
		)
	}

	CompositionLocalProvider(LocalPalette provides palette) {
		MaterialTheme(
			colorScheme = colors,
			typography = Typography(
				bodyLarge = ProofType.body,
				bodyMedium = ProofType.body,
				labelSmall = ProofType.label,
			),
			content = content,
		)
	}
}

@Composable
fun StageKind.tint(): Color {
	val palette = LocalPalette.current
	return when (this) {
		StageKind.COLD_RETARD -> palette.cold
		StageKind.BAKE -> palette.hot
		StageKind.PREFERMENT -> palette.go
		else -> palette.ink
	}
}

@Composable
fun StageStatus.tint(): Color {
	val palette = LocalPalette.current
	return when (this) {
		StageStatus.OVERDUE, StageStatus.DUE -> palette.hot
		StageStatus.ACTIVE -> palette.go
		StageStatus.DONE, StageStatus.UPCOMING -> palette.inkMute
	}
}

val StageStatus.label: String
	get() = when (this) {
		StageStatus.OVERDUE -> "Overdue"
		StageStatus.DUE -> "Due"
		StageStatus.ACTIVE -> "Running"
		StageStatus.DONE -> "Done"
		StageStatus.UPCOMING -> "Waiting"
	}
