package com.example.dreamchat.ui.login_fragments

import android.R as AndroidR
import com.example.dreamchat.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.example.dreamchat.databinding.FragmentEnterPhoneBinding
import com.example.dreamchat.ui.BindingFragment
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EnterPhoneFragment : BindingFragment<FragmentEnterPhoneBinding>() {
    override val bindingInflater: (LayoutInflater) -> ViewBinding
        get() = FragmentEnterPhoneBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val phoneUtil = PhoneNumberUtil.getInstance()
        val countryCodes = phoneUtil.supportedRegions.map { regionCode ->
            val countryCode = phoneUtil.getCountryCodeForRegion(regionCode)
            "+$countryCode ($regionCode)"
        }
        val spinner = binding.spinnerCountryCodes
        val adapter =
            ArrayAdapter(requireContext(), AndroidR.layout.simple_spinner_item, countryCodes)
        adapter.setDropDownViewResource(AndroidR.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        binding.fabNext.setOnClickListener {
            val phoneNumber = binding.editTextPhoneNumber.text.toString()
            val countryCode = spinner.selectedItem.toString().split(" ")[0]
            val fullPhoneNumber = "$countryCode$phoneNumber"
            try {
                val numberProto: Phonenumber.PhoneNumber = phoneUtil.parse(fullPhoneNumber, null)
                if (phoneUtil.isValidNumber(numberProto)) {
                    Toast.makeText(requireContext(), "Valid phone number", Toast.LENGTH_SHORT)
                        .show()

                    findNavController().navigate(
                        R.id.action_enterPhoneFragment_to_phoneVerificationFragment,
                        Bundle().apply{putString("phoneNumber", fullPhoneNumber)}
                    )
                } else {
                    Toast.makeText(requireContext(), "Invalid phone number", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                Log.e("Enter Phone fragment", e.message.toString())
            }
        }
    }
}