package com.oxygenupdater.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ui.theme.PreviewAppTheme

object Symbols
object Logos

inline fun materialSymbol(
    name: String,
    autoMirror: Boolean = false,
    pathBuilder: PathBuilder.() -> Unit,
) = ImageVector.Builder(
    name = name,
    defaultWidth = MaterialIconDimension,
    defaultHeight = MaterialIconDimension,
    viewportWidth = SymbolViewportDimension,
    viewportHeight = SymbolViewportDimension,
    autoMirror = autoMirror,
).symbolPath(pathBuilder).build()

inline fun ImageVector.Builder.symbolPath(
    pathBuilder: PathBuilder.() -> Unit,
) = path(
    fill = SolidColor(Color.Black),
    stroke = null,
    strokeLineWidth = 1.0f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    strokeLineMiter = 1.0f,
    pathFillType = PathFillType.NonZero,
    pathBuilder = pathBuilder,
)

inline fun materialIcon(
    name: String,
    autoMirror: Boolean = false,
    pathBuilder: PathBuilder.() -> Unit,
) = ImageVector.Builder(
    name = name,
    defaultWidth = MaterialIconDimension,
    defaultHeight = MaterialIconDimension,
    viewportWidth = MaterialIconDimension.value,
    viewportHeight = MaterialIconDimension.value,
    autoMirror = autoMirror,
).symbolPath(pathBuilder).build()

@Composable
fun PreviewIcon(icon: ImageVector) = PreviewAppTheme {
    Icon(icon, null)
}

val MaterialIconDimension = 24.dp
const val SymbolViewportDimension = 960f
