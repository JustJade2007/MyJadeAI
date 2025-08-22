package com.justjade.myjadeai.presentation.dev.model

data class Model(
    val id: String = "",
    val name: String = "",
    val status: String = "" // e.g., "online", "offline"
)

data class ServerStatus(
    val status: String = "" // e.g., "Online", "Maintenance"
)
