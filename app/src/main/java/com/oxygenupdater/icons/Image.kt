package com.oxygenupdater.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val CustomIcons.Image: ImageVector
    get() {
        if (_image != null) return _image!!
        _image = materialIcon("Image") {
            materialPath {
                moveTo(21f, 19f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineTo(5f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                close()
                moveTo(8.9f, 13.98f)
                lineToRelative(2.1f, 2.53f)
                lineToRelative(3.1f, -3.99f)
                curveToRelative(0.2f, -0.26f, 0.6f, -0.26f, 0.8f, 0.01f)
                lineToRelative(3.51f, 4.68f)
                curveToRelative(0.25f, 0.33f, 0.01f, 0.8f, -0.4f, 0.8f)
                horizontalLineTo(6.02f)
                curveToRelative(-0.42f, 0f, -0.65f, -0.48f, -0.39f, -0.81f)
                lineTo(8.12f, 14f)
                curveToRelative(0.19f, -0.26f, 0.57f, -0.27f, 0.78f, -0.02f)
                close()
            }
        }
        return _image!!
    }

private var _image: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(CustomIcons.Image)
