package com.arhomedesign.auth.ui

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arhomedesign.core.ui.components.ErrorBanner
import com.arhomedesign.core.ui.components.PremiumScreenBackground

private const val MinPasswordLength = 6

@Composable
fun AuthScreen(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onClearError: () -> Unit = {},
    onContinueAsGuest: () -> Unit = {},
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.secondary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = MaterialTheme.colorScheme.secondary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.secondary,
        focusedLeadingIconColor = MaterialTheme.colorScheme.secondary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val emailOk = email.trim().isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordOk = password.length >= MinPasswordLength
    val signInReady = emailOk && password.isNotBlank() && !state.isLoading
    val signUpReady = emailOk && passwordOk && password == confirmPassword && !state.isLoading

    val primaryLabel = when {
        state.isLoading && state.inFlight == AuthInFlight.SignUp -> "Creating your account…"
        state.isLoading && state.inFlight == AuthInFlight.SignIn -> "Signing you in…"
        isSignUp -> "Create account"
        else -> "Sign in"
    }

    PremiumScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            BrandHeader()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = if (isSignUp) "New here?" else "Welcome back",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isSignUp) {
                            "Create an account to save rooms, materials, and estimates."
                        } else {
                            "Sign in to open your saved projects and design studio."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AuthModeSegmentedControl(
                        isSignUp = isSignUp,
                        onSelectSignIn = {
                            if (isSignUp) {
                                isSignUp = false
                                onClearError()
                            }
                        },
                        onSelectSignUp = {
                            if (!isSignUp) {
                                isSignUp = true
                                onClearError()
                            }
                        },
                        enabled = !state.isLoading
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (state.error != null) onClearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = MaterialTheme.shapes.medium,
                        colors = fieldColors,
                        enabled = !state.isLoading
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (state.error != null) onClearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (signInReady) onLogin(email.trim(), password)
                            }
                        ),
                        supportingText = if (isSignUp) {
                            {
                                Text(
                                    "At least $MinPasswordLength characters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (password.isNotEmpty() && !passwordOk) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        } else {
                            null
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = fieldColors,
                        enabled = !state.isLoading
                    )

                    AnimatedVisibility(
                        visible = isSignUp,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                if (state.error != null) onClearError()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm password") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                    Icon(
                                        imageVector = if (confirmVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (confirmVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (confirmVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            supportingText = {
                                if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                    Text(
                                        "Passwords do not match",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                            shape = MaterialTheme.shapes.medium,
                            colors = fieldColors,
                            enabled = !state.isLoading
                        )
                    }

                    state.error?.let { ErrorBanner(message = it) }

                    Button(
                        onClick = {
                            if (isSignUp) onRegister(email.trim(), password)
                            else onLogin(email.trim(), password)
                        },
                        enabled = if (isSignUp) signUpReady else signInReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 6.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        if (state.isLoading) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    androidx.compose.material3.TextButton(
                        onClick = onContinueAsGuest,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Continue as guest")
                    }

                    Text(
                        text = "Secure sign-in powered by Firebase. Your studio data stays tied to this account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Text(
                text = "Tip: use a strong password. You can reset it later from Firebase Console if needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun BrandHeader() {
    val accent = MaterialTheme.colorScheme.secondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AR",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(
            text = "AR Home Design",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Visualize interiors before you build",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
private fun AuthModeSegmentedControl(
    isSignUp: Boolean,
    onSelectSignIn: () -> Unit,
    onSelectSignUp: () -> Unit,
    enabled: Boolean
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegmentChip(
                text = "Sign in",
                selected = !isSignUp,
                onClick = onSelectSignIn,
                modifier = Modifier.weight(1f),
                enabled = enabled
            )
            SegmentChip(
                text = "Sign up",
                selected = isSignUp,
                onClick = onSelectSignUp,
                modifier = Modifier.weight(1f),
                enabled = enabled
            )
        }
    }
}

@Composable
private fun SegmentChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = bg,
        shadowElevation = if (selected) 3.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            textAlign = TextAlign.Center
        )
    }
}
