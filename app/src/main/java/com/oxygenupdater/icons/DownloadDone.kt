package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.DownloadDone: ImageVector
    get() = _downloadDone ?: materialSymbol(
        name = "DownloadDone",
    ) {
        moveToRelative(382.0f, 526.0f)
        lineToRelative(338.0f, -338.0f)
        quadToRelative(12.0f, -12.0f, 28.5f, -12.0f)
        reflectiveQuadToRelative(28.5f, 12.0f)
        quadToRelative(12.0f, 12.0f, 12.0f, 28.5f)
        reflectiveQuadTo(777.0f, 245.0f)
        lineTo(410.0f, 612.0f)
        quadToRelative(-12.0f, 12.0f, -28.0f, 12.0f)
        reflectiveQuadToRelative(-28.0f, -12.0f)
        lineTo(183.0f, 441.0f)
        quadToRelative(-12.0f, -12.0f, -11.5f, -28.5f)
        reflectiveQuadTo(184.0f, 384.0f)
        quadToRelative(12.0f, -12.0f, 28.5f, -12.0f)
        reflectiveQuadToRelative(28.5f, 12.0f)
        lineToRelative(141.0f, 142.0f)
        close()
        moveTo(240.0f, 800.0f)
        quadToRelative(-17.0f, 0.0f, -28.5f, -11.5f)
        reflectiveQuadTo(200.0f, 760.0f)
        quadToRelative(0.0f, -17.0f, 11.5f, -28.5f)
        reflectiveQuadTo(240.0f, 720.0f)
        horizontalLineToRelative(480.0f)
        quadToRelative(17.0f, 0.0f, 28.5f, 11.5f)
        reflectiveQuadTo(760.0f, 760.0f)
        quadToRelative(0.0f, 17.0f, -11.5f, 28.5f)
        reflectiveQuadTo(720.0f, 800.0f)
        lineTo(240.0f, 800.0f)
        close()
    }.also { _downloadDone = it }

private var _downloadDone: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.DownloadDone)
