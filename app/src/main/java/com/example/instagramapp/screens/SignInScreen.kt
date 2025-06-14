package com.example.instagramapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.instagramapp.models.Comment
import com.example.instagramapp.viewmodels.AuthUiState
import com.example.instagramapp.viewmodels.AuthViewModel
import com.example.serialization_library.SerializationFormat
import com.example.serialization_library.SerializationManager
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.util.UUID

@Composable
fun EmailPasswordScreen(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.uiState.collectLatest { state ->
            if (state is AuthUiState.Error) {
                email = ""
                password = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = uiState) {
            is AuthUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
            is AuthUiState.Error -> {
                Text(
                    text = state.message,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            is AuthUiState.Authenticated -> {
                if (state.message != null) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            else -> {}
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState) {
            is AuthUiState.Authenticated -> {
                val user = (uiState as AuthUiState.Authenticated).user
                Text(
                    text = "Signed in as: ${user.email ?: "Unknown"}",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!user.isEmailVerified) {
                    Button(
                        onClick = { viewModel.sendEmailVerification() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify Email")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = { viewModel.reloadUser() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reload User")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out")
                }
            }
            else -> {
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            return@Button
                        }
                        viewModel.signIn(email, password)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign In")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            return@Button
                        }
                        viewModel.signUp(email, password)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Account")
                }

                val serializer = SerializationManager()
                val context = LocalContext.current
                val comment = Comment(
                    authorUid = "jkvvkvl",
                    authorUsername = "Me",
                    postUuid = UUID.randomUUID(),
                    text = "New Comment",
                )
                val myObject = object {
                    val hello = "Hello"
                    val list = listOf(1, 2, 3)
                    val zero = null
                }
                val filename = "comment.xml"
                val file = File(context.applicationContext.filesDir, filename)
                //serializer.serialize(comment, file.absolutePath, SerializationFormat.JSON)
                serializer.serialize(comment, file.absolutePath, SerializationFormat.XML)
                //val commentFromXml = serializer.deserialize<Comment>("comment.xml", SerializationFormat.XML)

                val fileContent = try {
                    file.readText()
                } catch (e: Exception) {
                    "Не удалось прочитать файл: ${e.message}"
                }

                Column {
                    Text(text = "Комментарий сохранен в ${file.absolutePath}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Содержимое файла:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = fileContent)
                }
            }
        }
    }
}