package com.oxygenupdater.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val CustomIcons.Warning: ImageVector
    get() {
        if (_warning != null) return _warning!!
        _warning = materialIcon("Warning") {
            materialPath {
                moveTo(21.3f, 18f)
                lineTo(13.7f, 5f)
                curveTo(13f, 3.7f, 11f, 3.7f, 10.3f, 5f)
                lineTo(2.7f, 18f)
                curveToRelative(-0.8f, 1.3f, 0.2f, 3f, 1.7f, 3f)
                horizontalLineToRelative(15.1f)
                curveTo(21.1f, 21f, 22f, 19.3f, 21.3f, 18f)
                close()
                moveTo(17.8f, 19f)
                horizontalLineTo(6.2f)
                curveToRelative(-0.8f, 0f, -1.3f, -0.8f, -0.9f, -1.5f)
                lineToRelative(5.8f, -10f)
                curveToRelative(0.4f, -0.7f, 1.3f, -0.7f, 1.7f, 0f)
                lineToRelative(5.8f, 10f)
                curveTo(19f, 18.2f, 18.6f, 19f, 17.8f, 19f)
                close()
                moveTo(11f, 16f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-2f)
                verticalLineTo(16f)
                close()
                moveTo(12f, 10f)
                lineTo(12f, 10f)
                curveToRelative(-0.6f, 0f, -1f, 0.4f, -1f, 1f)
                verticalLineToRelative(2f)
                curveToRelative(0f, 0.6f, 0.4f, 1f, 1f, 1f)
                lineToRelative(0f, 0f)
                curveToRelative(0.6f, 0f, 1f, -0.4f, 1f, -1f)
                verticalLineToRelative(-2f)
                curveTo(13f, 10.4f, 12.6f, 10f, 12f, 10f)
                close()
            }
        }
        return _warning!!
    }

private var _warning: ImageVector? = null
