package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream

@Composable
fun MultiImagePicker(
    imageUris: List<String>,
    primaryIndex: Int,
    onImagesChanged: (List<String>) -> Unit,
    onPrimaryIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    var retakeIndex by remember { mutableStateOf<Int?>(null) }

    // Helper to save Bitmap to persistent app files directory
    fun saveBitmapToLocalUri(bitmap: Bitmap): String {
        val dir = File(context.filesDir, "product_images").apply { if (!exists()) mkdirs() }
        val file = File(dir, "img_${System.currentTimeMillis()}_${(100..999).random()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(file).toString()
    }

    // Camera Launcher for continuous single/multi photo capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val uriStr = saveBitmapToLocalUri(bitmap)
            val currentRetake = retakeIndex
            if (currentRetake != null && currentRetake in imageUris.indices) {
                // Retake individual photo (Requirement 1)
                val updated = imageUris.toMutableList()
                updated[currentRetake] = uriStr
                onImagesChanged(updated)
                retakeIndex = null
            } else {
                // Continuous camera capture (Requirement 1)
                val updated = imageUris.toMutableList()
                updated.add(uriStr)
                onImagesChanged(updated)
            }
        }
    }

    // Multi-Image Gallery Picker (Requirement 2)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newUris = uris.map { it.toString() }
            val combined = (imageUris + newUris).distinct()
            onImagesChanged(combined)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCameraSafely() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Product Photos (${imageUris.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Multi-camera continuous & multi-gallery support",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Multi-Camera and Multi-Gallery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        retakeIndex = null
                        launchCameraSafely()
                    },
                    modifier = Modifier.weight(1f).testTag("btn_camera_capture"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Camera", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo")
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).testTag("btn_gallery_pick")
                ) {
                    Icon(Icons.Outlined.Collections, contentDescription = "Gallery", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Multi")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Images Strip / Thumbnails
            if (imageUris.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "No Photos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No images added. Take continuous photos or pick from gallery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(imageUris) { index, uriStr ->
                        val isPrimary = index == primaryIndex

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isPrimary) 2.5.dp else 1.dp,
                                    color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(uriStr)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Product Image $index",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { previewImageUri = uriStr }
                            )

                            // Primary Badge
                            if (isPrimary) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "Primary",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Bottom Controls Overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Move Left / Reorder
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val updated = imageUris.toMutableList()
                                            val item = updated.removeAt(index)
                                            updated.add(index - 1, item)
                                            onImagesChanged(updated)
                                            if (primaryIndex == index) onPrimaryIndexChanged(index - 1)
                                            else if (primaryIndex == index - 1) onPrimaryIndexChanged(index)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(24.dp))
                                }

                                // Make Primary
                                if (!isPrimary) {
                                    IconButton(
                                        onClick = { onPrimaryIndexChanged(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.StarBorder, contentDescription = "Make Primary", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Retake Photo (Requirement 1)
                                IconButton(
                                    onClick = {
                                        retakeIndex = index
                                        launchCameraSafely()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retake", tint = Color.Cyan, modifier = Modifier.size(14.dp))
                                }

                                // Delete individual photo (Requirement 1 & 2)
                                IconButton(
                                    onClick = {
                                        val updated = imageUris.toMutableList()
                                        updated.removeAt(index)
                                        onImagesChanged(updated)
                                        if (primaryIndex >= updated.size) {
                                            onPrimaryIndexChanged((updated.size - 1).coerceAtLeast(0))
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6B6B), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Preview Dialog (Requirement 1 & 2)
    if (previewImageUri != null) {
        Dialog(onDismissRequest = { previewImageUri = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Photo Preview", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { previewImageUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(previewImageUri)
                            .build(),
                        contentDescription = "Full Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}
