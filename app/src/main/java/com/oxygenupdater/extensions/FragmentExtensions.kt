package com.oxygenupdater.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.facebook.shimmer.ShimmerFrameLayout
import com.oxygenupdater.R
import com.oxygenupdater.utils.Utils

/**
 * Inflates and adds as many placeholderItems as necessary, as per the calculation: [rootView].height / [placeholderItemHeight].
 *
 * Note: [placeholderItemHeight] must be hardcoded in the calling [Fragment], because [View.getHeight] returns 0 for views that haven't been drawn yet.
 *
 * [View.post] won't work either, because control goes into the lambda only after the view has been drawn
 *
 * @param inflater the LayoutInflater
 * @param container the container
 * @param rootView this fragment's rootView
 * @param placeholderItemHeight height of the placeholder item, in pixels
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun Fragment.addPlaceholderItemsForShimmer(
    inflater: LayoutInflater,
    container: ViewGroup?,
    rootView: View,
    @LayoutRes placeholderRes: Int,
    placeholderItemHeight: Float
) = LinearLayout(context).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    orientation = LinearLayout.VERTICAL

    // calculate how many placeholderItems to add
    val count = rootView.height / Utils.dpToPx(context!!, placeholderItemHeight).toInt()

    // add `count + 1` placeholderItems
    for (i in 0..count) {
        addView(
            // each placeholderItem must be inflated within the loop to avoid
            // the "The specified child already has a parent. You must call removeView() on the child's parent first." error
            inflater.inflate(placeholderRes, container, false)
        )
    }

    rootView.findViewById<ShimmerFrameLayout>(R.id.shimmerFrameLayout).addView(this)
}
