package com.oxygenupdater.activities

import android.os.Bundle
import androidx.core.view.isVisible
import com.oxygenupdater.R
import com.oxygenupdater.adapters.FaqAdapter
import com.oxygenupdater.databinding.ActivityFaqBinding
import com.oxygenupdater.viewmodels.FaqViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class FaqActivity : SupportActionBarActivity(
    R.layout.activity_faq,
    MainActivity.PAGE_ABOUT
) {

    private val faqViewModel by viewModel<FaqViewModel>()
    private val adapter = FaqAdapter()

    private lateinit var binding: ActivityFaqBinding
    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        binding = ActivityFaqBinding.bind(rootView)
        binding.swipeRefreshLayout.apply {
            setOnRefreshListener { loadData() }
            setColorSchemeResources(R.color.colorPrimary)
        }.also { loadData() }

        binding.faqRecyclerView.adapter = adapter
        faqViewModel.inAppFaq.observe(this) {
            binding.shimmerFrameLayout.isVisible = false
            binding.swipeRefreshLayout.isRefreshing = false

            adapter.submitList(it)
        }
    }

    private fun loadData() {
        binding.shimmerFrameLayout.isVisible = true
        faqViewModel.fetchFaqCategories()
    }

    companion object {
        const val TRANSITION_NAME = "FAQ_TRANSITION"
    }
}
