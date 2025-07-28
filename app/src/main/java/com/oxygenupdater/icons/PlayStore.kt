package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Logos.PlayStore: ImageVector
    get() = _playStore ?: materialIcon("PlayStore") {
        moveToRelative(12.954f, 11.616f)
        lineToRelative(2.957f, -2.957f)
        lineTo(6.36f, 3.291f)
        curveToRelative(-0.633f, -0.342f, -1.226f, -0.39f, -1.746f, -0.016f)
        lineToRelative(8.34f, 8.341f)
        close()
        moveTo(16.415f, 15.078f)
        lineToRelative(3.074f, -1.729f)
        curveToRelative(0.6f, -0.336f, 0.929f, -0.812f, 0.929f, -1.34f)
        curveToRelative(0.0f, -0.527f, -0.329f, -1.004f, -0.928f, -1.34f)
        lineToRelative(-2.783f, -1.563f)
        lineToRelative(-3.133f, 3.132f)
        lineToRelative(2.841f, 2.84f)
        close()
        moveTo(4.1f, 4.002f)
        curveToRelative(-0.064f, 0.197f, -0.1f, 0.417f, -0.1f, 0.658f)
        verticalLineToRelative(14.705f)
        curveToRelative(0.0f, 0.381f, 0.084f, 0.709f, 0.236f, 0.97f)
        lineToRelative(8.097f, -8.098f)
        lineTo(4.1f, 4.002f)
        close()
        moveTo(12.954f, 12.857f)
        lineTo(4.902f, 20.91f)
        curveToRelative(0.154f, 0.059f, 0.32f, 0.09f, 0.495f, 0.09f)
        curveToRelative(0.312f, 0.0f, 0.637f, -0.092f, 0.968f, -0.276f)
        lineToRelative(9.255f, -5.197f)
        lineToRelative(-2.666f, -2.67f)
        close()
    }.also { _playStore = it }

private var _playStore: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Logos.PlayStore)
