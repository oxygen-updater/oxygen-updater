package com.oxygenupdater.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.adapters.AboutButtonAdapter
import com.oxygenupdater.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_about.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class AboutFragment : Fragment(R.layout.fragment_about) {

    private val mainViewModel by sharedViewModel<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainViewModel.saveSubtitleForPage(
            R.id.page_about,
            getString(R.string.summary_oxygen, BuildConfig.VERSION_NAME)
        )

        buttonRecyclerView.let { recyclerView ->
            val verticalDecorator = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            val horizontalDecorator = DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL)

            ContextCompat.getDrawable(requireContext(), R.drawable.divider)?.let {
                verticalDecorator.setDrawable(it)
                horizontalDecorator.setDrawable(it)

                recyclerView.addItemDecoration(verticalDecorator)
                recyclerView.addItemDecoration(horizontalDecorator)
            }

            // Performance optimization
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = AboutButtonAdapter(requireActivity())
        }

        // Make the links in the background story clickable.
        LinkMovementMethod.getInstance().let {
            appDescriptionTextView.movementMethod = it
            appSupportTextView.movementMethod = it
            backgroundStoryTextView.movementMethod = it
        }
    }
}
