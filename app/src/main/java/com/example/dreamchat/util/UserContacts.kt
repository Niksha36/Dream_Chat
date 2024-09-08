package com.example.dreamchat.util

import androidx.fragment.app.Fragment
import android.database.Cursor
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserContacts {
    suspend fun getContactList(fragment: Fragment): List<String> = withContext(Dispatchers.IO) {
        val contactList = mutableListOf<String>()
        val contentResolver = fragment.requireContext().contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val number = it.getString(numberIndex)
                contactList.add(number)
            }
        }
        contactList
    }
}