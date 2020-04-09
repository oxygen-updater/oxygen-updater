package com.arjanvlek.oxygenupdater.extensions

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * Reduces drag sensitivity of [ViewPager2] widget. Should be used in complex layouts
 * where ViewPager2 contains child layouts that have vertical scrolling.
 * This uses reflection because unfortunately, AndroidX classes are tightly-locked down nowadays.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun ViewPager2.reduceDragSensitivity() {
    try {
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
        val recyclerView = recyclerViewField.get(this) as RecyclerView

        val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
        touchSlopField.isAccessible = true
        val touchSlop = touchSlopField.get(recyclerView) as Int
        touchSlopField.set(recyclerView, touchSlop * 3) // "3" was obtained experimentally
    } catch (e: Exception) {
        // no-op
    }
}
