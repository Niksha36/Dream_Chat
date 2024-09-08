package com.example.dreamchat.ui.chat_fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.example.dreamchat.R
import com.example.dreamchat.databinding.DialogLogoutBinding

class LogoutDialogFragment(private val changeUserDataFragment: ChangeUserDataFragment) :
    DialogFragment() {
    interface LogoutDialogListener {
        fun onLogoutConfirmed()
    }

    var listener: LogoutDialogListener? = null

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogLogoutBinding.inflate(LayoutInflater.from(context))
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialog)

        builder.setView(binding.root)
            .setCancelable(false)

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.logoutButton.setOnClickListener {
            changeUserDataFragment.connectingUserUiState()
            listener?.onLogoutConfirmed()
            dismiss()
        }

        return builder.create()
    }
}