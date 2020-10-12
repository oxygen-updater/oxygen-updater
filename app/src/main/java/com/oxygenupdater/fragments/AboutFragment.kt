package com.oxygenupdater.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.fragment.app.Fragment
import com.oxygenupdater.ActivityLauncher
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_about.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class AboutFragment : Fragment(R.layout.fragment_about) {

    private val mainViewModel by sharedViewModel<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activityLauncher = ActivityLauncher(requireActivity())

        mainViewModel.saveSubtitleForPage(
            R.id.page_about,
            getString(R.string.summary_oxygen, BuildConfig.VERSION_NAME)
        )

        // Make the links in the background story clickable.
        backgroundStoryTextView.movementMethod = LinkMovementMethod.getInstance()

        discordButton.setOnClickListener { activityLauncher.openDiscord(requireContext()) }
        githubButton.setOnClickListener { activityLauncher.openGitHub(requireContext()) }
        websiteButton.setOnClickListener { activityLauncher.openWebsite(requireContext()) }
        emailButton.setOnClickListener { activityLauncher.openEmail(requireContext()) }
        rateButton.setOnClickListener { activityLauncher.openPlayStorePage(requireContext()) }
    }
}
