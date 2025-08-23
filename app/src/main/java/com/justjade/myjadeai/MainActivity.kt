package com.justjade.myjadeai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.justjade.myjadeai.presentation.auth.AuthViewModel
import com.justjade.myjadeai.presentation.chat.ChatViewModel
import com.justjade.myjadeai.presentation.chat.ChatViewModelFactory
import com.justjade.myjadeai.presentation.chat.ModelSelectionScreen
import com.justjade.myjadeai.presentation.chat.model.Message
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

                    LaunchedEffect(user, userStatus, isDevUser) {
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (user == null) {
                            if (currentRoute != "login") {
                                navController.navigate("login") { popUpTo("loading") { inclusive = true } }
                            }
                        } else {
                            if (userStatus != null) {
                                val destination = when {
                                    userStatus == "approved" && isDevUser -> "dev_landing"
                                    userStatus == "approved" && !isDevUser -> "model_selection"
                                    userStatus == "pending" -> "pending"
                                    userStatus == "declined" -> "declined"
                                    else -> null
                                }
                                destination?.let {
                                    if (currentRoute != it) {
                                        navController.navigate(it) { popUpTo("loading") { inclusive = true } }
                                    }
                                }
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = "loading") {
                        composable("loading") {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        composable("login") { LoginScreen(viewModel = authViewModel) }
                        composable("model_selection") { ModelSelectionScreen(navController = navController) }
                        composable("dev_panel") { DevPanelScreen(navController = navController, viewModel = viewModel()) }
                        composable("pending") { PendingScreen(viewModel = authViewModel) }
                        composable("declined") { DeclinedScreen(viewModel = authViewModel) }
                        composable("dev_landing") { DevLandingScreen(navController = navController, authViewModel = authViewModel) }
                        composable("chat/{conversationId}") { backStackEntry ->
                            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                            val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(conversationId))
                            ChatScreen(
                                navController = navController,
                                authViewModel = authViewModel,
                                chatViewModel = chatViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { viewModel.signInWithEmail(email, password) }) { Text("Login") }
            Button(onClick = { viewModel.registerWithEmail(email, password) }) { Text("Register") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("74989094283-a7j7n1spbl593m6f5obgheb922vhd1rc.apps.googleusercontent.com")
                .build()
            val request: GetCredentialRequest = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
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
        }) { Text("Sign in with Google") }
    }
}

@Composable
fun PendingScreen(viewModel: AuthViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Your account is pending approval.", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.signOut() }) { Text("Log Out") }
    }
}

@Composable
fun DeclinedScreen(viewModel: AuthViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Your account has been declined.", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.signOut() }) { Text("Log Out") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevLandingScreen(navController: NavController, authViewModel: AuthViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Hub") },
                actions = {
                    Button(onClick = { navController.navigate("dev_panel") }) { Text("Dev Panel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { authViewModel.signOut() }) { Text("Log Out") }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            Text("Welcome, Developer!", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, authViewModel: AuthViewModel, chatViewModel: ChatViewModel) {
    val messages by chatViewModel.messages.collectAsState()
    var text by remember { mutableStateOf("") }
    val currentUser = Firebase.auth.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = { chatViewModel.resetConversation() }) { Text("Reset") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { authViewModel.signOut() }) { Text("Log Out") }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), reverseLayout = true) {
                items(messages.reversed()) { message ->
                    MessageItem(message, message.senderId == currentUser?.uid)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Message") }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    chatViewModel.sendMessage(text)
                    text = ""
                }) { Text("Send") }
            }
        }
    }
}

@Composable
fun MessageItem(message: Message, isSentByCurrentUser: Boolean) {
    val alignment = if (isSentByCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val horizontalGravity = if (isSentByCurrentUser) Alignment.End else Alignment.Start

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = alignment) {
        Column(horizontalAlignment = horizontalGravity) {
            Text(text = message.senderName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
            Text(text = message.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(8.dp))
            Text(text = message.timestamp.toDate().toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}
