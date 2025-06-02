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
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.instagramapp.models.Profile
import com.example.instagramapp.viewmodels.AuthUiState
import com.example.instagramapp.viewmodels.AuthViewModel
import com.example.instagramapp.viewmodels.ProfileUiState
import com.example.instagramapp.viewmodels.ProfileViewModel

@Composable
fun EmailPasswordScreen(
    authViewModel: AuthViewModel,
    onNavigateToUsername: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
) {
    val uiState by authViewModel.uiState.collectAsState()
    val hasProfile by authViewModel.hasProfile.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState, hasProfile) {
        when (uiState) {
            is AuthUiState.Authenticated -> {
                if (hasProfile) {
                    val userUid = authViewModel.currentUser!!.uid
                    onNavigateToProfile(userUid)
                } else {
                    onNavigateToUsername()
                }
            }
            else -> {}
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
                //val user = (uiState as AuthUiState.Authenticated).user


                //Spacer(modifier = Modifier.height(16.dp))

                /*if (!user.isEmailVerified) {
                    Button(
                        onClick = { authViewModel.sendEmailVerification() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify Email")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }*/

                Button(
                    onClick = { authViewModel.reloadUser() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reload User")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { authViewModel.signOut() },
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
                        authViewModel.signIn(email, password)
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
                        authViewModel.signUp(email, password)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Account")
                }
            }
        }
    }
}

@Composable
fun SetProfileUsernameScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    onProfileCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    val uiState by profileViewModel.profileUiState.collectAsState()
    val currentUser = authViewModel.currentUser

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Loaded && currentUser != null) {
            onProfileCreated(currentUser!!.uid)
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
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = "Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (username.isBlank()) {
                    return@Button
                }
                val newProfile = Profile(
                    userUid = currentUser!!.uid,
                    username = username,
                )
                profileViewModel.createProfile(newProfile)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Profile")
        }
    }
}