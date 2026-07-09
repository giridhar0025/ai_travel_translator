package com.divami.traveltranslator.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.AppBottomSheet
import com.divami.traveltranslator.designsystem.components.CoralButton
import com.divami.traveltranslator.designsystem.components.CoralTextField
import com.divami.traveltranslator.designsystem.components.CoralTextFieldKind

// #12 — email format validation helper (shared with other screens via top-level or duplicated).
private fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

// #13 — password strength score: 0–3.
private fun passwordStrengthScore(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val colors = AppTheme.colors
    val systemInsets = WindowInsets.systemBars.asPaddingValues()
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.state.collectAsState()

    LaunchedAuthEffects(viewModel, context, onAuthenticated = onAuthenticated)

    // #11 — rememberSaveable for form state.
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val submitting = uiState.submitting

    // #20 — keyboard dismiss.
    val keyboardController = LocalSoftwareKeyboardController.current

    // #14 — focus requesters for field chaining.
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    // #12 — email validity.
    val emailValid = email.isBlank() || email.isValidEmail()
    val formEnabled = name.isNotBlank() && email.isNotBlank() && emailValid && password.length >= 8 && !submitting

    // #26 — bottom sheet state for ToS / Privacy Policy.
    var showTermsSheet by rememberSaveable { mutableStateOf(false) }
    var showPrivacySheet by rememberSaveable { mutableStateOf(false) }

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
            title = "Create Account",
            subtitle = "Join thousands of travelers worldwide",
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = DesignTokens.Spacing.S6),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S4),
        ) {
            // Name field — ImeAction.Next chained to email (#14).
            CoralTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "John Doe",
                label = "Full Name",
                leadingIcon = Icons.Outlined.Person,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { emailFocusRequester.requestFocus() },
                ),
            )

            // Email field — ImeAction.Next chained to password, with error caption (#12, #14).
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
                    focusRequester = emailFocusRequester,
                )
                // #12 — error caption when non-blank and invalid.
                if (email.isNotBlank() && !email.isValidEmail()) {
                    Text(
                        text = "Enter a valid email",
                        color = colors.destructive,
                        fontSize = DesignTokens.Typography.Size.Xs,
                    )
                }
            }

            // Password field with strength meter (#13) and IME Done (#14).
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2)) {
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
                                viewModel.signUpWithEmail(name, email, password)
                            }
                        },
                    ),
                    focusRequester = passwordFocusRequester,
                )

                // #13 — Password strength meter.
                PasswordStrengthMeter(password = password)
            }

            Spacer(Modifier.height(DesignTokens.Spacing.S1))

            // #27 — isLoading; #20 — keyboard dismiss.
            CoralButton(
                text = "Create Account",
                onClick = {
                    keyboardController?.hide()
                    viewModel.signUpWithEmail(name, email, password)
                },
                enabled = formEnabled,
                isLoading = submitting,
            )
        }

        // #26 — Footer with clickable "Terms of Service" and "Privacy Policy".
        // TODO(audit-#26): replace with real hosted URLs (Phase 9)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S6)
                .padding(bottom = DesignTokens.Spacing.S8),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "By signing up, you agree to our ",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Xs,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Terms of Service",
                color = colors.primary,
                fontSize = DesignTokens.Typography.Size.Xs,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showTermsSheet = true },
            )
            Text(
                text = " and ",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Xs,
            )
            Text(
                text = "Privacy Policy",
                color = colors.primary,
                fontSize = DesignTokens.Typography.Size.Xs,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showPrivacySheet = true },
            )
        }
    }

    // #26 — Terms of Service bottom sheet.
    if (showTermsSheet) {
        AppBottomSheet(onDismissRequest = { showTermsSheet = false }) {
            Text(
                text = "Terms of Service",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.S4))
            Text(
                text = "Terms of Service will appear here",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
            )
        }
    }

    // #26 — Privacy Policy bottom sheet.
    if (showPrivacySheet) {
        AppBottomSheet(onDismissRequest = { showPrivacySheet = false }) {
            Text(
                text = "Privacy Policy",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.S4))
            Text(
                text = "Privacy Policy will appear here",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
            )
        }
    }
}

// #13 — Horizontal 3-bar password strength indicator composable.
@Composable
private fun PasswordStrengthMeter(password: String) {
    val colors = AppTheme.colors
    val score = passwordStrengthScore(password)

    val barColors = listOf(
        if (score >= 1) colors.destructive else colors.border,
        if (score >= 2) colors.accent else colors.border,
        if (score >= 3) colors.success else colors.border,
    )
    val label = when {
        password.isEmpty() -> "Must be at least 8 characters"
        score == 1         -> "Weak"
        score == 2         -> "Good"
        else               -> "Strong"
    }
    val labelColor = when {
        password.isEmpty() -> colors.textSecondary
        score == 1         -> colors.destructive
        score == 2         -> colors.accent
        else               -> colors.success
    }

    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
        ) {
            barColors.forEach { barColor ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor),
                )
            }
        }
        Text(
            text = label,
            color = labelColor,
            fontSize = DesignTokens.Typography.Size.Xs,
        )
    }
}
