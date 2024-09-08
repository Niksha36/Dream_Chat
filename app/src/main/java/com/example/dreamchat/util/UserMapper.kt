package com.example.dreamchat.util

import com.example.dreamchat.model.UserDTO
import io.getstream.chat.android.models.User

object UserMapper {
    fun mapUserToDTO(user: User): UserDTO {
        return UserDTO(
            id = user.id,
            role = user.role,
            name = user.name,
            image = user.image,
            invisible = user.invisible,
            privacySettings = user.privacySettings,
            language = user.language,
            banned = user.banned,
            devices = user.devices,
            online = user.online,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            lastActive = user.lastActive,
            totalUnreadCount = user.totalUnreadCount,
            unreadChannels = user.unreadChannels,
            mutes = user.mutes,
            teams = user.teams,
            channelMutes = user.channelMutes,
            extraData = user.extraData,
            deactivatedAt = user.deactivatedAt
        )
    }

    fun mapDTOToUser(userDTO: UserDTO): User {
        return User(
            id = userDTO.id,
            role = userDTO.role,
            name = userDTO.name,
            image = userDTO.image,
            invisible = userDTO.invisible,
            privacySettings = userDTO.privacySettings,
            language = userDTO.language,
            banned = userDTO.banned,
            devices = userDTO.devices,
            online = userDTO.online,
            createdAt = userDTO.createdAt,
            updatedAt = userDTO.updatedAt,
            lastActive = userDTO.lastActive,
            totalUnreadCount = userDTO.totalUnreadCount,
            unreadChannels = userDTO.unreadChannels,
            mutes = userDTO.mutes,
            teams = userDTO.teams,
            channelMutes = userDTO.channelMutes,
            extraData = userDTO.extraData,
            deactivatedAt = userDTO.deactivatedAt
        )
    }
}