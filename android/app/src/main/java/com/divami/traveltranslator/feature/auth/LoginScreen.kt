package com.divami.traveltranslator.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.AppBottomSheet
import com.divami.traveltranslator.designsystem.components.CoralButton
import com.divami.traveltranslator.designsystem.components.CoralButtonVariant

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onEmailLogin: () -> Unit,
    viewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val colors = AppTheme.colors
    val systemInsets = WindowInsets.systemBars.asPaddingValues()
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.state.collectAsState()

    // Route auth outcomes: success navigates forward, errors surface as a toast.
    LaunchedAuthEffects(viewModel, context, onAuthenticated)

    val submitting = uiState.submitting

    // #26 — bottom sheet state.
    var showTermsSheet   by rememberSaveable { mutableStateOf(false) }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S6)
                .padding(top = 56.dp, bottom = DesignTokens.Spacing.S8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(DesignTokens.Radius.Xl))
                    .background(
                        Brush.linearGradient(
                            listOf(DesignTokens.Brand.Coral, DesignTokens.Brand.CoralDark)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = "Welcome to Travel Translator",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xxl,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S4),
            )
            Text(
                text = "Connect with locals anywhere in the world",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S2),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = DesignTokens.Spacing.S6),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
        ) {
            // #27 — Email login button (primary CTA — top position for hierarchy).
            CoralButton(
                text = "Continue with Email",
                onClick = onEmailLogin,
                variant = CoralButtonVariant.Primary,
                leadingIcon = Icons.Outlined.Mail,
                enabled = !submitting,
            )

            // Divider row between primary CTA and social alternatives.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.border))
                Text(
                    "or",
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Xs,
                    modifier = Modifier.padding(horizontal = DesignTokens.Spacing.S3),
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.border))
            }

            // #27 — Google login button with 600 ms loading feedback (secondary).
            CoralButton(
                text = "Continue with Google",
                onClick = { viewModel.signInWithGoogle(context) },
                variant = CoralButtonVariant.Secondary,
                leadingIcon = Icons.Outlined.Public,
                isLoading = submitting,
                enabled = !submitting,
            )
            // Apple sign-in needs a server-side OAuth web flow on Android (no
            // native SDK), so it's out of scope here — inform the user rather
            // than pretend it works.
            BlackPillButton(
                text = "Continue with Apple",
                onClick = {
                    android.widget.Toast.makeText(
                        context,
                        "Apple sign-in isn't available on Android yet — use Email or Google.",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                },
                icon = Icons.Outlined.PhoneIphone,
                isLoading = false,
                enabled = !submitting,
            )

            Spacer(Modifier.height(DesignTokens.Spacing.S2))

            CoralButton(
                text = "Skip for now",
                onClick = { viewModel.continueAsGuest() },
                variant = CoralButtonVariant.Text,
                enabled = !submitting,
                modifier = Modifier.align(Alignment.CenterHorizontally),
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
                text = "By continuing, you agree to our ",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Xs,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Terms of Service",
                color = colors.primary,
                fontSize = DesignTokens.Typography.Size.Xs,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(role = Role.Button) { showTermsSheet = true },
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
                modifier = Modifier.clickable(role = Role.Button) { showPrivacySheet = true },
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

@Composable
private fun BlackPillButton(
    text: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (enabled) Color.Black else Color(0xFF444444))
            // Border gives the black pill a visible edge against the dark theme background.
            .border(width = 1.dp, color = colors.border, shape = shape)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S4),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = DesignTokens.Typography.Size.Base,
            fontWeight = FontWeight.Medium,
        )
    }
}
