package com.justjade.myjadeai.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore
    private val DEV_USER_UID = "Cq3vnNS8hwQjDnvSr5lRwWy9GYT2"

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val _userStatus = MutableStateFlow<String?>(null)
    val userStatus: StateFlow<String?> = _userStatus

    private val _isDevUser = MutableStateFlow(false)
    val isDevUser: StateFlow<Boolean> = _isDevUser

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                checkUserStatus()
                _isDevUser.value = firebaseAuth.currentUser?.uid == DEV_USER_UID
            } else {
                _userStatus.value = null
                _isDevUser.value = false
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                // Status will be checked by the auth state listener
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email Sign-In failed", e)
            }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                checkAndCreateUserDocument()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email Registration failed", e)
            }
        }
    }

    private fun checkUserStatus() {
        viewModelScope.launch {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                try {
                    val document = firestore.collection("users").document(firebaseUser.uid).get().await()
                    if (document.exists()) {
                        _userStatus.value = document.getString("status")
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error checking user status", e)
                }
            }
        }
    }

    fun handleGoogleSignInCredential(credential: AuthCredential) {
        viewModelScope.launch {
            try {
                auth.signInWithCredential(credential).await()
                checkAndCreateUserDocument()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In failed", e)
            }
        }
    }

    private fun checkAndCreateUserDocument() {
        viewModelScope.launch {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                val userRef = firestore.collection("users").document(firebaseUser.uid)
                val document = userRef.get().await()
                if (!document.exists()) {
                    val userData = mapOf(
                        "status" to "pending",
                        "email" to firebaseUser.email
                    )
                    userRef.set(userData).await()
                    _userStatus.value = "pending"
                } else {
                    _userStatus.value = document.getString("status")
                }
            }
        }
    }
}
