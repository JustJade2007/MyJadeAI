package com.justjade.myjadeai.presentation.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.justjade.myjadeai.presentation.chat.model.Conversation
import com.justjade.myjadeai.presentation.dev.model.Model
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ModelSelectionViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val _models = MutableStateFlow<List<Model>>(emptyList())
    val models: StateFlow<List<Model>> = _models

    init {
        fetchModels()
    }

    private fun fetchModels() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDoc = firestore.collection("users").document(userId).get().await()
                val accessibleModelIds = userDoc.get("accessibleModelIds") as? List<String>

                if (accessibleModelIds.isNullOrEmpty()) {
                    _models.value = emptyList()
                    return@launch
                }

                // Fetch only the models the user has access to
                // Temporarily removing the "status == online" check for debugging
                val result = firestore.collection("models")
                    .whereIn(FieldPath.documentId(), accessibleModelIds)
                    .get()
                    .await()

                val modelList = result.documents.map { document ->
                    Model(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        status = document.getString("status") ?: ""
                    )
                }
                _models.value = modelList
            } catch (e: Exception) {
                Log.e("ModelSelectionViewModel", "Error fetching models", e)
            }
        }
    }

    fun onModelSelected(model: Model, userId: String, userName: String, navigateToChat: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val modelId = model.id
                val conversationId = if (userId < modelId) "${userId}_${modelId}" else "${modelId}_${userId}"

                val conversation = Conversation(
                    id = conversationId,
                    userId = userId,
                    userName = userName,
                    modelId = modelId,
                    modelName = model.name
                )

                firestore.collection("conversations").document(conversationId)
                    .set(conversation, SetOptions.merge())
                    .await()

                navigateToChat(conversationId)
            } catch (e: Exception) {
                Log.e("ModelSelectionViewModel", "Error creating/updating conversation", e)
            }
        }
    }
}
