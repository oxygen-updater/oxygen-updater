package com.oxygenupdater.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

val Symbols.MobileQuestion: ImageVector
    get() = _mobileQuestion ?: materialSymbol(
        name = "MobileQuestion",
    ) {
        moveTo(280.0f, 920.0f)
        quadToRelative(-33.0f, 0.0f, -56.5f, -23.5f)
        reflectiveQuadTo(200.0f, 840.0f)
        verticalLineToRelative(-720.0f)
        quadToRelative(0.0f, -33.0f, 23.5f, -56.5f)
        reflectiveQuadTo(280.0f, 40.0f)
        horizontalLineToRelative(400.0f)
        quadToRelative(33.0f, 0.0f, 56.5f, 23.5f)
        reflectiveQuadTo(760.0f, 120.0f)
        verticalLineToRelative(124.0f)
        quadToRelative(18.0f, 7.0f, 29.0f, 22.0f)
        reflectiveQuadToRelative(11.0f, 34.0f)
        verticalLineToRelative(80.0f)
        quadToRelative(0.0f, 19.0f, -11.0f, 34.0f)
        reflectiveQuadToRelative(-29.0f, 22.0f)
        verticalLineToRelative(404.0f)
        quadToRelative(0.0f, 33.0f, -23.5f, 56.5f)
        reflectiveQuadTo(680.0f, 920.0f)
        lineTo(280.0f, 920.0f)
        close()
        moveTo(280.0f, 840.0f)
        horizontalLineToRelative(400.0f)
        verticalLineToRelative(-720.0f)
        lineTo(280.0f, 120.0f)
        verticalLineToRelative(720.0f)
        close()
        moveTo(280.0f, 840.0f)
        verticalLineToRelative(-720.0f)
        verticalLineToRelative(720.0f)
        close()
        moveTo(480.0f, 680.0f)
        quadToRelative(17.0f, 0.0f, 29.5f, -12.5f)
        reflectiveQuadTo(522.0f, 638.0f)
        quadToRelative(0.0f, -17.0f, -12.5f, -29.5f)
        reflectiveQuadTo(480.0f, 596.0f)
        quadToRelative(-17.0f, 0.0f, -29.5f, 12.5f)
        reflectiveQuadTo(438.0f, 638.0f)
        quadToRelative(0.0f, 17.0f, 12.5f, 29.5f)
        reflectiveQuadTo(480.0f, 680.0f)
        close()
        moveTo(389.0f, 373.0f)
        quadToRelative(11.0f, 6.0f, 22.5f, 3.5f)
        reflectiveQuadTo(431.0f, 363.0f)
        quadToRelative(8.0f, -12.0f, 21.5f, -18.5f)
        reflectiveQuadTo(480.0f, 338.0f)
        quadToRelative(21.0f, 0.0f, 37.5f, 13.0f)
        reflectiveQuadToRelative(16.5f, 33.0f)
        quadToRelative(0.0f, 17.0f, -10.5f, 31.0f)
        reflectiveQuadTo(500.0f, 442.0f)
        quadToRelative(-19.0f, 17.0f, -34.0f, 37.0f)
        reflectiveQuadToRelative(-15.0f, 45.0f)
        quadToRelative(0.0f, 12.0f, 8.5f, 20.0f)
        reflectiveQuadToRelative(20.5f, 8.0f)
        quadToRelative(12.0f, 0.0f, 20.5f, -8.5f)
        reflectiveQuadTo(511.0f, 523.0f)
        quadToRelative(3.0f, -18.0f, 15.5f, -31.0f)
        reflectiveQuadToRelative(25.5f, -26.0f)
        quadToRelative(17.0f, -17.0f, 29.5f, -38.0f)
        reflectiveQuadToRelative(12.5f, -46.0f)
        quadToRelative(0.0f, -47.0f, -33.0f, -74.5f)
        reflectiveQuadTo(480.0f, 280.0f)
        quadToRelative(-30.0f, 0.0f, -56.5f, 14.5f)
        reflectiveQuadTo(380.0f, 335.0f)
        quadToRelative(-7.0f, 10.0f, -4.0f, 21.5f)
        reflectiveQuadToRelative(13.0f, 16.5f)
        close()
    }.also { _mobileQuestion = it }

private var _mobileQuestion: ImageVector? = null

@Preview
@Composable
private fun Preview() = PreviewIcon(Symbols.MobileQuestion)
