package com.oxygenupdater.ui.update

import androidx.compose.runtime.Immutable
import com.oxygenupdater.ui.common.RichTextType

@Immutable
data class DownloadErrorParams(
    val text: String,
    val type: RichTextType? = null,
    val resumable: Boolean = false,
    val callback: ((Boolean) -> Unit)? = null,
)
