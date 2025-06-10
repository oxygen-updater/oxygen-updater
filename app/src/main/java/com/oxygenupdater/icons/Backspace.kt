package com.oxygenupdater.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val CustomIcons.Backspace: ImageVector
    get() {
        if (_backspace != null) return _backspace!!
        _backspace = materialIcon("Backspace", autoMirror = true) {
            materialPath {
                moveTo(22.0f, 3.0f)
                horizontalLineTo(7.0f)
                curveTo(6.3f, 3.0f, 5.8f, 3.3f, 5.4f, 3.9f)
                curveToRelative(-1.2f, 1.7f, -3.3f, 5.0f, -4.7f, 7.0f)
                curveToRelative(-0.4f, 0.7f, -0.4f, 1.5f, 0.0f, 2.2f)
                lineToRelative(4.7f, 7.0f)
                curveTo(5.8f, 20.6f, 6.3f, 21.0f, 7.0f, 21.0f)
                horizontalLineToRelative(15.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(5.0f)
                curveTo(24.0f, 3.9f, 23.1f, 3.0f, 22.0f, 3.0f)
                close()
                moveTo(21.0f, 19.0f)
                horizontalLineTo(7.6f)
                curveToRelative(-0.3f, 0.0f, -0.6f, -0.2f, -0.8f, -0.4f)
                lineToRelative(-4.0f, -6.0f)
                curveToRelative(-0.2f, -0.3f, -0.2f, -0.8f, 0.0f, -1.1f)
                lineToRelative(4.0f, -6.0f)
                curveTo(6.9f, 5.2f, 7.3f, 5.0f, 7.6f, 5.0f)
                horizontalLineTo(21.0f)
                curveToRelative(0.6f, 0.0f, 1.0f, 0.4f, 1.0f, 1.0f)
                verticalLineToRelative(12.0f)
                curveTo(22.0f, 18.6f, 21.6f, 19.0f, 21.0f, 19.0f)
                close()
                moveTo(11.1f, 16.3f)
                lineToRelative(2.9f, -2.9f)
                lineToRelative(2.9f, 2.9f)
                curveToRelative(0.4f, 0.4f, 1.0f, 0.4f, 1.4f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(0.4f, -0.4f, 0.4f, -1.0f, 0.0f, -1.4f)
                lineTo(15.4f, 12.0f)
                lineToRelative(2.9f, -2.9f)
                curveToRelative(0.4f, -0.4f, 0.4f, -1.0f, 0.0f, -1.4f)
                verticalLineToRelative(0.0f)
                curveToRelative(-0.4f, -0.4f, -1.0f, -0.4f, -1.4f, 0.0f)
                lineTo(14.0f, 10.6f)
                lineToRelative(-2.9f, -2.9f)
                curveToRelative(-0.4f, -0.4f, -1.0f, -0.4f, -1.4f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(-0.4f, 0.4f, -0.4f, 1.0f, 0.0f, 1.4f)
                lineToRelative(2.9f, 2.9f)
                lineToRelative(-2.9f, 2.9f)
                curveToRelative(-0.4f, 0.4f, -0.4f, 1.0f, 0.0f, 1.4f)
                horizontalLineToRelative(0.0f)
                curveTo(10.1f, 16.7f, 10.7f, 16.7f, 11.1f, 16.3f)
                close()
            }
        }
        return _backspace!!
    }

private var _backspace: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(CustomIcons.Backspace)
