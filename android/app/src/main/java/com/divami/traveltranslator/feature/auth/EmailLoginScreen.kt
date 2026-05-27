package com.divami.traveltranslator.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.CoralButton
import com.divami.traveltranslator.designsystem.components.CoralTextField
import com.divami.traveltranslator.designsystem.components.CoralTextFieldKind

// #12 — email format validation helper.
private fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

@Composable
fun EmailLoginScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
) {
    val colors = AppTheme.colors
    val systemInsets = WindowInsets.systemBars.asPaddingValues()

    // #11 — rememberSaveable for form state.
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // #27 — isLoading on submit.
    var submitting by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // #20 — keyboard dismiss on submit.
    val keyboardController = LocalSoftwareKeyboardController.current

    // #14 — IME action chaining: focus requester for password field.
    val passwordFocusRequester = remember { FocusRequester() }

    // #12 — email validity for enabled gate.
    val emailValid = email.isBlank() || email.isValidEmail()
    val formEnabled = email.isNotBlank() && emailValid && password.isNotBlank() && !submitting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(
                top = systemInsets.calculateTopPadding(),
                bottom = systemInsets.calculateBottomPadding(),
            ),
    ) {
        AuthHeader(
            onBack = onBack,
            title = "Sign in with Email",
            subtitle = "Enter your credentials to continue",
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = DesignTokens.Spacing.S6),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S4),
        ) {
            // Email field with inline error caption (#12) and IME Next (#14).
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1)) {
                CoralTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "your@email.com",
                    label = "Email",
                    kind = CoralTextFieldKind.Email,
                    leadingIcon = Icons.Outlined.Mail,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() },
                    ),
                )
                // #12 — show error only when non-blank and invalid.
                if (email.isNotBlank() && !email.isValidEmail()) {
                    Text(
                        text = "Enter a valid email",
                        color = colors.destructive,
                        fontSize = DesignTokens.Typography.Size.Xs,
                    )
                }
            }

            // Password field with IME Done (#14) and submit on Done.
            CoralTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••",
                label = "Password",
                kind = CoralTextFieldKind.Password,
                leadingIcon = Icons.Outlined.Lock,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (formEnabled) {
                            keyboardController?.hide()
                            scope.launch {
                                submitting = true
                                delay(600)
                                submitting = false
                                onLogin()
                            }
                        }
                    },
                ),
                focusRequester = passwordFocusRequester,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "Forgot password?",
                    color = colors.primary,
                    fontSize = DesignTokens.Typography.Size.Sm,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable(onClick = onForgotPassword)
                        .padding(DesignTokens.Spacing.S2),
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.S1))

            // #27 — isLoading passed to CoralButton; enabled gate includes !submitting.
            CoralButton(
                text = "Sign In",
                onClick = {
                    // #20 — dismiss keyboard before invoking callback.
                    keyboardController?.hide()
                    scope.launch {
                        submitting = true
                        delay(600)
                        submitting = false
                        onLogin()
                    }
                },
                enabled = formEnabled,
                isLoading = submitting,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S6)
                .padding(bottom = DesignTokens.Spacing.S8),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Don't have an account? ",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Sm,
            )
            Text(
                text = "Sign up",
                color = colors.primary,
                fontSize = DesignTokens.Typography.Size.Sm,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onSignUp),
            )
        }
    }
}
