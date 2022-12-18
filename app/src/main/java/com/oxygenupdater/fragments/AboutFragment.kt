package com.oxygenupdater.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.adapters.AboutButtonAdapter
import com.oxygenupdater.databinding.FragmentAboutBinding
import com.oxygenupdater.viewmodels.MainViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AboutFragment : Fragment() {

    private val mainViewModel by activityViewModel<MainViewModel>()

    /** Only valid between `onCreateView` and `onDestroyView` */
    private var binding: FragmentAboutBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = FragmentAboutBinding.inflate(inflater, container, false).run {
        binding = this
        root
    }

    override fun onDestroyView() = super.onDestroyView().also {
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainViewModel.saveSubtitleForPage(
            R.id.page_about,
            getString(R.string.summary_oxygen, BuildConfig.VERSION_NAME)
        )

        binding?.buttonRecyclerView?.let { recyclerView ->
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
            binding?.appDescriptionTextView?.movementMethod = it
            binding?.appSupportTextView?.movementMethod = it
            binding?.backgroundStoryTextView?.movementMethod = it
        }
    }
}
