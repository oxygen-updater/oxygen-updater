package com.arjanvlek.oxygenupdater.extensions

import android.content.res.TypedArray
import androidx.annotation.StyleableRes

/**
 * Modified from [androidx.core.content.res.TypedArrayUtils.getString]
 *
 * @return a string value of `index`. If it does not exist, a string value of
 * `fallbackIndex`. If it still does not exist, `null`.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun TypedArray.getString(
    @StyleableRes index: Int,
    @StyleableRes fallbackIndex: Int
) = getString(index) ?: getString(fallbackIndex)
