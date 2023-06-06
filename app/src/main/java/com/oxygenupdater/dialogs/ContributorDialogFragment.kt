package com.oxygenupdater.dialogs

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.oxygenupdater.R
import com.oxygenupdater.databinding.BottomSheetContributorBinding
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.PROPERTY_CONTRIBUTE
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.hasRootAccess

/**
 * Wrapper around [BottomSheetDialogFragment]
 */
@RequiresApi(Build.VERSION_CODES.Q) // same as RootFileService
class ContributorDialogFragment(
    private val showEnrollment: Boolean = false
) : BottomSheetDialogFragment() {

    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ) = super.onCreateDialog(savedInstanceState).apply {
        setOnShowListener {
            // Open up the dialog fully by default
            (this as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    /** Only valid between `onCreateView` and `onDestroyView` */
    private var binding: BottomSheetContributorBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = BottomSheetContributorBinding.inflate(inflater, container, false).run {
        binding = this
        root
    }.also {
        // This behaviour gets updated when checkbox value changes
        // But we still need the default behaviour to be applied
        isCancelable = true
    }

    override fun onDestroyView() = super.onDestroyView().also {
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = setupViews()

    private fun setupViews() {
        val initialValue = PrefManager.getBoolean(PROPERTY_CONTRIBUTE, false)

        binding?.negativeButton?.run {
            isVisible = showEnrollment
            setOnClickListener {
                binding?.contributeCheckbox?.isChecked = initialValue.also { dismiss() }
            }
        }

        binding?.positiveButton?.run {
            isVisible = showEnrollment
            setOnClickListener {
                if (binding?.contributeCheckbox?.isChecked == true) hasRootAccess {
                    if (it) ContributorUtils.flushSettings(context, true).also {
                        dismiss()
                    } else Toast.makeText(context, R.string.contribute_allow_storage, Toast.LENGTH_LONG).show()
                } else ContributorUtils.flushSettings(context, false).also {
                    dismiss()
                }
            }
        }

        binding?.contributeCheckbox?.run {
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
