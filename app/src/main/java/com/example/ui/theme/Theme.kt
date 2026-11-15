package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class FileMasterExtraColors(
    val isDark: Boolean,
    val heroCardBg: Color,
    val heroCardBorder: Color,
    val iconBadgeBg: Color,
    val selectedRowBg: Color,
    val navBarBg: Color,
    val successBg: Color,
    val successBorder: Color,
    val successFg: Color,
    val warningBg: Color,
    val warningFg: Color,
    val errorBg: Color,
    val errorBorder: Color
)

private val DarkExtraColors = FileMasterExtraColors(
    isDark = true,
    heroCardBg = HeroCardBgDark,
    heroCardBorder = HeroCardBorderDark,
    iconBadgeBg = IconBadgeBgDark,
    selectedRowBg = SelectedRowBgDark,
    navBarBg = NavBarBgDark,
    successBg = SuccessContainerDark,
    successBorder = SuccessBorderDark,
    successFg = SuccessContentDark,
    warningBg = WarningContainerDark,
    warningFg = WarningContentDark,
    errorBg = ErrorContainerDark,
    errorBorder = ErrorBorderDark
)

private val LightExtraColors = FileMasterExtraColors(
    isDark = false,
    heroCardBg = HeroCardBgLight,
    heroCardBorder = HeroCardBorderLight,
    iconBadgeBg = IconBadgeBgLight,
    selectedRowBg = SelectedRowBgLight,
    navBarBg = NavBarBgLight,
    successBg = SuccessContainerLight,
    successBorder = SuccessBorderLight,
    successFg = SuccessContentLight,
    warningBg = WarningContainerLight,
    warningFg = WarningContentLight,
    errorBg = ErrorContainerLight,
    errorBorder = ErrorBorderLight
)

val LocalExtraColors = staticCompositionLocalOf { DarkExtraColors }

val MaterialTheme.extraColors: FileMasterExtraColors
    @Composable
    get() = LocalExtraColors.current

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemePreference {
    SYSTEM, LIGHT, DARK
}

@Composable
fun FileMasterTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extra = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
