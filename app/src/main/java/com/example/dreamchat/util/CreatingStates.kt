package com.example.dreamchat.util

sealed class CreatingStates {
    data object Success : CreatingStates()
    data class Error(val message: String?) : CreatingStates()
}