package com.example.instagramapp.screens

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraPreview(
    onImageCaptured: (Uri) -> Unit,
    onCameraReady: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // CameraX components
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var cameraProviderState by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val previewView = remember { PreviewView(context) }

    // Initialize camera provider
    LaunchedEffect(Unit) {
        try {
            cameraProviderState = ProcessCameraProvider.getInstance(context).get()
        } catch (e: Exception) {
            Log.e("CameraPreview", "Failed to get camera provider", e)
        }
    }

    // Bind use cases when cameraProvider is ready
    LaunchedEffect(cameraProviderState) {
        val provider = cameraProviderState ?: return@LaunchedEffect

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (ex: Exception) {
            Log.e("CameraPreview", "Failed to bind camera use cases", ex)
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    // Handle photo capture trigger
    LaunchedEffect(imageCapture) {
        onCameraReady {
            val outputDirectory = File(context.cacheDir, "images").apply { mkdirs() }
            val photoFile = File(outputDirectory, "${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(ex: ImageCaptureException) {
                        Log.e("CameraPreview", "Photo capture failed: ${ex.message}", ex)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        onImageCaptured(savedUri)
                    }
                }
            )
        }
    }
}