package com.justjade.myjadeai.presentation.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.justjade.myjadeai.presentation.chat.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val conversationId: String) : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    init {
        listenForMessages()
    }

    private fun listenForMessages() {
        if (conversationId.isBlank()) {
            Log.w("ChatViewModel", "Conversation ID is blank. Not listening for messages.")
            return
        }
        firestore.collection("conversations").document(conversationId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ChatViewModel", "Listen failed for conversation $conversationId", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messageList = snapshot.toObjects(Message::class.java).mapIndexed { index, message ->
                        message.copy(id = snapshot.documents[index].id)
                    }
                    _messages.value = messageList
                }
            }
    }

    fun sendMessage(text: String) {
        val currentUser = auth.currentUser
        if (text.isNotBlank() && currentUser != null && conversationId.isNotBlank()) {
            val senderName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: currentUser.email ?: "Anonymous"
            val message = Message(
                senderId = currentUser.uid,
                senderName = senderName,
                text = text,
                timestamp = com.google.firebase.Timestamp.now()
            )
            firestore.collection("conversations").document(conversationId).collection("messages")
                .add(message)
                .addOnSuccessListener {
                    Log.d("ChatViewModel", "Message sent to conversation $conversationId")
                }
                .addOnFailureListener { e ->
                    Log.w("ChatViewModel", "Error sending message to conversation $conversationId", e)
                }
        }
    }
}
