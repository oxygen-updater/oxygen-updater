package com.oxygenupdater.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val CustomIcons.Patreon: ImageVector
    get() {
        if (_patreon != null) return _patreon!!
        _patreon = materialIcon("Patreon") {
            materialPath {
                moveTo(14.8f, 9.6f)
                moveToRelative(-7.2f, 0f)
                arcToRelative(7.2f, 7.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 14.4f, dy1 = 0f)
                arcToRelative(7.2f, 7.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -14.4f, dy1 = 0f)
            }
            materialPath {
                moveTo(2f, 2.4f)
                horizontalLineToRelative(3.5f)
                verticalLineToRelative(19.2f)
                horizontalLineToRelative(-3.5f)
                close()
            }
        }
        return _patreon!!
    }

private var _patreon: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(CustomIcons.Patreon)
