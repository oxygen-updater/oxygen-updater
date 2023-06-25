package com.oxygenupdater.compose.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val CustomIcons.Incremental: ImageVector
    get() {
        if (_incremental != null) return _incremental!!
        _incremental = materialIcon("Incremental") {
            materialPath {
                moveTo(19.5f, 9.5f)
                curveToRelative(-1.03f, 0f, -1.9f, 0.62f, -2.29f, 1.5f)
                horizontalLineToRelative(-2.92f)
                curveToRelative(-0.39f, -0.88f, -1.26f, -1.5f, -2.29f, -1.5f)
                reflectiveCurveToRelative(-1.9f, 0.62f, -2.29f, 1.5f)
                horizontalLineTo(6.79f)
                curveToRelative(-0.39f, -0.88f, -1.26f, -1.5f, -2.29f, -1.5f)
                curveTo(3.12f, 9.5f, 2f, 10.62f, 2f, 12f)
                reflectiveCurveToRelative(1.12f, 2.5f, 2.5f, 2.5f)
                curveToRelative(1.03f, 0f, 1.9f, -0.62f, 2.29f, -1.5f)
                horizontalLineToRelative(2.92f)
                curveToRelative(0.39f, 0.88f, 1.26f, 1.5f, 2.29f, 1.5f)
                reflectiveCurveToRelative(1.9f, -0.62f, 2.29f, -1.5f)
                horizontalLineToRelative(2.92f)
                curveToRelative(0.39f, 0.88f, 1.26f, 1.5f, 2.29f, 1.5f)
                curveToRelative(1.38f, 0f, 2.5f, -1.12f, 2.5f, -2.5f)
                reflectiveCurveToRelative(-1.12f, -2.5f, -2.5f, -2.5f)
                close()
            }
        }
        return _incremental!!
    }

private var _incremental: ImageVector? = null
