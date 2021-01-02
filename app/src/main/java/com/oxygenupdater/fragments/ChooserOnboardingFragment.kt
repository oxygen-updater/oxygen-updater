package com.oxygenupdater.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.oxygenupdater.R
import com.oxygenupdater.adapters.ChooserOnboardingAdapter
import com.oxygenupdater.extensions.addPlaceholderItemsForShimmer
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.models.SelectableModel
import kotlinx.android.synthetic.main.fragment_onboarding_chooser.*

abstract class ChooserOnboardingFragment : Fragment(R.layout.fragment_onboarding_chooser) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = super.onCreateView(inflater, container, savedInstanceState).also {
        it?.post {
            // placeholderItem's height is 2x 16dp padding + 24dp icon = 56dp
            addPlaceholderItemsForShimmer(inflater, container, it, R.layout.placeholder_item_onboarding_chooser, 56f)
        }
    }

    /**
     * Make sure children override this method,
     * so they can apply their own modifications to the generic UI this fragment offers
     * (e.g. update text for [onboardingChooserCaption])
     */
    abstract override fun onViewCreated(view: View, savedInstanceState: Bundle?)

    /**
     * Have a dedicated method for handling retrieving data from any data source
     */
    abstract fun fetchData()

    /**
     * Sets up the [onboardingChooserRecyclerView]. This method must be called from child classes, and is thus annotated by [CallSuper]
     */
    @CallSuper
    open fun setupRecyclerView(
        data: List<SelectableModel>,
        initialSelectedIndex: Int = -1,
        onItemSelectedListener: KotlinCallback<SelectableModel> = {}
    ) {
        // Do not load if app is in process of being exited when data arrives from server.
        if (activity == null || !isAdded) {
            return
        }

        onboardingChooserRecyclerView.let {
            // Performance optimization
            it.setHasFixedSize(true)
            it.adapter = ChooserOnboardingAdapter(
                context,
                initialSelectedIndex,
                onItemSelectedListener
            ).apply { submitList(data) }

            // for better UX, scroll down to selection, if any
            if (initialSelectedIndex != -1) {
                it.smoothScrollToPosition(initialSelectedIndex)
            }
        }

        shimmerFrameLayout.isVisible = false
    }
}
