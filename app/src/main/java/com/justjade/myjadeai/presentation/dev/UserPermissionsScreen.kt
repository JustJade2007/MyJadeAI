package com.justjade.myjadeai.presentation.dev

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPermissionsScreen(navController: NavController, userId: String) {
    val viewModel: UserPermissionsViewModel = viewModel(factory = UserPermissionsViewModelFactory(userId))
    val user by viewModel.user.collectAsState()
    val allModels by viewModel.models.collectAsState()

    var selectedModelIds by remember(user) {
        mutableStateOf(user?.accessibleModelIds?.toSet() ?: emptySet())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.email ?: "Manage Permissions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.savePermissions(selectedModelIds.toList())
                navController.popBackStack()
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(allModels) { model ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = selectedModelIds.contains(model.id),
                        onCheckedChange = { isChecked ->
                            selectedModelIds = if (isChecked) {
                                selectedModelIds + model.id
                            } else {
                                selectedModelIds - model.id
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = model.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
