package com.oxygenupdater.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import kotlinx.android.synthetic.main.bottom_sheet_update_changelog.*

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class UpdateChangelogDialog(
    private val mContext: Context,
    private val oxygenOsVersion: String?,
    private val changelog: CharSequence,
    // This value is only set if the user's currently installed version doesn't match the version this changelog is meant for
    private val differentVersionChangelogNoticeText: String? = null
) : BottomSheetDialog(mContext) {

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(LayoutInflater.from(mContext).inflate(R.layout.bottom_sheet_update_changelog, null, false))
        setupViews()
    }

    private fun setupViews() {
        changelogTextView.text = changelog

        oxygenOsVersionTextView.setOnClickListener { cancel() }
        oxygenOsVersionTextView.text = if (oxygenOsVersion != OxygenUpdater.NO_OXYGEN_OS) {
            oxygenOsVersion
        } else {
            mContext.getString(R.string.update_information_view_update_information)
        }

        // display a notice if the user's currently installed version doesn't match the version this changelog is meant for
        if (differentVersionChangelogNoticeText != null) {
            differentVersionChangelogNoticeTextView.text = differentVersionChangelogNoticeText
            differentVersionChangelogNoticeTextView.isVisible = true
            differentVersionChangelogNoticeDivider.isVisible = true
        }
    }
}
