package com.justjade.myjadeai.presentation.dev.model

data class User(
    val uid: String = "",
    val email: String = "",
    val status: String = "",
    val accessibleModelIds: List<String> = emptyList()
)
