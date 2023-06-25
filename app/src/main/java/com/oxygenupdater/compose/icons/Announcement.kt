package com.oxygenupdater.compose.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val CustomIcons.Announcement: ImageVector
    get() {
        if (_announcement != null) return _announcement!!
        _announcement = materialIcon("Announcement") {
            materialPath {
                moveTo(20f, 2f)
                horizontalLineTo(4f)
                curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
                verticalLineToRelative(18f)
                lineToRelative(4f, -4f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
                close()
                moveTo(20f, 15f)
                curveToRelative(0f, 0.6f, -0.4f, 1f, -1f, 1f)
                horizontalLineTo(5.2f)
                lineTo(4f, 17.2f)
                verticalLineTo(5f)
                curveToRelative(0f, -0.6f, 0.4f, -1f, 1f, -1f)
                horizontalLineToRelative(14f)
                curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                verticalLineTo(15f)
                close()
            }
            materialPath {
                moveTo(12f, 5f)
                lineTo(12f, 5f)
                curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 0.6f, -0.4f, 1f, -1f, 1f)
                horizontalLineToRelative(0f)
                curveToRelative(-0.6f, 0f, -1f, -0.4f, -1f, -1f)
                verticalLineTo(6f)
                curveTo(11f, 5.4f, 11.4f, 5f, 12f, 5f)
                close()
            }
            materialPath {
                moveTo(11f, 13f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(-2f)
                close()
            }
        }
        return _announcement!!
    }

private var _announcement: ImageVector? = null
