package com.oxygenupdater.internal

import android.content.Context
import android.graphics.Paint
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.core.content.res.ResourcesCompat
import com.oxygenupdater.R

class GoogleSansMediumTypefaceSpan(private val context: Context) : MetricAffectingSpan() {

    override fun updateDrawState(ds: TextPaint) = applyTypeFace(ds)

    override fun updateMeasureState(paint: TextPaint) = applyTypeFace(paint)

    private fun applyTypeFace(paint: Paint) {
        paint.typeface = ResourcesCompat.getFont(context, R.font.google_sans_medium)
    }
}
