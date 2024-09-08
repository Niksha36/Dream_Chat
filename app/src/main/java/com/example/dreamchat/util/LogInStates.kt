package com.example.dreamchat.util

sealed class LogInStates {
    object Success : LogInStates()
    object ErrorUserNameTooShort : LogInStates()
    object ErrorLastNameTooShort : LogInStates()
    data class Error(val message: String) : LogInStates()
}