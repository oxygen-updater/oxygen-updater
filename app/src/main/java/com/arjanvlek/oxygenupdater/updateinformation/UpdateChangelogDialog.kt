package com.arjanvlek.oxygenupdater.updateinformation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet_update_changelog.*

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
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
        oxygenOsVersionTextView.text = if (oxygenOsVersion != ApplicationData.NO_OXYGEN_OS) {
            oxygenOsVersion
        } else {
            mContext.getString(R.string.update_information_view_update_information)
        }

        // display a notice if the user's currently installed version doesn't match the version this changelog is meant for
        if (differentVersionChangelogNoticeText != null) {
            differentVersionChangelogNoticeTextView.text = differentVersionChangelogNoticeText
            differentVersionChangelogNoticeTextView.visibility = View.VISIBLE
            differentVersionChangelogNoticeDivider.visibility = View.VISIBLE
        }
    }
}
