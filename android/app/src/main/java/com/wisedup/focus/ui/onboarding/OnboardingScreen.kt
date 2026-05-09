package com.wisedup.focus.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wisedup.focus.R
import com.wisedup.focus.ui.theme.WisedUpTheme

/**
 * One-screen onboarding (US-R1-03).
 *
 * - Title, single text field, primary button.
 * - Continue is disabled when the trimmed input is empty.
 * - Field uses rememberSaveable so backgrounding mid-entry preserves typed text.
 * - No accessibility-permission prompt here — that lives in the activate flow (US-R1-05).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onContinueComplete: () -> Unit = {},
) {
    val name: String by viewModel.name.collectAsState()
    val isSubmitting: Boolean by viewModel.isSubmitting.collectAsState()
    val continueEnabled = name.trim().isNotEmpty() && !isSubmitting

    // rememberSaveable mirrors the VM-held draft so a process death (rare for this flow)
    // doesn't lose the typed text. The VM is the source of truth at runtime.
    var draft: String by rememberSaveable { mutableStateOf(name) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    viewModel.onNameChange(it)
                },
                label = { Text(stringResource(R.string.onboarding_name_label)) },
                placeholder = { Text(stringResource(R.string.onboarding_name_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                supportingText = {
                    Text(
                        text = stringResource(
                            R.string.onboarding_name_counter,
                            draft.length,
                            viewModel.maxLength,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.submit(onSuccess = onContinueComplete)
                },
                enabled = continueEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOnboardingScreen() {
    WisedUpTheme {
        // Preview without a real VM is not meaningful here; the @Preview is a placeholder
        // so Studio's preview pane remains useful for layout iteration.
    }
}
