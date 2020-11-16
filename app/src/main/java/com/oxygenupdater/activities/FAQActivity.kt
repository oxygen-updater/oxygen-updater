package com.oxygenupdater.activities

import android.os.Bundle
import androidx.core.view.isVisible
import com.oxygenupdater.R
import com.oxygenupdater.adapters.FaqAdapter
import com.oxygenupdater.viewmodels.FaqViewModel
import kotlinx.android.synthetic.main.activity_faq.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class FaqActivity : SupportActionBarActivity(R.layout.activity_faq) {

    private val faqViewModel by viewModel<FaqViewModel>()
    private val adapter = FaqAdapter()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        swipeRefreshLayout.apply {
            setOnRefreshListener { loadData() }
            setColorSchemeResources(R.color.colorPrimary)
        }.also { loadData() }

        faqRecyclerView.adapter = adapter
        faqViewModel.inAppFaq.observe(this) {
            shimmerFrameLayout.isVisible = false
            swipeRefreshLayout.isRefreshing = false

            adapter.submitList(it)
        }
    }

    private fun loadData() {
        shimmerFrameLayout.isVisible = true
        faqViewModel.fetchFaqCategories()
    }

    companion object {
        const val TRANSITION_NAME = "FAQ_TRANSITION"
    }
}
