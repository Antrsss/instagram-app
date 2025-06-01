package com.example.instagramapp.utils

// ImagePicker.kt
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class ImagePicker(private val context: Context) {
    var onImagePicked: ((Uri?) -> Unit)? = null

    fun pickImage(onImagePicked: (Uri?) -> Unit) {
        this.onImagePicked = onImagePicked
        // Реальная реализация будет в Composable-функции
    }
}

@Composable
fun rememberImagePicker(): ImagePicker {
    val context = LocalContext.current
    return remember { ImagePicker(context) }
}

@Composable
fun RegisterImagePicker(
    imagePicker: ImagePicker,
    onImagePicked: (Uri?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> onImagePicked(uri) }
    )

    LaunchedEffect(Unit) {
        imagePicker.onImagePicked = { uri ->
            onImagePicked(uri)
        }
    }
}