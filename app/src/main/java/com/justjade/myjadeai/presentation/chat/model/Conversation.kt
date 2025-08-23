package com.justjade.myjadeai.presentation.chat.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Conversation(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val modelId: String = "",
    val modelName: String = "",
    val lastMessageText: String = "",
    @ServerTimestamp
    val lastMessageTimestamp: Timestamp? = null
)
