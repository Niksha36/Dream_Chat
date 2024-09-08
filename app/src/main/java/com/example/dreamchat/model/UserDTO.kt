package com.example.dreamchat.model

import io.getstream.chat.android.PrivacySettings
import io.getstream.chat.android.models.ChannelMute
import io.getstream.chat.android.models.Device
import io.getstream.chat.android.models.Mute
import java.util.Date

data class UserDTO(
    val id: String = "",
    val role: String = "",
    val name: String = "",
    val image: String = "",
    val invisible: Boolean? = null,
    val privacySettings: PrivacySettings? = null,
    val language: String = "",
    val banned: Boolean? = null,
    val devices: List<Device> = listOf(),
    val online: Boolean = false,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val lastActive: Date? = null,
    val totalUnreadCount: Int = 0,
    val unreadChannels: Int = 0,
    val mutes: List<Mute> = listOf(),
    val teams: List<String> = listOf(),
    val channelMutes: List<ChannelMute> = emptyList(),
    val extraData: Map<String, Any> = mapOf(),
    val deactivatedAt: Date? = null
)
