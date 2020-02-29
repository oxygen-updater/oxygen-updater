package com.arjanvlek.oxygenupdater.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.activities.InstallActivity
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.viewmodels.InstallViewModel
import kotlinx.android.synthetic.main.fragment_install_method_chooser.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class InstallMethodChooserFragment : Fragment(R.layout.fragment_install_method_chooser) {

    private lateinit var updateData: UpdateData

    private val installViewModel by sharedViewModel<InstallViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        updateData = arguments!!.getParcelable(InstallActivity.INTENT_UPDATE_DATA)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        installViewModel.updateToolbarTitle(R.string.install_method_chooser_title)
        installViewModel.updateToolbarSubtitle(R.string.install_method_chooser_subtitle)
        installViewModel.updateToolbarImage(R.drawable.list_select)

        (activity as InstallActivity?)?.setupAppBarForMethodChooserFragment()

        automaticInstallCard.setOnClickListener { openAutomaticInstallOptionsSelection() }
        manualInstallCard.setOnClickListener { (activity as InstallActivity?)?.openInstallGuide() }
    }

    private fun openAutomaticInstallOptionsSelection() {
        if (isAdded) {
            parentFragmentManager.commit {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                replace(
                    R.id.fragmentContainer,
                    AutomaticInstallFragment().apply {
                        arguments = bundleOf(InstallActivity.INTENT_UPDATE_DATA to updateData)
                    },
                    InstallActivity.AUTOMATIC_INSTALL_FRAGMENT_TAG
                )
                addToBackStack(InstallActivity.AUTOMATIC_INSTALL_FRAGMENT_TAG)
            }
        }
    }
}
