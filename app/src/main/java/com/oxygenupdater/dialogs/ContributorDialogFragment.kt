package com.oxygenupdater.dialogs

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.oxygenupdater.OxygenUpdater.Companion.VERIFY_FILE_PERMISSION
import com.oxygenupdater.R
import com.oxygenupdater.extensions.openAppDetailsPage
import com.oxygenupdater.internal.settings.SettingsManager.PROPERTY_CONTRIBUTE
import com.oxygenupdater.internal.settings.SettingsManager.getPreference
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.Logger.logInfo
import kotlinx.android.synthetic.main.bottom_sheet_contributor.*

/**
 * Wrapper around [BottomSheetDialogFragment]
 */
class ContributorDialogFragment(
    private val showEnrollment: Boolean = false
) : BottomSheetDialogFragment() {

    private val permissionCallback = ActivityResultCallback<Boolean> {
        logInfo(TAG, "`$VERIFY_FILE_PERMISSION` granted: $it")

        if (it) {
            ContributorUtils.flushSettings(true)
            dismiss()
        } else {
            Toast.makeText(
                context,
                R.string.contribute_allow_storage,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        RequestPermission(),
        permissionCallback
    )

    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ) = super.onCreateDialog(savedInstanceState).apply {
        setOnShowListener {
            // Open up the dialog fully by default
            (this as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(
        R.layout.bottom_sheet_contributor,
        container,
        false
    ).also {
        // This behaviour gets updated when checkbox value changes
        // But we still need the default behaviour to be applied
        isCancelable = true
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) = setupViews()

    private fun setupViews() {
        val initialValue = getPreference(
            PROPERTY_CONTRIBUTE,
            false
        )

        negativeButton.run {
            isVisible = showEnrollment
            setOnClickListener {
                contributeCheckbox.isChecked = initialValue.also { dismiss() }
            }
        }

        positiveButton.run {
            isVisible = showEnrollment
            setOnClickListener {
                if (contributeCheckbox.isChecked) {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        VERIFY_FILE_PERMISSION
                    ).let {
                        when {
                            it == PackageManager.PERMISSION_GRANTED -> permissionCallback.onActivityResult(true)
                            shouldShowRequestPermissionRationale(
                                VERIFY_FILE_PERMISSION
                            ) -> permissionCallback.onActivityResult(false).also {
                                handler?.postDelayed(
                                    { context.openAppDetailsPage() },
                                    2000
                                )
                            }
                            else -> requestPermissionLauncher.launch(VERIFY_FILE_PERMISSION)
                        }
                    }
                } else {
                    ContributorUtils.flushSettings(false)
                    dismiss()
                }
            }
        }

        contributeCheckbox.run {
            isVisible = showEnrollment
            isChecked = initialValue

            setOnCheckedChangeListener { _, isChecked ->
                // Allow dismissing the dialog only if value hasn't changed
                isCancelable = !showEnrollment || initialValue == isChecked
            }
        }
    }

    companion object {
        const val TAG = "ContributorDialogFragment"
    }
}
