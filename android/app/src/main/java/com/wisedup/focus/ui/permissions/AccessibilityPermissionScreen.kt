package com.wisedup.focus.ui.permissions

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wisedup.focus.R

/**
 * Stand-alone screen for the accessibility-permission rationale + deep-link.
 *
 * In R1 the same rationale is also shown as a dialog from the Home screen (US-R1-05).
 * This screen is the alternative full-screen variant — useful if a future story wants a
 * blocking onboarding step. It is wired up via [com.wisedup.focus.MainActivity] only as a
 * destination if the host requests it; in the default R1 flow we use the dialog form to
 * keep the brief's "≤ 2 in-app taps" path tight.
 *
 * Kept for completeness because the engineering brief lists it explicitly.
 */
@Composable
fun AccessibilityPermissionScreen(
    onPermissionInteractionDone: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.permission_dialog_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.permission_dialog_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    onPermissionInteractionDone()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.permission_dialog_open_settings))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPermissionInteractionDone,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.permission_dialog_cancel))
            }
        }
    }
}
