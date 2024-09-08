package com.example.dreamchat.ui.login_fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.example.dreamchat.R
import com.example.dreamchat.ViewModels.LoginViewModel
import com.example.dreamchat.databinding.FragmentUserDataBinding
import com.example.dreamchat.ui.BindingFragment
import com.example.dreamchat.util.CreatingStates
import com.example.dreamchat.util.ImagePickerUtil
import com.example.dreamchat.util.LogInStates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class UserDataFragment : BindingFragment<FragmentUserDataBinding>() {
    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentUserDataBinding::inflate

    private val viewModel: LoginViewModel by activityViewModels()
    private lateinit var imagePickerUtil: ImagePickerUtil

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToEvents()
        subscribeToUserSetImage()
        imagePickerUtil = ImagePickerUtil(
            fragment = this,
            onImagePicked = {
                viewModel.setImageUri(it)
                Glide.with(this).load(it).into(binding.imageProfile)
                Toast.makeText(requireContext(), "Setting image!", Toast.LENGTH_SHORT).show()
                connectingUserUiState()
                viewModel.uploadPhotoToStorage(it)
            },
            onError = { it.printStackTrace() }
        )
        binding.etName.addTextChangedListener {
            binding.etName.error = null
        }
        binding.etLastname.addTextChangedListener {
            binding.etLastname.error = null
        }
        //открываем галлерею при нажатии на изображение с картинкой профиля
        binding.imageProfile.setOnClickListener {
            imagePickerUtil.openGallery()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userImageUri.collectLatest {
                    it?.let {
                        Glide.with(this@UserDataFragment).load(it).into(binding.imageProfile)
                    }
                }
            }
        }
        binding.fabNext.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val lastname = binding.etLastname.text.toString().trim()
            connectingUserUiState()
            viewModel.setUser(name, lastname)
        }
    }

    private fun connectingUserUiState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.fabNext.isEnabled = false
    }

    private fun notConnectionUiState() {
        binding.progressBar.visibility = View.INVISIBLE
        binding.fabNext.isEnabled = true
    }

    private fun subscribeToEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginEvent.collect {
                withContext(Dispatchers.Main) {
                    when (it) {
                        is LogInStates.Success -> {
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.enterPhoneFragment, inclusive = true)
                                .build()
                            notConnectionUiState()
                            findNavController().navigate(
                                R.id.action_userDataFragment_to_chatListFragment,
                                null,
                                navOptions
                            )
                        }

                        is LogInStates.Error -> {
                            notConnectionUiState()
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                        }

                        is LogInStates.ErrorUserNameTooShort -> {
                            notConnectionUiState()
                            binding.etName.error = "Name length should be greater then 2"
                        }

                        is LogInStates.ErrorLastNameTooShort -> {
                            notConnectionUiState()
                            binding.etLastname.error = "Last name should not be empty"
                        }
                    }
                }
            }
        }
    }

    private fun subscribeToUserSetImage() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.setUserProfileImageEvent.collect {
                withContext(Dispatchers.Main) {
                    when (it) {
                        is CreatingStates.Success -> {
                            notConnectionUiState()
                            Toast.makeText(
                                requireContext(),
                                "Image was successfully set",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is CreatingStates.Error -> {
                            notConnectionUiState()
                            Toast.makeText(
                                requireContext(),
                                "An error occurred while setting image: ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
}