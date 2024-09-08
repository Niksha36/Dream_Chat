package com.example.dreamchat.ui.chat_fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.example.dreamchat.R
import com.example.dreamchat.ViewModels.ChatViewModel
import com.example.dreamchat.adapters.UserListAdapter
import com.example.dreamchat.databinding.FragmentCreateChannelBinding
import com.example.dreamchat.ui.BindingFragment
import com.example.dreamchat.util.CreatingStates
import com.example.dreamchat.util.ImagePickerUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class CreateChannelFragment : BindingFragment<FragmentCreateChannelBinding>() {
    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentCreateChannelBinding::inflate
    lateinit var myAdapter: UserListAdapter
    lateinit var imagePickerUtil: ImagePickerUtil
    private val viewModel: ChatViewModel by navGraphViewModels(R.id.chat_nav_graph) { defaultViewModelProviderFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // Set up the toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(
                requireContext(),
                R.color.whatsapp_secondary
            )
        )
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        return view

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e(
            "Checking selected users",
            "OC CreateChannelFragment ${viewModel.selectedContacts.value.toString()}"
        )
        imagePickerUtil = ImagePickerUtil(
            fragment = this,
            onImagePicked = {
                viewModel.setImageUri(it)
                Glide.with(this).load(it).into(binding.channelImage)
                creatingChannelUiState()
                viewModel.setChannelImage(it)
            },
            onError = { it.printStackTrace() }
        )
        setUpRecyclerView()
        subscribeToSetChannelImageEvent()
        subscribeToSelectedUsers()
        subscribeToCreateChannel()
        subscribeToImage()
        Log.e("ImageURI", viewModel.channelImageUri.value.toString())
        binding.channelImage.setOnClickListener {
            imagePickerUtil.openGallery()
        }
        binding.etChannelName.addTextChangedListener {
            binding.textInputLayoutChannelName.error = null
        }
        binding.fabCreateChannel.setOnClickListener {
            val channelName = binding.etChannelName.text.toString().trim()
            if (channelName.isNotEmpty()) {
                creatingChannelUiState()
                Log.e("CreatingChat", "I am here before: ${viewModel.selectedUserIds}")
                viewModel.createChat(channelName, viewModel.selectedUserIds)
            } else {
                binding.textInputLayoutChannelName.error = "Set name!"
            }
        }
    }

    fun subscribeToSelectedUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedContacts.collect {
                    myAdapter.differ.submitList(it)
                }
            }
        }
    }

    fun subscribeToCreateChannel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createChannelEvent.collect {
                    withContext(Dispatchers.Main) {
                        when (it) {
                            is CreatingStates.Success -> {
                                val navOptions = NavOptions.Builder()
                                    .setPopUpTo(R.id.chat_nav_graph, inclusive = true)
                                    .build()
                                findNavController().navigate(
                                    R.id.action_createChannelFragment_to_chatListFragment,
                                    null,
                                    navOptions
                                )
                            }

                            is CreatingStates.Error -> {
                                notCreatingChannelUiState()
                                Toast.makeText(
                                    requireContext(),
                                    "Could not create channel: ${it.message.toString()}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    fun subscribeToImage() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.channelImageUri.collectLatest {
                    it?.let {
                        Glide.with(this@CreateChannelFragment).load(it).into(binding.channelImage)
                    }
                }
            }
        }
    }

    fun subscribeToSetChannelImageEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.setChannelImageEvent.collect {
                    when (it) {
                        is CreatingStates.Error -> {
                            notCreatingChannelUiState()
                            Toast.makeText(
                                requireContext(),
                                "An error occurred while setting image: ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        is CreatingStates.Success -> {
                            notCreatingChannelUiState()
                            Toast.makeText(
                                requireContext(),
                                "Image was successfully set",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    fun setUpRecyclerView() {
        myAdapter = UserListAdapter()
        with(binding.rvSelectedContacts) {
            adapter = myAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    fun creatingChannelUiState() {
        binding.fabCreateChannel.isEnabled = false
    }

    fun notCreatingChannelUiState() {
        binding.fabCreateChannel.isEnabled = true
    }
}