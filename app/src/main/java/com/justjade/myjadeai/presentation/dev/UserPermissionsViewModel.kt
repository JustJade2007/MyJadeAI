package com.justjade.myjadeai.presentation.dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.justjade.myjadeai.presentation.dev.model.Model
import com.justjade.myjadeai.presentation.dev.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserPermissionsViewModel(private val userId: String) : ViewModel() {
    private val firestore = Firebase.firestore

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _models = MutableStateFlow<List<Model>>(emptyList())
    val models: StateFlow<List<Model>> = _models

    init {
        fetchUser()
        fetchModels()
    }

    private fun fetchUser() {
        viewModelScope.launch {
            val document = firestore.collection("users").document(userId).get().await()
            _user.value = document.toObject(User::class.java)
        }
    }

    private fun fetchModels() {
        viewModelScope.launch {
            val result = firestore.collection("models").get().await()
            _models.value = result.toObjects(Model::class.java)
        }
    }

    fun savePermissions(accessibleModelIds: List<String>) {
        viewModelScope.launch {
            firestore.collection("users").document(userId)
                .update("accessibleModelIds", accessibleModelIds)
                .await()
        }
    }
}

class UserPermissionsViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserPermissionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserPermissionsViewModel(userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
