package com.justjade.myjadeai.presentation.dev

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.justjade.myjadeai.presentation.dev.model.Model
import com.justjade.myjadeai.presentation.dev.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevPanelScreen(navController: NavController, viewModel: DevViewModel) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("User Management", "Model Status", "Server Status")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Panel") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            when (tabIndex) {
                0 -> UserManagementScreen(viewModel)
                1 -> ModelStatusScreen(viewModel)
                2 -> ServerStatusScreen(viewModel)
            }
        }
    }
}

@Composable
fun UserManagementScreen(viewModel: DevViewModel) {
    val users by viewModel.users.collectAsState()

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(users) { user ->
            UserRow(user = user, viewModel = viewModel)
        }
    }
}

@Composable
fun UserRow(user: User, viewModel: DevViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.email, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Status: ${user.status}", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (user.status == "pending") {
            Button(onClick = { viewModel.updateUserStatus(user.uid, "approved") }) {
                Text("Approve")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.updateUserStatus(user.uid, "declined") }) {
                Text("Decline")
            }
        } else {
            Button(onClick = { viewModel.updateUserStatus(user.uid, "pending") }) {
                Text("Revoke")
            }
        }
    }
}

@Composable
fun ModelStatusScreen(viewModel: DevViewModel) {
    val models by viewModel.models.collectAsState()
    var newModelName by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newModelName,
                onValueChange = { newModelName = it },
                label = { Text("New Model Name") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newModelName.isNotBlank()) {
                    viewModel.addModel(newModelName)
                    newModelName = ""
                }
            }) {
                Text("Add")
            }
        }
        LazyColumn {
            items(models) { model ->
                ModelRow(model = model, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ModelRow(model: Model, viewModel: DevViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = model.name, modifier = Modifier.weight(1f))
        Text(text = "Status: ${model.status}")
        Spacer(modifier = Modifier.width(8.dp))
        val newStatus = if (model.status == "online") "offline" else "online"
        Button(onClick = { viewModel.updateModelStatus(model.id, newStatus) }) {
            Text(if (model.status == "online") "Set Offline" else "Set Online")
        }
    }
}

@Composable
fun ServerStatusScreen(viewModel: DevViewModel) {
    val serverStatus by viewModel.serverStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Current Server Status: ${serverStatus.status}", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.width(16.dp))
        Row {
            Button(onClick = { viewModel.updateServerStatus("Online") }) {
                Text("Set Online")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.updateServerStatus("Maintenance") }) {
                Text("Set Maintenance")
            }
        }
    }
}
