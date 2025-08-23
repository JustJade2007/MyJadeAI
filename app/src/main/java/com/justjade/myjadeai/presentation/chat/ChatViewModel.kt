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
import kotlinx.coroutines.tasks.await

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
            val conversationRef = firestore.collection("conversations").document(conversationId)

            firestore.runBatch { batch ->
                // Add the new message
                batch.set(conversationRef.collection("messages").document(), message)

                // Update the last message on the conversation document
                val conversationUpdate = mapOf(
                    "lastMessageText" to text,
                    "lastMessageTimestamp" to message.timestamp
                )
                batch.update(conversationRef, conversationUpdate)
            }.addOnSuccessListener {
                Log.d("ChatViewModel", "Message sent and conversation updated for $conversationId")
            }.addOnFailureListener { e ->
                Log.w("ChatViewModel", "Error sending message for conversation $conversationId", e)
            }
        }
    }

    fun resetConversation() {
        if (conversationId.isBlank()) {
            Log.w("ChatViewModel", "Conversation ID is blank. Cannot reset.")
            return
        }
        viewModelScope.launch {
            try {
                val messageCollection = firestore.collection("conversations").document(conversationId).collection("messages")
                val messages = messageCollection.get().await()
                val batch = firestore.batch()
                for (document in messages) {
                    batch.delete(document.reference)
                }
                batch.commit().await()
                Log.d("ChatViewModel", "Conversation $conversationId reset successfully.")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error resetting conversation $conversationId", e)
            }
        }
    }
}
