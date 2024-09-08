package com.example.dreamchat.ui.login_fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.example.dreamchat.R
import com.example.dreamchat.ViewModels.LoginViewModel
import com.example.dreamchat.databinding.FragmentPhoneVerifivationBinding
import com.example.dreamchat.ui.BindingFragment
import com.example.dreamchat.util.CreatingStates
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PhoneVerificationFragment : BindingFragment<FragmentPhoneVerifivationBinding>() {
    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentPhoneVerifivationBinding::inflate

    private val viewModel: LoginViewModel by activityViewModels()
    private val phoneNumber by lazy { requireArguments().getString("phoneNumber") }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editTexts = listOf(
            binding.etDigit1, binding.etDigit2, binding.etDigit3,
            binding.etDigit4, binding.etDigit5, binding.etDigit6
        )

        fun codeVerificationFieldAvailable() {
            editTexts.forEach { it.isEnabled = true }
            binding.progressBar.visibility = View.INVISIBLE
        }

        fun codeVerificationFieldUnavailable() {
            editTexts.forEach { it.isEnabled = false }
            binding.progressBar.visibility = View.VISIBLE
        }
        codeVerificationFieldUnavailable()

        // Callback for phone number verification
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                findNavController().navigate(R.id.action_phoneVerificationFragment_to_userDataFragment)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(requireContext(), e.message.toString(), Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                viewModel.verificationId = verificationId
                Toast.makeText(
                    requireContext(),
                    requireContext().getString(R.string.sending_verification_code),
                    Toast.LENGTH_LONG
                ).show()
                codeVerificationFieldAvailable()
            }
        }

        // Send verification code
        phoneNumber?.let { viewModel.sendVerificationCode(it, requireActivity(), callbacks) }

        // User inputs code & this code checks with correct
        editTexts.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < editTexts.size - 1) {
                        editTexts[index + 1].requestFocus()
                    } else if (s?.length == 0 && index > 0) {
                        editTexts[index - 1].requestFocus()
                    }
                    if (editTexts.all { it.text.length == 1 }) {
                        val code = editTexts.joinToString("") { it.text.toString() }
                        codeVerificationFieldUnavailable()
                        viewModel.codeVerification(code)

                    }
                }
            })
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is CreatingStates.Success -> {
                            val userId = phoneNumber!!.replace("+", "")
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.enterPhoneFragment, inclusive = true)
                                .build()
                            val deffer = viewModel.isUserExists(userId).await()
                            if (deffer) {
                                viewModel.setExistingUser(userId)
                                viewModel.signInEvent.collect {
                                    when (it) {
                                        is CreatingStates.Success -> {
                                            findNavController().navigate(
                                                R.id.action_phoneVerificationFragment_to_chatListFragment,
                                                null,
                                                navOptions
                                            )
                                        }

                                        is CreatingStates.Error -> {
                                            Toast.makeText(
                                                requireContext(),
                                                it.message,
                                                Toast.LENGTH_LONG
                                            ).show()
                                            codeVerificationFieldAvailable()
                                            Log.e("error in setExistingUser", it.message.toString())
                                        }
                                    }
                                }
                            } else {
                                viewModel.phoneNumber = phoneNumber!!
                                findNavController().navigate(
                                    R.id.action_phoneVerificationFragment_to_userDataFragment,
                                    null,
                                    navOptions
                                )
                            }

                        }

                        is CreatingStates.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG)
                                .show()
                            codeVerificationFieldAvailable()
                            Log.e("Firebase", state.message.toString())
                        }
                    }
                }
            }
        }
    }
}