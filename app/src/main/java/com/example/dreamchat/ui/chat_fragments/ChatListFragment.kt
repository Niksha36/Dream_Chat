package com.example.dreamchat.ui.chat_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.example.dreamchat.R
import com.example.dreamchat.ViewModels.ChatViewModel
import com.example.dreamchat.databinding.FragmentChatListBinding
import com.example.dreamchat.ui.BindingFragment
import dagger.hilt.android.AndroidEntryPoint
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.querysort.QuerySortByField
import io.getstream.chat.android.ui.viewmodel.channels.ChannelListHeaderViewModel
import io.getstream.chat.android.ui.viewmodel.channels.ChannelListViewModel
import io.getstream.chat.android.ui.viewmodel.channels.ChannelListViewModelFactory
import io.getstream.chat.android.ui.viewmodel.channels.bindView

@AndroidEntryPoint
class ChatListFragment : BindingFragment<FragmentChatListBinding>() {
    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentChatListBinding::inflate
    private val viewModel: ChatViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = viewModel.getUser()
        if (user == null) {
            findNavController().navigate(
                R.id.enterPhoneFragment, null, NavOptions.Builder()
                    .setPopUpTo(R.id.enterPhoneFragment, true)
                    .build()
            )
        }
        binding.channelListView.setChannelItemClickListener { channel ->
            findNavController().navigate(
                R.id.action_chatListFragment_to_chatFragment,
                Bundle().apply { putString("chatId", channel.cid) }
            )
        }

        binding.channelListHeaderView.setOnUserAvatarClickListener {
            findNavController().navigate(R.id.action_chatListFragment_to_changeUserDataFragment)
        }

        binding.channelListHeaderView.setOnActionButtonClickListener {
            findNavController().navigate(R.id.action_chatListFragment_to_chatsFromContactFragment)
        }

        val channelListFactory: ChannelListViewModelFactory = ChannelListViewModelFactory(
            filter = Filters.and(
                Filters.eq("type", "messaging"),
                Filters.`in`("members", listOf(user!!.id)),
            ),
            sort = QuerySortByField.descByName("last_updated"),
            limit = 30,
        )
        val channelListViewModel: ChannelListViewModel by viewModels { channelListFactory }
        val channelListHeaderViewModel: ChannelListHeaderViewModel by viewModels()
        channelListHeaderViewModel.bindView(binding.channelListHeaderView, viewLifecycleOwner)
        channelListViewModel.bindView(binding.channelListView, viewLifecycleOwner)
    }

}