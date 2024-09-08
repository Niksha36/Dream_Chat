package com.example.dreamchat.model

import java.io.Serializable

data class UserData(
    val id: String = "",
    val name: String = "",
    val image: String = "",
    var isSelected: Boolean = false
) : Serializable
