@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.expensetracker.app.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.expensetracker.app.auth.AuthState
import com.expensetracker.app.auth.AuthViewModel
import com.expensetracker.app.ui.theme.AccentDivider
import com.expensetracker.app.ui.theme.DetailTopBar
import com.expensetracker.app.ui.theme.GlassPanel
import com.expensetracker.app.ui.theme.NeonPill
import com.expensetracker.app.ui.theme.ScreenEdgePadding
import com.expensetracker.app.ui.theme.SectionSpacing
import java.io.File

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val biometric by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val profilePicturePath by viewModel.profilePicturePath.collectAsStateWithLifecycle()

    var confirmSignOut by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val user = (authState as? AuthState.SignedIn)?.user

    val hasPhoto = profilePicturePath.isNotEmpty() && File(profilePicturePath).exists()

    // Gallery picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setProfilePictureUri(it) { path ->
                path?.let { p -> viewModel.setProfilePicturePath(p) }
            }
        }
    }

    // Camera capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.savePhotoFromCamera { path ->
                path?.let { p -> viewModel.setProfilePicturePath(p) }
            }
        } else {
            viewModel.cancelCameraCapture()
        }
    }

    // Camera permission
    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.createCameraImageUri()?.let { cameraLauncher.launch(it) }
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            viewModel.createCameraImageUri()?.let { cameraLauncher.launch(it) }
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title          = "Profile",
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // ── Hero header ────────────────────────────────────────────────────
            item {
                ProfileHeroSection(
                    displayName = user?.displayName ?: "Local Profile",
                    email = user?.email,
                    profilePicturePath = profilePicturePath,
                    onEditPhoto = { showPhotoSheet = true }
                )
            }

            // ── Gap ───────────────────────────────────────────────────────────
            item { Spacer(Modifier.height(SectionSpacing)) }

            // ── Security card ─────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = ScreenEdgePadding)) {
                    SectionEyebrow("Security")
                    Spacer(Modifier.height(8.dp))
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accent = MaterialTheme.colorScheme.primary,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        SettingsRow(
                            icon = Icons.Default.Fingerprint,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Biometric lock",
                            subtitle = "Require fingerprint or face to open",
                            trailing = {
                                Switch(checked = biometric, onCheckedChange = viewModel::setBiometric)
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(SectionSpacing)) }

            // ── Storage card ──────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = ScreenEdgePadding)) {
                    SectionEyebrow("Data & Privacy")
                    Spacer(Modifier.height(8.dp))
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accent = MaterialTheme.colorScheme.secondary,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        SettingsRow(
                            icon = Icons.Default.Lock,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            title = "On-device storage",
                            subtitle = "Financial records stay on this device only"
                        ) {
                            NeonPill("Local", accent = MaterialTheme.colorScheme.secondary)
                        }
                        AccentDivider(
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        SettingsRow(
                            icon = Icons.Default.Shield,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            title = "No cloud sync",
                            subtitle = "Your data is never uploaded or shared"
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(SectionSpacing * 2)) }

            // ── Sign out ──────────────────────────────────────────────────────
            item {
                Box(modifier = Modifier.padding(horizontal = ScreenEdgePadding)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                                MaterialTheme.shapes.large
                            )
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                            .clickable { confirmSignOut = true }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Sign out",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Photo source bottom sheet ──────────────────────────────────────────────
    if (showPhotoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    "Profile photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(8.dp))
                PhotoSheetOption(
                    icon = Icons.Default.CameraAlt,
                    label = "Take photo",
                    tint = MaterialTheme.colorScheme.primary
                ) {
                    showPhotoSheet = false
                    launchCamera()
                }
                PhotoSheetOption(
                    icon = Icons.Default.Image,
                    label = "Choose from gallery",
                    tint = MaterialTheme.colorScheme.secondary
                ) {
                    showPhotoSheet = false
                    imagePickerLauncher.launch("image/*")
                }
                if (hasPhoto) {
                    PhotoSheetOption(
                        icon = Icons.Default.Delete,
                        label = "Remove photo",
                        tint = MaterialTheme.colorScheme.error
                    ) {
                        showPhotoSheet = false
                        viewModel.removeProfilePicture()
                    }
                }
            }
        }
    }

    // ── Sign out confirmation ─────────────────────────────────────────────────
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("Your local data stays on this device. Signing out only clears the active session.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    authViewModel.signOut()
                    onNavigateBack()
                }) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Hero section ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeroSection(
    displayName: String,
    email: String?,
    profilePicturePath: String,
    onEditPhoto: () -> Unit
) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    val hasPhoto  = profilePicturePath.isNotEmpty() && File(profilePicturePath).exists()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.18f),
                        secondary.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background blobs
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(tertiary.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(secondary.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with camera button
            Box(contentAlignment = Alignment.BottomEnd) {
                // Gradient ring
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(primary, secondary, tertiary))
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(102.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasPhoto) {
                            AsyncImage(
                                model = File(profilePicturePath),
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // Camera FAB
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(primary, secondary))
                        )
                        .clickable { onEditPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Edit photo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Name + subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!email.isNullOrBlank()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Tap the camera to change your photo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ── Shared row for settings ────────────────────────────────────────────────────

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun SectionEyebrow(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun PhotoSheetOption(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
