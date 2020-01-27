package com.arjanvlek.oxygenupdater.updateinformation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.dialog_update_changelog.*

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class UpdateChangelogDialogFragment(
    private val oxygenOsVersion: String?,
    private val changelog: CharSequence
) : BottomSheetDialogFragment() {

    private lateinit var dialog: BottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.dialog_update_changelog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViews()
    }

    override fun dismiss() {
        dialog.behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setupViews() {
        changelogTextView.text = changelog

        oxygenOsVersionTextView.post {
            dialog.behavior.peekHeight = oxygenOsVersionTextView.height
        }

        oxygenOsVersionTextView.setOnClickListener { dismiss() }
        oxygenOsVersionTextView.text = if (oxygenOsVersion != ApplicationData.NO_OXYGEN_OS) {
            getString(R.string.update_information_oxygen_os_version, oxygenOsVersion)
        } else {
            getString(R.string.update_information_view_update_information)
        }
    }
}
