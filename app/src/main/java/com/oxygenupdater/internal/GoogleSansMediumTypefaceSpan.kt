package com.oxygenupdater.internal

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.core.content.res.ResourcesCompat
import androidx.core.provider.FontsContractCompat.FontRequestCallback
import com.oxygenupdater.R
import com.oxygenupdater.utils.Logger.logWarning

class GoogleSansMediumTypefaceSpan(
    private val context: Context
) : MetricAffectingSpan() {

    override fun updateDrawState(ds: TextPaint) = applyTypeFace(ds)

    override fun updateMeasureState(paint: TextPaint) = applyTypeFace(paint)

    /**
     * Since we don't want the app to crash just because of a custom font,
     * this function tries to work around the "Resources.NotFoundException", that could
     * happen either because of a network error (fonts are retrieved from Google Fonts),
     * or due to the device not using the correct GMS package.
     */
    private fun applyTypeFace(paint: Paint) {
        // Use a callback for a non-blocking retrieval
        val callback = object : ResourcesCompat.FontCallback() {
            override fun onFontRetrieved(typeface: Typeface) {
                paint.typeface = typeface
            }

            override fun onFontRetrievalFailed(reason: Int) = when (reason) {
                FontRequestCallback.FAIL_REASON_PROVIDER_NOT_FOUND -> "PROVIDER_NOT_FOUND"
                FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR -> "FONT_LOAD_ERROR"
                FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND -> "FONT_NOT_FOUND"
                FontRequestCallback.FAIL_REASON_FONT_UNAVAILABLE -> "FONT_UNAVAILABLE"
                FontRequestCallback.FAIL_REASON_MALFORMED_QUERY -> "MALFORMED_QUERY"
                FontRequestCallback.FAIL_REASON_WRONG_CERTIFICATES -> "WRONG_CERTIFICATES"
                FontRequestCallback.FAIL_REASON_SECURITY_VIOLATION -> "SECURITY_VIOLATION"
                else -> "UNKNOWN"
            }.let {
                logWarning(TAG, "Font retrieval failed: $it ($reason)")
                // Fallback to the default bold font
                // paint.typeface = Typeface.DEFAULT_BOLD
            }
        }

        ResourcesCompat.getFont(
            context,
            R.font.google_sans_medium,
            callback,
            null
        )
    }

    companion object {
        private const val TAG = "GoogleSansMediumTypefaceSpan"
    }
}
