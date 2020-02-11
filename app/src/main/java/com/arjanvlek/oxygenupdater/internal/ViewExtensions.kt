package com.arjanvlek.oxygenupdater.internal

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Everything in this file is taken from [com.google.android.material.internal.ViewUtils], but modified for Kotlin.
 * Got the idea from [Chris Banes' blog post](https://chris.banes.dev/2019/04/12/insets-listeners-to-layouts/)
 *
 * Purpose: since WindowInsets can be dispatched at any time, and multiple times during the lifecycle of a view,
 * we need an idempotent listener (if the listener is called multiple times with the same insets, the result should be the same each time).
 * This code block is not idempotent, because it increases the view's padding every time it's called:
 * ```
 * ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
 *     v.updatePadding(bottom = v.paddingBottom + insets.systemWindowInsets.bottom)
 *     insets
 * }
 * ```
 *
 * Wrapper around [androidx.core.view.OnApplyWindowInsetsListener] that records the * initial padding of the view
 * and requests that insets are applied when attached.
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
fun View.doOnApplyWindowInsets(function: (View, WindowInsetsCompat, InitialPadding) -> Unit) = recordInitialPaddingForView(this).let {
    // Create a snapshot of the view's padding state.
    // Set an actual OnApplyWindowInsetsListener which proxies to the given
    // lambda, also passing in the original padding state
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        function(view, insets, it)
        // Always return the insets, so that children can also use them
        insets
    }

    // request some insets
    requestApplyInsetsWhenAttached()
}

/**
 * Requests that insets should be applied to this view once it is attached.
 *
 * If a view calls requestApplyInsets() while it is not attached to the view hierarchy, the call is dropped on the floor and ignored.
 *
 * This is a common scenario when you create views in `Fragment.onCreateView()`.
 * The fix would be to make sure to simply call the method in `onStart()` instead, or use a listener to request insets once attached.
 * This extension function handles both cases.
 */
fun View.requestApplyInsetsWhenAttached() {
    if (ViewCompat.isAttachedToWindow(this)) {
        // We're already attached, just request as normal
        ViewCompat.requestApplyInsets(this)
    } else {
        // We're not attached to the hierarchy, add a listener to request when we are
        this.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(view)
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        })
    }
}

data class InitialPadding(val start: Int, val top: Int, val end: Int, val bottom: Int)

private fun recordInitialPaddingForView(view: View) = InitialPadding(
    view.paddingStart, view.paddingTop, view.paddingEnd, view.paddingBottom
)
