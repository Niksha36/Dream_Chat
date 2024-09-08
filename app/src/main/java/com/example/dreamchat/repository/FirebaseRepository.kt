package com.example.dreamchat.repository

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.example.dreamchat.model.UserDTO
import com.example.dreamchat.util.CreatingStates
import com.example.dreamchat.util.UserMapper.mapDTOToUser
import com.example.dreamchat.util.UserMapper.mapUserToDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.getstream.chat.android.models.User
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRepository @Inject constructor(
    val firebaseAuth: FirebaseAuth,
    val firestoreDb: FirebaseFirestore,
    val fireStorage: FirebaseStorage
) {
    fun sendVerificationCode(
        phoneNumber: String,
        activity: FragmentActivity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(verificationId: String, code: String): PhoneAuthCredential {
        return PhoneAuthProvider.getCredential(verificationId, code)
    }

    suspend fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential
    ): CreatingStates {
        return try {
            firebaseAuth.signInWithCredential(credential).await()
            CreatingStates.Success
        } catch (e: Exception) {
            CreatingStates.Error(e.message.toString())
        }
    }

    fun isLoggedIn(): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser != null
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun saveUserToFirestore(userId: String, userData: User) {
        val userDTO = mapUserToDTO(userData)
        try {
            firestoreDb.collection("users").document(userId).set(userDTO).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getFirestoreUserData(userId: String): User? {
        return try {
            val userData = firestoreDb.collection("users").document(userId).get().await()
            userData?.toObject(UserDTO::class.java)?.let { mapDTOToUser(it) }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun isUserExists(userId: String): Boolean {
        return try {
            val document = firestoreDb.collection("users").document(userId).get().await()
            document.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadPhotoToStorage(photoUri: Uri, userId: String): Uri? {
        return try {
            val storageRef = fireStorage.reference.child("user_photos/$userId/profile.jpg")
            val uploadTask = storageRef.putFile(photoUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun uploadChannelImageToStorage(photoUri: Uri): Uri? {
        return try {
            val storageRef =
                fireStorage.reference.child("channel_images/${photoUri.lastPathSegment}")
            val uploadTask = storageRef.putFile(photoUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            downloadUrl
        } catch (e: Exception) {
            throw e
        }
    }
}