package com.oxygenupdater.compose.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val CustomIcons.Faq: ImageVector
    get() {
        if (_faq != null) return _faq!!
        _faq = materialIcon("Faq") {
            materialPath {
                moveTo(15f, 2f)
                horizontalLineTo(4f)
                curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
                verticalLineToRelative(13f)
                lineToRelative(4f, -4f)
                horizontalLineToRelative(9f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                curveTo(17f, 2.9f, 16.1f, 2f, 15f, 2f)
                close()
                moveTo(15f, 10f)
                curveToRelative(0f, 0.6f, -0.4f, 1f, -1f, 1f)
                horizontalLineTo(5.2f)
                lineTo(4f, 12.2f)
                verticalLineTo(5f)
                curveToRelative(0f, -0.6f, 0.4f, -1f, 1f, -1f)
                horizontalLineToRelative(9f)
                curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                verticalLineTo(10f)
                close()
                moveTo(20f, 6f)
                horizontalLineToRelative(-1f)
                verticalLineToRelative(9f)
                horizontalLineTo(6f)
                verticalLineToRelative(1f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(10f)
                lineToRelative(4f, 4f)
                verticalLineTo(8f)
                curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
                close()
            }
        }
        return _faq!!
    }

private var _faq: ImageVector? = null
