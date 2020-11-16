package com.oxygenupdater.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.oxygenupdater.R
import com.oxygenupdater.adapters.ServerMessagesAdapter
import com.oxygenupdater.models.ServerMessage
import kotlinx.android.synthetic.main.bottom_sheet_server_messages.*

/**
 * Wrapper around [BottomSheetDialogFragment]
 */
class ServerMessagesDialogFragment : BottomSheetDialogFragment() {

    private val serverMessagesAdapter = ServerMessagesAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(
        R.layout.bottom_sheet_server_messages,
        container,
        false
    )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) = setupViews()

    fun submitList(
        bannerList: List<ServerMessage>
    ) = serverMessagesAdapter.submitList(bannerList)

    private fun setupViews() {
        recyclerView.apply {
            // Performance optimization
            setHasFixedSize(true)
            adapter = serverMessagesAdapter
        }

        headerTextView.setOnClickListener { dismiss() }
    }

    companion object {
        const val TAG = "ServerMessagesDialogFragment"
    }
}
