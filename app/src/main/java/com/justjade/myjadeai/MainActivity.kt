package com.justjade.myjadeai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.justjade.myjadeai.ui.theme.MyJadeAITheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyJadeAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel()
                    val navController = rememberNavController()
                    val user by authViewModel.user.collectAsState()
                    val userStatus by authViewModel.userStatus.collectAsState()

                    NavHost(navController = navController, startDestination = if (user != null) "status_router" else "login") {
                        composable("login") {
                            LoginScreen(navController = navController, viewModel = authViewModel)
                        }
                        composable("status_router") {
                            UserStatusRouter(userStatus = userStatus, navController = navController)
                        }
                        composable("pending") {
                            PendingScreen()
                        }
                        composable("declined") {
                            DeclinedScreen()
                        }
                        composable("chat") {
                            ChatScreen()
                        }
                    }
                }
            }
        }
    }
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user

    private val _userStatus = MutableStateFlow<String?>(null)
    val userStatus: StateFlow<String?> = _userStatus

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                checkUserStatus()
            } else {
                _userStatus.value = null
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
                    val userDa_userStatus.value = "pending"
                } else {
                    _userStatus.value = document.getString("status")
                }
            }
        }
    }
}

@Composable
fun UserStatusRouter(userStatus: String?, navController: NavController) {
    when (userStatus) {
        "approved" -> navController.navigate("chat") {
            popUpTo("login") { inclusive = true }
        }
        "pending" -> navController.navigate("pending") {
            popUpTo("login") { inclusive = true }
        }
        "declined" -> navController.navigate("declined") {
            popUpTo("login") { inclusive = true }
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            viewModel.handleGoogleSignInCredential(credential)
        } catch (e: ApiException) {
            Log.w("LoginScreen", "Google sign in failed", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {authViewModel.signInWithEmail(email, password)}) {
                Text("Login")
            }
            Button(onClick = {authViewModel.registerWithEmail(email, password)}) {
                Text("Register")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("74989094283-a7j7n1spbl593m6f5obgheb922vhd1rc.apps.googleusercontent.com")
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            launcher.launch(googleSignInClient.signInIntent)
        }) {
            Text("Sign in with Google")
        }
    }

    val userStatus by viewModel.userStatus.collectAsState()
    LaunchedEffect(userStatus) {
        if (userStatus != null) {
            navController.navigate("status_router") {
                popUpTo("login") { inclusive = true }
            }
        }
    }
}

@Composable
fun PendingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Your account is pending approval.", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun DeclinedScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Your account has been declined.", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun ChatScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Welcome to the Chat!", style = MaterialTheme.typography.headlineMedium)
    }
}
