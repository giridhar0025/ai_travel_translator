package com.divami.traveltranslator.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val colors = AppTheme.colors
    val systemInsets = WindowInsets.systemBars.asPaddingValues()
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.state.collectAsState()

    // #11 — rememberSaveable for form state (also preserves success state across rotation).
    var email by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(false) }

    // The reset email is sent asynchronously; flip to the success screen only
    // when the backend confirms via the effect.
    LaunchedAuthEffects(viewModel, context, onPasswordResetSent = { sent = true })

    val submitting = uiState.submitting

    // #20 — keyboard dismiss.
    val keyboardController = LocalSoftwareKeyboardController.current

    // #12 — email validity for enabled gate.
    val emailValid = email.isBlank() || email.isValidEmail()
    val formEnabled = email.isNotBlank() && emailValid && !submitting

    if (sent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(
                    top = systemInsets.calculateTopPadding(),
                    bottom = systemInsets.calculateBottomPadding(),
                ),
        ) {
            // #32 — 48 dp tap target: outer Box 48×48, 24 dp icon centred inside.
            Box(
                modifier = Modifier
                    .padding(DesignTokens.Spacing.S4)
                    .size(DesignTokens.TouchTarget.Min)
                    .clip(RoundedCornerShape(DesignTokens.Radius.Md))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.foreground,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.S6),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colors.success.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = colors.success,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(DesignTokens.Spacing.S6))
                Text(
                    text = "Check Your Email",
                    color = colors.foreground,
                    fontSize = DesignTokens.Typography.Size.Xxl,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(DesignTokens.Spacing.S3))
                Text(
                    text = "We've sent a password reset link to:",
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Base,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(DesignTokens.Spacing.S2))
                Text(
                    text = email,
                    color = colors.foreground,
                    fontSize = DesignTokens.Typography.Size.Base,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(DesignTokens.Spacing.S6))
                CoralButton(text = "Back to Login", onClick = onBack)
            }
        }
        return
    }

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
            title = "Reset Password",
            subtitle = "Enter your email and we'll send you a reset link",
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.S6),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S4),
        ) {
            // #12 — email field with error caption; #14 — ImeAction.Done submits.
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
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (formEnabled) {
                                keyboardController?.hide()
                                viewModel.sendPasswordReset(email)
                            }
                        },
                    ),
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

            Spacer(Modifier.height(DesignTokens.Spacing.S1))

            // #27 — isLoading; #20 — keyboard dismiss.
            CoralButton(
                text = "Send Reset Link",
                onClick = {
                    keyboardController?.hide()
                    viewModel.sendPasswordReset(email)
                },
                enabled = formEnabled,
                isLoading = submitting,
            )
        }
    }
}
