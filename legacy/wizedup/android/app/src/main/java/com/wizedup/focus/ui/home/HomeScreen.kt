package com.wizedup.focus.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wizedup.focus.R
import com.wizedup.focus.data.StudentProfile
import com.wizedup.focus.ui.theme.FocusOffGreen
import com.wizedup.focus.ui.theme.FocusOffGreenContainer
import com.wizedup.focus.util.AccessibilityUtils

/**
 * Home screen (US-R1-04). Displays the green "Focus Off" state and a single dominant
 * "Activate Focus" button. Tapping it walks the user through the accessibility +
 * notification permission gates (US-R1-05) before flipping the flag.
 */
@Composable
fun HomeScreen(
    profile: StudentProfile,
    viewModel: HomeViewModel,
    onActivateFocus: () -> Unit,
) {
    val context = LocalContext.current
    val isActive: Boolean by viewModel.isFocusActive.collectAsState()

    var showAccessibilityRationale by remember { mutableStateOf(false) }

    // Notification permission launcher (Android 13+). On grant, we proceed with activate.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.activate(onActivated = onActivateFocus)
        } else {
            Toast.makeText(
                context,
                R.string.notification_permission_required,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun handleActivateTap() {
        // Tap-count anchor (US-R1-04 + engineering brief):
        // This is in-app tap #1. The branches below either proceed straight to activate
        // (when both permissions are already in order — total in-app taps = 1) or open
        // a system Settings deep-link (system-side, not counted) before the user comes
        // back and taps Activate again (in-app tap #2). Result: ≤ 2 in-app taps from
        // home to active focus on every launch after the first.
        if (!AccessibilityUtils.isAccessibilityServiceEnabled(context)) {
            showAccessibilityRationale = true
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            val granted = androidx.core.content.ContextCompat
                .checkSelfPermission(context, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(perm)
                return
            }
        }

        viewModel.activate(onActivated = onActivateFocus)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.home_greeting, profile.displayName),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(24.dp))
                StatusBadge(isActive = isActive)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.home_subtitle_off),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            Button(
                onClick = ::handleActivateTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_activate_button),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    if (showAccessibilityRationale) {
        AccessibilityRationaleDialog(
            onConfirm = {
                showAccessibilityRationale = false
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            onDismiss = { showAccessibilityRationale = false },
        )
    }
}

/**
 * Pill badge showing focus status. Green container + green-ink dot when off; reuses the
 * error tint when somehow visible during active mode (defensive — Home shouldn't be
 * visible while focus is active because MainActivity bounces to FocusActivity).
 */
@Composable
private fun StatusBadge(isActive: Boolean) {
    val container = if (isActive) MaterialTheme.colorScheme.error else FocusOffGreenContainer
    val ink = if (isActive) MaterialTheme.colorScheme.onError else FocusOffGreen
    val label = if (isActive) {
        stringResource(R.string.home_status_on)
    } else {
        stringResource(R.string.home_status_off)
    }

    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(ink, CircleShape),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = label,
                color = ink,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AccessibilityRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permission_dialog_title)) },
        text = { Text(stringResource(R.string.permission_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.permission_dialog_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.permission_dialog_cancel))
            }
        },
    )
}
