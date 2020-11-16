package com.oxygenupdater.extensions

import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun MaterialButton.setup(
    string: String?,
    onClickListener: View.OnClickListener,
    @DrawableRes drawableResId: Int? = null
) {
    string?.let {
        isVisible = true
        text = it
        setOnClickListener(onClickListener)
    }

    drawableResId?.let { icon = ContextCompat.getDrawable(context, it) }
}
