package com.example.dreamchat.ui.chat_fragments

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.example.dreamchat.R
import com.example.dreamchat.ViewModels.ChatViewModel
import com.example.dreamchat.databinding.FragmentChangeUserDataBinding
import com.example.dreamchat.ui.BindingFragment
import com.example.dreamchat.util.CreatingStates
import com.example.dreamchat.util.ImagePickerUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ChangeUserDataFragment : BindingFragment<FragmentChangeUserDataBinding>(),
    LogoutDialogFragment.LogoutDialogListener {
    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentChangeUserDataBinding::inflate

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var imagePickerUtil: ImagePickerUtil

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        subscribeToChangeUserDataEvent()
        subscribeToLogoutEvent()
        subscribeToChangeUserImageEvent()
        subscribeToProfileImage()
        val user = viewModel.getUser()
        viewModel.setUserImageUri(user!!.image)

        val fullName = user.name.split(" ")
        val userName = fullName[0]
        val userLastname = fullName[1]
        binding.etName.text = Editable.Factory.getInstance().newEditable(userName)
        binding.etLastname.text = Editable.Factory.getInstance().newEditable(userLastname)
        imagePickerUtil = ImagePickerUtil(
            fragment = this,
            onImagePicked = {
                viewModel.setUserImageUri(it.toString())
                Glide.with(this).load(it).error(R.drawable.profile_image).into(binding.imageProfile)
                connectingUserUiState()
                viewModel.setUserImage(it)
            },
            onError = { it.printStackTrace() }
        )
        binding.flSetImage.setOnClickListener {
            imagePickerUtil.openGallery()
        }
        binding.etName.addTextChangedListener {
            binding.etName.error = null
        }
        binding.etLastname.addTextChangedListener {
            binding.etLastname.error = null
        }
        binding.buttonLogout.setOnClickListener {
            val dialog = LogoutDialogFragment(this)
            dialog.listener = this
            dialog.show(parentFragmentManager, "LogoutDialog")
        }
        binding.fabSubmitChanges.setOnClickListener {
            connectingUserUiState()
            val name = binding.etName.text.toString().trim()
            val lastname = binding.etLastname.text.toString().trim()
            if (!isCorrectName(name)) {
                binding.etName.error = "Name length should be greater then 2"
                notConnectingUiState()
                return@setOnClickListener
            }
            if (!isCorrectLastname(lastname)) {
                binding.etLastname.error = "Last name should not be empty"
                notConnectingUiState()
                return@setOnClickListener
            }
            viewModel.changeUserData(name, lastname, user.id)
        }
    }

    fun connectingUserUiState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.fabSubmitChanges.isEnabled = false
    }

    private fun notConnectingUiState() {
        binding.progressBar.visibility = View.INVISIBLE
        binding.fabSubmitChanges.isEnabled = true
    }

    private fun subscribeToChangeUserDataEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginEvent.collect {
                withContext(Dispatchers.Main) {
                    when (it) {
                        is CreatingStates.Success -> {
                            notConnectingUiState()
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.changeUserDataFragment, true)
                                .build()
                            findNavController().navigate(
                                R.id.action_changeUserDataFragment_to_chatListFragment,
                                null,
                                navOptions
                            )
                        }

                        is CreatingStates.Error -> {
                            notConnectingUiState()
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun subscribeToProfileImage() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userImageUri.collect {
                    if (it != "") {
                        Glide.with(this@ChangeUserDataFragment).load(it)
                            .error(R.drawable.profile_image).into(binding.imageProfile)
                    }
                }
            }
        }
    }

    fun isCorrectLastname(lastname: String) = lastname.isNotEmpty()
    fun isCorrectName(name: String) = name.length > 2
    override fun onLogoutConfirmed() {
        viewModel.logOut()

    }
    private fun subscribeToLogoutEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logoutEvent.collect {
                    withContext(Dispatchers.Main) {
                        when (it) {
                            is CreatingStates.Success -> {
                                viewModel.sighOutFireauth()
                                findNavController().navigate(
                                    R.id.enterPhoneFragment, null, NavOptions.Builder()
                                        .setPopUpTo(R.id.enterPhoneFragment, true)
                                        .build()
                                )
                            }

                            is CreatingStates.Error -> {
                                Toast.makeText(
                                    requireContext(),
                                    it.message.toString(),
                                    Toast.LENGTH_LONG
                                ).show()
                                notConnectingUiState()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun subscribeToChangeUserImageEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.setUserImageEvent.collect {
                    withContext(Dispatchers.Main) {
                        when (it) {
                            is CreatingStates.Error -> {
                                notConnectingUiState()
                                Toast.makeText(
                                    requireContext(),
                                    "An error occurred while setting image: ${it.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            is CreatingStates.Success -> {
                                notConnectingUiState()
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
    }
    // Restore the original orientation
    override fun onDestroyView() {
        super.onDestroyView()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
