package com.justjade.myjadeai.presentation.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.justjade.myjadeai.presentation.dev.model.Model
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ModelSelectionViewModel : ViewModel() {
    private val firestore = Firebase.firestore

    private val _models = MutableStateFlow<List<Model>>(emptyList())
    val models: StateFlow<List<Model>> = _models

    init {
        fetchModels()
    }

    private fun fetchModels() {
        viewModelScope.launch {
            try {
                // We only want to show online models to the user
                val result = firestore.collection("models").whereEqualTo("status", "online").get().await()
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
}
