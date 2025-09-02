package com.ntg.lmd.mainscreen.domain.model

import com.ntg.lmd.mainscreen.data.model.ActiveUserDto

data class ActiveUser(
    val id: String,
    val name: String,
)

fun ActiveUserDto.toDomain() = ActiveUser(id = id, name = name)
