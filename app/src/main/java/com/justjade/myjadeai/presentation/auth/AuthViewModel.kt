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
            val user = firebaseAuth.currentUser
            _user.value = user
            if (user != null) {
                _isDevUser.value = user.uid == DEV_USER_UID
                viewModelScope.launch {
                    try {
                        val userRef = firestore.collection("users").document(user.uid)
                        if (user.uid == DEV_USER_UID) {
                            // Dev user is always approved
                            _userStatus.value = "approved"
                            // Ensure the dev user document exists and is approved in Firestore
                            val doc = userRef.get().await()
                            if (!doc.exists() || doc.getString("status") != "approved") {
                                userRef.set(mapOf("status" to "approved", "email" to user.email)).await()
                            }
                        } else {
                            // Regular user logic
                            val document = userRef.get().await()
                            if (document.exists()) {
                                _userStatus.value = document.getString("status")
                            } else {
                                // If document doesn't exist for a regular user, create it as pending
                                val userData = mapOf("status" to "pending", "email" to user.email)
                                userRef.set(userData).await()
                                _userStatus.value = "pending"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Error checking user status/document", e)
                        _userStatus.value = null // Reset status on error
                    }
                }
            } else {
                // User is logged out, reset everything
                _userStatus.value = null
                _isDevUser.value = false
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                // Auth state listener will handle the rest
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email Sign-In failed", e)
            }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                // Auth state listener will handle the rest
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email Registration failed", e)
            }
        }
    }

    fun handleGoogleSignInCredential(credential: AuthCredential) {
        viewModelScope.launch {
            try {
                auth.signInWithCredential(credential).await()
                // Auth state listener will handle the rest
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In failed", e)
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
