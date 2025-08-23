package com.justjade.myjadeai.presentation.dev

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justjade.myjadeai.presentation.chat.model.Conversation
import com.justjade.myjadeai.presentation.dev.model.Model
import com.justjade.myjadeai.presentation.dev.model.ServerStatus
import com.justjade.myjadeai.presentation.dev.model.User
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DevViewModel : ViewModel() {
    private val firestore = Firebase.firestore

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _models = MutableStateFlow<List<Model>>(emptyList())
    val models: StateFlow<List<Model>> = _models

    private val _serverStatus = MutableStateFlow(ServerStatus())
    val serverStatus: StateFlow<ServerStatus> = _serverStatus

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    init {
        fetchUsers()
        fetchModels()
        fetchServerStatus()
        listenForConversations()
    }

    fun fetchUsers() {
        viewModelScope.launch {
            try {
                val result = firestore.collection("users").get().await()
                val userList = result.documents.map { document ->
                    User(
                        uid = document.id,
                        email = document.getString("email") ?: "No Email",
                        status = document.getString("status") ?: "No Status"
                    )
                }
                _users.value = userList
            } catch (e: Exception) {
                Log.e("DevViewModel", "Error fetching users", e)
            }
        }
    }

    fun updateUserStatus(uid: String, status: String) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid).update("status", status).await()
                fetchUsers() // Refresh the list after updating
            } catch (e: Exception) {
                Log.e("DevViewModel", "Error updating user status", e)
            }
        }
    }

    fun fetchModels() {
        viewModelScope.launch {
            try {
                val result = firestore.collection("models").get().await()
                val modelList = result.documents.map { document ->
                    Model(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        status = document.getString("status") ?: ""
                    )
                }
                _models.value = modelList
            } catch (e: Exception) {
                Log.e("DevViewModel", "Error fetching models", e)
            }
        }
    }

    fun addModel(name: String) {
        viewModelScope.launch {
            try {
                firestore.collection("models").add(mapOf("name" to name, "status" to "offline")).await()
                fetchModels()
            } catch (e: Exception) {
                Log.e("DevViewModel", "Error adding model", e)
            }
        }
    }

    fun updateModelStatus(id: String, status: String) {
        viewModelScope.launch {
            try {
                firestore.collection("models").document(id).update("status", status).await()
                fetchModels()
            } catch (e: Exception) {
                Log.e("DevViewModel", "Error updating model status", e)
            }
        }
    }

    fun fetchServerStatus() {
        viewModelScope.launch {
            try {
                val result = firestore.collection("server_config").document("status").get().await()
                if (result.exists()) {
                    _serverStatus.value = ServerStatus(status = result.getString("status") ?: "Offline")
                }
            } catch (e: Exception) {
                Log.e("DevViewModel", "Error fetching server status", e)
            }
        }
    }

    fun updateServerStatus(status: String) {
        viewModelScope.launch {
            try {
                firestore.collection("server_config").document("status").set(mapOf("status" to status)).await()
                fetchServerStatus()
            } catch (e: Exception) {
                Log.e("DevViewModel", "Error updating server status", e)
            }
        }
    }

    private fun listenForConversations() {
        firestore.collection("conversations")
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("DevViewModel", "Listen for conversations failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val conversationList = snapshot.toObjects(Conversation::class.java)
                    _conversations.value = conversationList
                }
            }
    }
}
