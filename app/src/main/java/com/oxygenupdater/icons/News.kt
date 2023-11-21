package com.oxygenupdater.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val CustomIcons.News: ImageVector
    get() {
        if (_news != null) return _news!!
        _news = materialIcon("News", autoMirror = true) {
            materialPath {
                moveTo(20f, 3f)
                horizontalLineTo(4f)
                curveTo(2.9f, 3f, 2f, 3.9f, 2f, 5f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveTo(22f, 3.9f, 21.1f, 3f, 20f, 3f)
                close()
                moveTo(20f, 18f)
                curveToRelative(0f, 0.6f, -0.4f, 1f, -1f, 1f)
                horizontalLineTo(5f)
                curveToRelative(-0.6f, 0f, -1f, -0.4f, -1f, -1f)
                verticalLineTo(6f)
                curveToRelative(0f, -0.6f, 0.4f, -1f, 1f, -1f)
                horizontalLineToRelative(14f)
                curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                verticalLineTo(18f)
                close()
                moveTo(17f, 17f)
                horizontalLineTo(7f)
                curveToRelative(-0.6f, 0f, -1f, -0.4f, -1f, -1f)
                verticalLineToRelative(0f)
                curveToRelative(0f, -0.6f, 0.4f, -1f, 1f, -1f)
                horizontalLineToRelative(10f)
                curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                verticalLineToRelative(0f)
                curveTo(18f, 16.6f, 17.6f, 17f, 17f, 17f)
                close()
                moveTo(17f, 11f)
                horizontalLineToRelative(-4f)
                curveToRelative(-0.6f, 0f, -1f, 0.4f, -1f, 1f)
                verticalLineToRelative(0f)
                curveToRelative(0f, 0.6f, 0.4f, 1f, 1f, 1f)
                horizontalLineToRelative(4f)
                curveToRelative(0.6f, 0f, 1f, -0.4f, 1f, -1f)
                verticalLineToRelative(0f)
                curveTo(18f, 11.4f, 17.6f, 11f, 17f, 11f)
                close()
                moveTo(17f, 7f)
                horizontalLineToRelative(-4f)
                curveToRelative(-0.6f, 0f, -1f, 0.4f, -1f, 1f)
                verticalLineToRelative(0f)
                curveToRelative(0f, 0.6f, 0.4f, 1f, 1f, 1f)
                horizontalLineToRelative(4f)
                curveToRelative(0.6f, 0f, 1f, -0.4f, 1f, -1f)
                verticalLineToRelative(0f)
                curveTo(18f, 7.4f, 17.6f, 7f, 17f, 7f)
                close()
                moveTo(9f, 13f)
                horizontalLineTo(7f)
                curveToRelative(-0.6f, 0f, -1f, -0.4f, -1f, -1f)
                verticalLineTo(8f)
                curveToRelative(0f, -0.6f, 0.4f, -1f, 1f, -1f)
                horizontalLineToRelative(2f)
                curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                verticalLineToRelative(4f)
                curveTo(10f, 12.6f, 9.6f, 13f, 9f, 13f)
                close()
            }
        }
        return _news!!
    }

private var _news: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(CustomIcons.News)
