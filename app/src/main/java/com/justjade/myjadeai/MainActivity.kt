package com.justjade.myjadeai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.justjade.myjadeai.presentation.auth.AuthViewModel
import com.justjade.myjadeai.presentation.dev.DevPanelScreen
import com.justjade.myjadeai.presentation.dev.DevViewModel
import com.justjade.myjadeai.ui.theme.MyJadeAITheme
import kotlinx.coroutines.launch

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
                    val isDevUser by authViewModel.isDevUser.collectAsState()

                    NavHost(navController = navController, startDestination = if (user != null) "status_router" else "login") {
                        composable("login") {
                            LoginScreen(navController = navController, viewModel = authViewModel)
                        }
                        composable("dev_panel") {
                            val devViewModel: DevViewModel = viewModel()
                            DevPanelScreen(navController = navController, viewModel = devViewModel)
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
                            ChatScreen(isDevUser = isDevUser, navController = navController)
                        }
                    }
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
    val coroutineScope = rememberCoroutineScope()

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
            Button(onClick = { viewModel.signInWithEmail(email, password) }) {
                Text("Login")
            }
            Button(onClick = { viewModel.registerWithEmail(email, password) }) {
                Text("Register")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("74989094283-a7j7n1spbl593m6f5obgheb922vhd1rc.apps.googleusercontent.com")
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            coroutineScope.launch {
                try {
                    val result = credentialManager.getCredential(context, request)
                    val credential = result.credential
                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                            viewModel.handleGoogleSignInCredential(firebaseCredential)
                        } catch (e: GoogleIdTokenParsingException) {
                            Log.e("LoginScreen", "Google ID token parsing failed", e)
                        }
                    } else {
                        Log.e("LoginScreen", "Unexpected credential type")
                    }
                } catch (e: GetCredentialException) {
                    Log.e("LoginScreen", "Google Sign-In failed", e)
                }
            }
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
fun ChatScreen(isDevUser: Boolean, navController: NavController) {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Welcome to the Chat!", style = MaterialTheme.typography.headlineMedium)
        if (isDevUser) {
            Button(
                onClick = { navController.navigate("dev_panel") },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text("Dev Panel")
            }
        }
    }
}
