package com.oxygenupdater.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.oxygenupdater.R
import com.oxygenupdater.adapters.ServerMessagesAdapter
import com.oxygenupdater.models.Banner
import kotlinx.android.synthetic.main.bottom_sheet_server_messages.*

/**
 * Wrapper around [BottomSheetDialog], that shows server messages
 */
open class ServerMessagesDialog(
    /**
     * Pass this context in any classes that are being instantiated within this dialog,
     * instead of [BottomSheetDialog.getContext] to preserve theme information (colors, etc.)
     */
    private val mContext: Context,
    private val bannerList: List<Banner>
) : BottomSheetDialog(mContext) {

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).let {
        setContentView(LayoutInflater.from(mContext).inflate(R.layout.bottom_sheet_server_messages, null, false))
        setupViews()
    }

    private fun setupViews() {
        recyclerView.adapter = ServerMessagesAdapter(mContext, bannerList)

        headerTextView.setOnClickListener { cancel() }
    }
}
