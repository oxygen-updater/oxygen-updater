package com.oxygenupdater.compose.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val CustomIcons.Info: ImageVector
    get() {
        if (_info != null) return _info!!
        _info = materialIcon("Info") {
            materialPath {
                moveTo(11f, 7f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-2f)
                verticalLineTo(7f)
                close()
                moveTo(12f, 11f)
                lineTo(12f, 11f)
                curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 0.6f, -0.4f, 1f, -1f, 1f)
                horizontalLineToRelative(0f)
                curveToRelative(-0.6f, 0f, -1f, -0.4f, -1f, -1f)
                verticalLineToRelative(-4f)
                curveTo(11f, 11.4f, 11.4f, 11f, 12f, 11f)
                close()
                moveTo(12f, 2f)
                curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
                reflectiveCurveToRelative(4.5f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.5f, 10f, -10f)
                reflectiveCurveTo(17.5f, 2f, 12f, 2f)
                close()
                moveTo(12f, 20f)
                curveToRelative(-4.4f, 0f, -8f, -3.6f, -8f, -8f)
                reflectiveCurveToRelative(3.6f, -8f, 8f, -8f)
                reflectiveCurveToRelative(8f, 3.6f, 8f, 8f)
                reflectiveCurveTo(16.4f, 20f, 12f, 20f)
                close()
            }
        }
        return _info!!
    }

private var _info: ImageVector? = null
