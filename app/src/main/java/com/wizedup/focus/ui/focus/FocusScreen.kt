package com.wizedup.focus.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wizedup.focus.R
import com.wizedup.focus.ui.theme.FocusActiveOn
import com.wizedup.focus.ui.theme.FocusActiveRed

/**
 * Compose root for the focus lock surface (US-R1-06).
 *
 * - Red full-bleed surface with white ink for contrast (WCAG AA).
 * - Title, display name, MM:SS / HH:MM:SS elapsed timer.
 * - Single Exit Focus button → confirmation dialog (US-R1-10).
 */
@Composable
fun FocusScreen(
    displayName: String,
    elapsedMs: Long,
    onExitConfirmed: () -> Unit,
) {
    var showExitDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusActiveRed),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 32.dp, vertical = 48.dp)),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top stack: title, name, elapsed.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.focus_active_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = FocusActiveOn,
                    fontWeight = FontWeight.Bold,
                )
                if (displayName.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = FocusActiveOn.copy(alpha = 0.85f),
                    )
                }
                Spacer(Modifier.height(48.dp))
                Text(
                    text = formatElapsed(elapsedMs),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light,
                    color = FocusActiveOn,
                )
            }

            // Bottom: Exit Focus button.
            Button(
                onClick = { showExitDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = FocusActiveOn,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = stringResource(R.string.focus_exit_button),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }

    if (showExitDialog) {
        ExitConfirmDialog(
            onDismiss = { showExitDialog = false },
            onConfirm = {
                showExitDialog = false
                onExitConfirmed()
            },
        )
    }
}

@Composable
private fun ExitConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_dialog_title)) },
        text = { Text(stringResource(R.string.exit_dialog_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.exit_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.exit_dialog_dismiss))
            }
        },
    )
}

/**
 * Format elapsed milliseconds. MM:SS for first hour, HH:MM:SS thereafter (US-R1-06 AC).
 */
internal fun formatElapsed(ms: Long): String {
    val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
