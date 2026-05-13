package com.wizedup.focus.ui.registration

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wizedup.focus.R
import com.wizedup.focus.data.StudentProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolRegistrationScreen(
    profile: StudentProfile,
    viewModel: SchoolRegistrationViewModel,
) {
    val code: String by viewModel.code.collectAsStateWithLifecycle()
    val isSubmitting: Boolean by viewModel.isSubmitting.collectAsStateWithLifecycle()
    var errorText: String? by remember { mutableStateOf(null) }

    val continueEnabled = viewModel.isContinueEnabled() && !isSubmitting

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
                text = stringResource(R.string.school_registration_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.school_registration_subtitle, profile.displayName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.school_registration_transparency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = code,
                onValueChange = viewModel::onCodeChange,
                label = { Text(stringResource(R.string.school_registration_code_label)) },
                placeholder = { Text(stringResource(R.string.school_registration_code_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                supportingText = {
                    Text(
                        text = stringResource(
                            R.string.school_registration_code_counter,
                            code.length,
                            viewModel.maxLength,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            errorText?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    errorText = null
                    viewModel.submit(
                        onSuccess = { errorText = null },
                        onError = { errorText = it },
                    )
                },
                enabled = continueEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.school_registration_continue))
            }
        }
    }
}
