package com.example.dreamchat.ViewModels

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dreamchat.model.UserData
import com.example.dreamchat.repository.FirebaseRepository
import com.example.dreamchat.util.CreatingStates
import com.example.dreamchat.util.getObject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.api.models.QueryUsersRequest
import io.getstream.chat.android.models.Filters
import io.getstream.chat.android.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val client: ChatClient,
    val firebaseRepository: FirebaseRepository,
    val sharedPreferences: SharedPreferences
) : ViewModel() {
    var loadContactsFlag = true
    private val _loginEvent = MutableSharedFlow<CreatingStates>()
    val loginEvent: SharedFlow<CreatingStates> = _loginEvent

    private val _createChannelEvent = MutableSharedFlow<CreatingStates>()
    val createChannelEvent = _createChannelEvent.asSharedFlow()

    private val _registeredUsersFromContacts =
        MutableStateFlow<MutableList<UserData>>(mutableListOf())
    val registeredUsersFromContacts = _registeredUsersFromContacts.asStateFlow()

    private val _selectedContacts = MutableStateFlow<MutableList<UserData>>(mutableListOf())
    val selectedContacts = _selectedContacts.asStateFlow()

    //Picture from CreateChannelFragment
    private val _channelImageUri = MutableStateFlow<Uri?>(null)
    val channelImageUri: StateFlow<Uri?> = _channelImageUri
    var channelImageFirestoreUri: Uri? = null
    private val _setChannelImageEvent = MutableSharedFlow<CreatingStates>()
    val setChannelImageEvent = _setChannelImageEvent.asSharedFlow()

    val selectedUserIds: MutableList<String> = mutableListOf()

    private val _userImageUri = MutableStateFlow<String>("")
    val userImageUri: StateFlow<String> = _userImageUri

    //Set user image event
    var userImageFirestoreUri: Uri? = null
    private val _setUserImageEvent = MutableSharedFlow<CreatingStates>()
    val setUserImageEvent: SharedFlow<CreatingStates> = _setUserImageEvent

    private val _logoutEvent = MutableSharedFlow<CreatingStates>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    fun getUser(): User? = client.getCurrentUser()

    fun setUser() {
        Log.e("SplashScreen", "setUser called!")
        val user = sharedPreferences.getObject<User>("user")
        user?.let { connectUser(it) }
    }

    @SuppressLint("CheckResult")
    fun logOut() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = getUser()
            try {
                firebaseRepository.saveUserToFirestore(user!!.id, user)
                Log.e("saveUserDataToFirestore", "User data has been successfully saved")
                client.disconnect(true).enqueue { result ->
                    viewModelScope.launch {
                        if (result.isSuccess) {
                            Log.d("ChatViewModel", "User disconnected successfully")
                            _logoutEvent.emit(CreatingStates.Success)
                        } else {
                            _logoutEvent.emit(CreatingStates.Error("An error occurred: could not disconnect the user"))
                            Log.e(
                                "ChatViewModel",
                                "Failed to disconnect user: ${result.errorOrNull()}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _logoutEvent.emit(CreatingStates.Error("An error occurred saving user data: ${e.message.toString()}"))
                Log.e("saveUserDataToFirestore", "Error occurred: ${e.message.toString()}")
            }
        }
    }

    //Create chat function
    fun createChat(chatName: String, userIds: MutableList<String>) =
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = getUser()!!.id
            val image = channelImageFirestoreUri?.toString() ?: ""
            val listOfUsers = userIds.toMutableList().apply { add(currentUser) }.toList()
            val result = client.createChannel(
                channelType = "messaging",
                channelId = UUID.randomUUID().toString(),
                memberIds = listOfUsers,
                extraData = mapOf(
                    "name" to chatName,
                    "image" to image
                )
            ).await()
            if (result.isFailure) {
                _createChannelEvent.emit(CreatingStates.Error("Could not create a channel"))
                return@launch
            }
            _createChannelEvent.emit(CreatingStates.Success)
        }

    //saving selected contacts
    fun saveSelectedContact(user: UserData) {
        _selectedContacts.value = _selectedContacts.value.apply { add(user) }
        selectedUserIds.add(user.id)
    }

    //deleting unselected contact
    fun deleteUnselectedContact(user: UserData) {
        _selectedContacts.value = _selectedContacts.value.apply { remove(user) }
        selectedUserIds.remove(user.id)
    }

    //Finding registered users
    fun findRegisteredUsersFromContacts(listOfUserId: Set<String>) {
        val listOfRegisteredContacts: MutableList<UserData> = mutableListOf()
        viewModelScope.launch(Dispatchers.IO) {
            val correctListOfUserId = listOfUserId.map { convertPhoneNumber(it) }.toSet()
            val registeredUsers = getAllRegisteredUsers()
            val registeredUsersInContacts = registeredUsers.filter { it.id in correctListOfUserId }
            if (registeredUsersInContacts.isNotEmpty()) {
                for (user in registeredUsersInContacts) {
                    listOfRegisteredContacts.add(
                        UserData(
                            id = user.id,
                            name = user.name,
                            image = user.image
                        )
                    )
                }
            }
            _registeredUsersFromContacts.emit(listOfRegisteredContacts)
        }
    }

    //fetching all registered users in app
    private suspend fun getAllRegisteredUsers(): List<User> {
        val request = QueryUsersRequest(
            filter = Filters.ne("id", getUser()!!.id),
            offset = 0,
            limit = 100,
        )
        val result = client.queryUsers(request).await()
        val users = result.getOrNull()
        return if (result.isSuccess && !users.isNullOrEmpty()) {
            users
        } else {
            emptyList()
        }
    }

    //checking phone fun
    private fun convertPhoneNumber(phoneNumber: String): String {
        val numericPhoneNumber = phoneNumber.filter { it.isDigit() }

        // Ensure the phone number starts with '7'
        return if (numericPhoneNumber.startsWith("7")) {
            numericPhoneNumber
        } else if (numericPhoneNumber.startsWith("8")) {
            "7" + numericPhoneNumber.drop(1)
        } else {
            numericPhoneNumber
        }
    }

    //setting imgeUri to _userImageUri
    fun setImageUri(imageUri: Uri) {
        _channelImageUri.value = imageUri
    }

    fun setUserImageUri(imageUri: String) {
        _userImageUri.value = imageUri
    }

    fun isLoggedIn() = firebaseRepository.isLoggedIn()

    //set user
    fun connectUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            val token = client.devToken(user.id)
            val result = client.connectUser(user, token).await()
            if (result.isFailure) {
                Log.e("CrashCheck", result.errorOrNull().toString())
                _loginEvent.emit(CreatingStates.Error("An error occurred:${result.errorOrNull()}"))
                return@launch
            }
            Log.e("CrashCheck", "Success log in to stream")
            _loginEvent.emit(CreatingStates.Success)
        }
    }

    fun changeUserData(name: String, lastname: String, userId: String) {
        client.disconnect(true).enqueue { result ->
            if (result.isSuccess) {
                Log.d("ChatViewModel", "User disconnected successfully")
                viewModelScope.launch(Dispatchers.IO) {
                    val user = User(
                        id = userId,
                        name = "$name $lastname",
                        image = userImageFirestoreUri?.toString() ?: ""
                    )
                    Log.e("User data", user.id)
                    val token = client.devToken(user.id)
                    val connectUserResult = client.connectUser(user, token).await()
                    if (connectUserResult.isFailure) {
                        Log.e("Checking Stream", connectUserResult.errorOrNull().toString())
                        _loginEvent.emit(CreatingStates.Error("An error occurred"))
                        return@launch
                    }
                    Log.e("Checking Stream", "Data has been successfully changed")
                    _loginEvent.emit(CreatingStates.Success)
                }
            } else {
                Log.e("ChatViewModel", "Failed to disconnect user: ${result.errorOrNull()}")
            }
        }
    }

    fun setChannelImage(photoUri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val storageUri = firebaseRepository.uploadChannelImageToStorage(photoUri)
            storageUri?.let { channelImageFirestoreUri = it }
            _setChannelImageEvent.emit(CreatingStates.Success)
        } catch (e: Exception) {
            Log.e("Storage", e.message.toString())
            _setChannelImageEvent.emit(CreatingStates.Error(e.message.toString()))
        }
    }

    fun setUserImage(photoUri: Uri) = viewModelScope.launch {
        try {
            val storageUri = firebaseRepository.uploadPhotoToStorage(photoUri, getUser()!!.id)
            storageUri?.let { userImageFirestoreUri = it }
            _setUserImageEvent.emit(CreatingStates.Success)
            Log.e("SetUserImage", "Emition")
        } catch (e: Exception) {
            Log.e("Storage", e.message.toString())
            _setUserImageEvent.emit(CreatingStates.Error(e.message.toString()))
        }
    }

    fun sighOutFireauth() = viewModelScope.launch(Dispatchers.IO) {
        firebaseRepository.signOut()
    }
}