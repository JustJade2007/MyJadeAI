package com.justjade.myjadeai.presentation.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.justjade.myjadeai.presentation.dev.model.Model

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(navController: NavController) {
    val viewModel: ModelSelectionViewModel = viewModel()
    val models by viewModel.models.collectAsState()
    val currentUser = Firebase.auth.currentUser

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select a Model to Chat With") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(models) { model ->
                ModelItem(model = model, onClick = {
                    currentUser?.uid?.let { userId ->
                        val modelId = model.id
                        val conversationId = if (userId < modelId) {
                            "${userId}_${modelId}"
                        } else {
                            "${modelId}_${userId}"
                        }
                        navController.navigate("chat/$conversationId")
                    }
                })
            }
        }
    }
}

@Composable
fun ModelItem(model: Model, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = model.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = "Status: ${model.status}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
