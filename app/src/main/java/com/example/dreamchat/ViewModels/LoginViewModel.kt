package com.example.dreamchat.ViewModels

import android.net.Uri
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dreamchat.repository.FirebaseRepository
import com.example.dreamchat.util.LogInStates
import com.example.dreamchat.util.CreatingStates
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val client: ChatClient,
    val firebaseRepository: FirebaseRepository
) : ViewModel() {
    //Firebase auth state
    private val _authState = MutableSharedFlow<CreatingStates>()
    var authState: SharedFlow<CreatingStates> = _authState
    var verificationId: String = ""
    //Stream auth state
    private val _loginEvent = MutableSharedFlow<LogInStates>()
    val loginEvent: SharedFlow<LogInStates> = _loginEvent

    private val _signInEvent = MutableSharedFlow<CreatingStates>()
    val signInEvent = _signInEvent.asSharedFlow()
    //Picture from UserDataFragment
    private val _userImageUri = MutableStateFlow<Uri?>(null)
    val userImageUri: StateFlow<Uri?> = _userImageUri
    //firestore profile image URI
    var firestoreUri: Uri? = null

    private val _setUserProfileImageEvent = MutableSharedFlow<CreatingStates>()
    val setUserProfileImageEvent: SharedFlow<CreatingStates> = _setUserProfileImageEvent
    //User telephone number
    var phoneNumber = ""
    //Firebase phone verification
    fun sendVerificationCode(
        phoneNumber: String,
        activity: FragmentActivity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) = viewModelScope.launch(Dispatchers.IO){
        firebaseRepository.sendVerificationCode(phoneNumber, activity,callbacks)
    }

    fun codeVerification(code: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val credential = firebaseRepository.verifyCode(verificationId, code)
            val result = firebaseRepository.signInWithPhoneAuthCredential(credential)
            _authState.emit(result)
        } catch (e:Exception){
            _authState.emit(CreatingStates.Error(e.message.toString()))
        }
    }

    fun setImageUri(imageUri: Uri){
        _userImageUri.value = imageUri
    }
    //Stream login
    fun setUser(name:String, lastname:String){
        viewModelScope.launch(Dispatchers.IO) {
            if (isCorrectNameAndLastname(name, lastname)){
                val user = User(
                    id = phoneNumber.replace("+", ""),
                    name = "$name $lastname",
                    image = firestoreUri?.toString() ?: ""
                )
                val token = client.devToken(user.id)
                val result = client.connectUser(user, token).await()
                if (result.isFailure) {
                    Log.e("Checking Stream", result.errorOrNull().toString())
                    _loginEvent.emit(LogInStates.Error("An error occurred"))
                    return@launch
                }
                Log.e("Checking Stream", "Success log in to stream")
                _loginEvent.emit(LogInStates.Success)
        } else{
            if (!correctName(name)) {
                _loginEvent.emit(LogInStates.ErrorUserNameTooShort)
            } else {_loginEvent.emit(LogInStates.ErrorLastNameTooShort)}
             }
        }
    }

    fun setExistingUser(userId: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val user = firebaseRepository.getFirestoreUserData(userId)
            val token = client.devToken(userId)
            val result = client.connectUser(user!!, token).await()
            if (result.isFailure) {
                Log.e("CrashCheck", result.errorOrNull().toString())
                _signInEvent.emit(CreatingStates.Error("An error occurred:${result.errorOrNull()}"))
                return@launch
            }
            Log.e("CrashCheck", "Success log in to stream")
            _signInEvent.emit(CreatingStates.Success)
        }catch (e: Exception){
            Log.e("Error in setExistingUser", "An error occured: ${e.message.toString()}")
            _signInEvent.emit(CreatingStates.Error("An error occurred:${e.message.toString()}"))
        }
    }

    fun isUserExists(userId: String) = viewModelScope.async(Dispatchers.IO) {
        firebaseRepository.isUserExists(userId)
    }

    fun uploadPhotoToStorage(photoUri: Uri) = viewModelScope.launch(Dispatchers.IO){
        try{
            val storageUri = firebaseRepository.uploadPhotoToStorage(photoUri, phoneNumber.replace("+", ""))
            storageUri?.let { firestoreUri = it }
            _setUserProfileImageEvent.emit(CreatingStates.Success)
        }catch (e:Exception){
            Log.e("Storage", e.message.toString())
            _setUserProfileImageEvent.emit(CreatingStates.Error(e.message.toString()))
        }
    }

    fun isCorrectNameAndLastname(name: String, lastname: String) = (name.length > 2) && (lastname.isNotEmpty())
    fun correctName(name: String) = name.length > 2
}