// ImagePicker.kt
package com.example.instagramapp.utils

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// ImagePicker.kt
class ImagePicker(private val context: Context) {
    var onImagePicked: ((Uri?) -> Unit)? = null
    private var isPicking: Boolean = false

    fun pickImage() {
        if (!isPicking) {
            isPicking = true
            onImagePicked?.invoke(null)
        }
    }

    fun reset() {
        isPicking = false
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
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            onImagePicked(uri)
        }
    )

    LaunchedEffect(Unit) {
        imagePicker.onImagePicked = {
            galleryLauncher.launch("image/*")
        }
    }
}