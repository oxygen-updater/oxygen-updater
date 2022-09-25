package com.oxygenupdater.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.oxygenupdater.adapters.ServerMessagesAdapter
import com.oxygenupdater.databinding.BottomSheetServerMessagesBinding
import com.oxygenupdater.models.ServerMessage

/**
 * Wrapper around [BottomSheetDialogFragment]
 */
class ServerMessagesDialogFragment : BottomSheetDialogFragment() {

    private val serverMessagesAdapter = ServerMessagesAdapter()

    /** Only valid between `onCreateView` and `onDestroyView` */
    private var binding: BottomSheetServerMessagesBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = BottomSheetServerMessagesBinding.inflate(inflater, container, false).run {
        binding = this
        root
    }

    override fun onDestroyView() = super.onDestroyView().also {
        binding = null
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) = setupViews()

    fun submitList(
        bannerList: List<ServerMessage>
    ) = serverMessagesAdapter.submitList(bannerList)

    private fun setupViews() {
        binding?.recyclerView?.apply {
            // Performance optimization
            setHasFixedSize(true)
            adapter = serverMessagesAdapter
        }

        binding?.headerTextView?.setOnClickListener { dismiss() }
    }

    companion object {
        const val TAG = "ServerMessagesDialogFragment"
    }
}
