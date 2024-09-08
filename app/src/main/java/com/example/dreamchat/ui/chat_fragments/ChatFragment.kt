package com.example.dreamchat.ui.chat_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewbinding.ViewBinding
import com.example.dreamchat.databinding.FragmentChatBinding
import com.example.dreamchat.ui.BindingFragment
import io.getstream.chat.android.ui.common.state.messages.Edit
import io.getstream.chat.android.ui.common.state.messages.Reply
import io.getstream.chat.android.ui.viewmodel.messages.MessageComposerViewModel
import io.getstream.chat.android.ui.viewmodel.messages.MessageListHeaderViewModel
import io.getstream.chat.android.ui.viewmodel.messages.MessageListViewModel
import io.getstream.chat.android.ui.viewmodel.messages.MessageListViewModelFactory
import io.getstream.chat.android.ui.viewmodel.messages.bindView


class ChatFragment : BindingFragment<FragmentChatBinding>() {
    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentChatBinding::inflate

    private val args: ChatFragmentArgs by navArgs()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Create ViewModels for the Views
        val factory = MessageListViewModelFactory(requireContext(), cid = args.chatId)
        val messageListHeaderViewModel: MessageListHeaderViewModel by viewModels { factory }
        val messageListViewModel: MessageListViewModel by viewModels { factory }
        val messageComposerViewModel: MessageComposerViewModel by viewModels { factory }

        // Bind the ViewModels with the Views
        messageListHeaderViewModel.bindView(binding.messageListHeaderView, viewLifecycleOwner)
        messageListViewModel.bindView(binding.messageListView, viewLifecycleOwner)
        messageComposerViewModel.bindView(binding.messageComposerView, viewLifecycleOwner)

        // Let the message composer know when we are replying to a message
        binding.messageListView.setMessageReplyHandler { _, message ->
            messageComposerViewModel.performMessageAction(Reply(message))
        }

        // Let the message composer know when we are editing a message
        binding.messageListView.setMessageEditHandler { message ->
            messageComposerViewModel.performMessageAction(Edit(message))
        }

        // Handle navigate up state
        messageListViewModel.state.observe(viewLifecycleOwner) { state ->
            if (state is MessageListViewModel.State.NavigateUp) {
                findNavController().navigateUp()
            }
        }
        // Handle back button behaviour correctly when you're in a thread
        val backHandler = {
            messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed)
        }
        binding.messageListHeaderView.setBackButtonClickListener(backHandler)

        // Override the default Activity's back button behaviour
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backHandler()
                }
            }
        )
    }
}