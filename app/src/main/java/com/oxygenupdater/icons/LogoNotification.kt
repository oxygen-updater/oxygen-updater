package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Logos.LogoNotification: ImageVector
    get() = _logoNotification ?: materialIcon("LogoNotification") {
        moveTo(12f, 0f)
        lineTo(12f, 0f)
        curveTo(5.4f, 0f, 0f, 5.4f, 0f, 12f)
        verticalLineToRelative(0f)
        curveToRelative(0f, 6.6f, 5.4f, 12f, 12f, 12f)
        horizontalLineToRelative(0f)
        curveToRelative(6.6f, 0f, 12f, -5.4f, 12f, -12f)
        verticalLineToRelative(0f)
        curveTo(24f, 5.4f, 18.6f, 0f, 12f, 0f)
        close()
        moveTo(6.3f, 9.3f)
        lineToRelative(5f, -5f)
        curveToRelative(0.4f, -0.4f, 1f, -0.4f, 1.3f, 0f)
        lineToRelative(5f, 5f)
        curveToRelative(0.3f, 0.3f, 0.1f, 0.9f, -0.4f, 0.9f)
        horizontalLineToRelative(-2.5f)
        verticalLineToRelative(4.7f)
        curveToRelative(0f, 0.5f, -0.4f, 0.9f, -0.9f, 0.9f)
        horizontalLineToRelative(-3.8f)
        curveToRelative(-0.5f, 0f, -0.9f, -0.4f, -0.9f, -0.9f)
        verticalLineToRelative(-4.7f)
        horizontalLineTo(6.7f)
        curveTo(6.2f, 10.2f, 6f, 9.6f, 6.3f, 9.3f)
        close()
        moveTo(17.7f, 20f)
        horizontalLineTo(6.3f)
        curveToRelative(-0.5f, 0f, -0.9f, -0.4f, -0.9f, -0.9f)
        reflectiveCurveToRelative(0.4f, -0.9f, 0.9f, -0.9f)
        horizontalLineToRelative(11.3f)
        curveToRelative(0.5f, 0f, 0.9f, 0.4f, 0.9f, 0.9f)
        reflectiveCurveTo(18.2f, 20f, 17.7f, 20f)
        close()
    }.also { _logoNotification = it }

private var _logoNotification: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Logos.LogoNotification)
