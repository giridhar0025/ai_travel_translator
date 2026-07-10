package com.divami.traveltranslator.feature.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.divami.traveltranslator.BuildConfig
import com.divami.traveltranslator.domain.model.PlaybackSpeed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.AppBottomSheet
import com.divami.traveltranslator.designsystem.components.CoralButton
import com.divami.traveltranslator.designsystem.components.CoralButtonVariant
import com.divami.traveltranslator.designsystem.components.RadioCard

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val colors = AppTheme.colors
    val uiState by viewModel.state.collectAsState()

    val defaultLanguage = uiState.defaultLanguageName
    val playbackSpeed = playbackSpeedLabel(uiState.playbackSpeed)
    val offlineMode = uiState.offlineEnabled

    // Sheet visibility state — #7 language, #9 speed, #8 privacy, #9 logout confirm
    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    var showSpeedSheet by rememberSaveable { mutableStateOf(false) }
    var showPrivacySheet by rememberSaveable { mutableStateOf(false) }
    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card)
                .padding(horizontal = DesignTokens.Spacing.S6)
                .padding(top = DesignTokens.Spacing.S6, bottom = DesignTokens.Spacing.S4),
        ) {
            Text(
                text = "Settings",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xl,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + DesignTokens.Spacing.S6),
        ) {

            // ── #44 Account header card ─────────────────────────────────────
            SettingsSection(title = "Account") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DesignTokens.Spacing.S6),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(colors.primaryLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Text(
                        text = uiState.displayName,
                        color = colors.foreground,
                        fontSize = DesignTokens.Typography.Size.Base,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = if (uiState.isGuest) "Sign in to sync your settings"
                        else uiState.email ?: "Signed in",
                        color = colors.textSecondary,
                        fontSize = DesignTokens.Typography.Size.Sm,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ── Translation section ─────────────────────────────────────────
            SettingsSection(title = "Translation") {
                SettingsRow(
                    icon = Icons.Outlined.Public,
                    title = "Default Language",
                    subtitle = defaultLanguage,
                    onClick = { showLanguageSheet = true },   // #7
                )
                Divider()
                SettingsRow(
                    icon = Icons.Outlined.Speed,
                    title = "Playback Speed",
                    subtitle = playbackSpeed,
                    onClick = { showSpeedSheet = true },      // #9
                )
            }

            SettingsSection(title = "Offline") {
                SettingsToggleRow(
                    icon = Icons.Outlined.Download,
                    title = "Download Phrases",
                    subtitle = "Use offline mode",
                    enabled = true,
                    checked = offlineMode,
                    onChange = { viewModel.setOffline(it) },
                )
            }

            SettingsSection(title = "Privacy & Security") {
                SettingsRow(
                    icon = Icons.Outlined.Security,
                    title = "Privacy Settings",
                    subtitle = "Manage your data",
                    onClick = { showPrivacySheet = true },    // #8
                )
            }

            // ── Log Out (with confirmation — #9 logout confirm) ─────────────
            SettingsSection(title = "Sign Out") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { showLogoutConfirm = true })
                        .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(DesignTokens.Radius.Md))
                            .background(colors.destructive.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = colors.destructive,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "Log Out",
                        color = colors.destructive,
                        fontSize = DesignTokens.Typography.Size.Base,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // ── #44 Version footer ──────────────────────────────────────────
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))
            Text(
                text = "Travel Translator AI · v${BuildConfig.VERSION_NAME}",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Xs,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // ── #7 Default Language sheet ───────────────────────────────────────────
    if (showLanguageSheet) {
        AppBottomSheet(onDismissRequest = { showLanguageSheet = false }) {
            Text(
                text = "Default Language",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S4))
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
            ) {
                for (lang in com.divami.traveltranslator.designsystem.components.AppLanguage.Supported) {
                    RadioCard(
                        title = lang.displayName,
                        isSelected = defaultLanguage == lang.displayName,
                        onClick = {
                            viewModel.setDefaultLanguage(lang.code)
                            showLanguageSheet = false
                        },
                    )
                }
            }
        }
    }

    // ── #9 Playback Speed sheet ─────────────────────────────────────────────
    if (showSpeedSheet) {
        val speeds = listOf("Normal", "Slow", "Very Slow")
        AppBottomSheet(onDismissRequest = { showSpeedSheet = false }) {
            Text(
                text = "Playback Speed",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S4))
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3)) {
                for (speed in speeds) {
                    RadioCard(
                        title = speed,
                        isSelected = playbackSpeed == speed,
                        onClick = {
                            viewModel.setPlaybackSpeed(playbackSpeedFor(speed))
                            showSpeedSheet = false
                        },
                    )
                }
            }
        }
    }

    // ── #8 Privacy Settings sheet ───────────────────────────────────────────
    if (showPrivacySheet) {
        AppBottomSheet(onDismissRequest = { showPrivacySheet = false }) {
            Text(
                text = "Privacy & Security",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S4))
            Text(
                text = "When offline mode is enabled, all translation processing happens on-device — no audio or text is sent to external servers.\n\nNo personal data is shared with third parties. All conversation history is stored locally on your device and can be cleared at any time.",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))
            CoralButton(
                text = "Got it",
                onClick = { showPrivacySheet = false },
            )
        }
    }

    // ── #9 Logout confirmation dialog ───────────────────────────────────────
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = {
                Text(
                    text = "Log out?",
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(
                    text = "You'll need to sign in again to access your account.",
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogout()
                }) {
                    Text(
                        text = "Log out",
                        color = colors.destructive,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(
                        text = "Cancel",
                        color = colors.textSecondary,
                    )
                }
            },
            containerColor = colors.card,
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.S6)
            .padding(top = DesignTokens.Spacing.S6),
    ) {
        Text(
            text = title.uppercase(),
            color = colors.textSecondary,
            fontSize = DesignTokens.Typography.Size.Xs,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = DesignTokens.Spacing.S3),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
                .background(colors.card)
                .border(
                    width = 1.dp,
                    color = colors.border,
                    shape = RoundedCornerShape(DesignTokens.Radius.Lg),
                ),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
    ) {
        IconBadge(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Base,
                fontWeight = FontWeight.Medium,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Sm,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    enabled: Boolean,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
    ) {
        IconBadge(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Base,
                fontWeight = FontWeight.Medium,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Sm,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
                )
            }
        }
        AppSwitch(checked = checked, enabled = enabled, onChange = onChange)
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(DesignTokens.Radius.Md))
            .background(colors.primaryLight),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun Divider() {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border),
    )
}

@Composable
private fun AppSwitch(
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = AppTheme.colors
    val trackOn = colors.primary
    val trackOff = colors.muted.copy(alpha = 1f)
    val track by animateColorAsState(if (checked) trackOn else trackOff, label = "switchTrack")
    val thumbOffset by animateDpAsState(if (checked) 22.dp else 4.dp, label = "switchThumb")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.5f, label = "switchAlpha")

    Box(
        modifier = Modifier
            .width(50.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(track.copy(alpha = alpha))
            .clickable(enabled = enabled) { onChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = thumbOffset, y = 4.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun animateColorAsState(target: Color, label: String) =
    androidx.compose.animation.animateColorAsState(targetValue = target, label = label)

// ── Display <-> domain mappings ─────────────────────────────────────────────

private fun playbackSpeedLabel(speed: PlaybackSpeed): String = when (speed) {
    PlaybackSpeed.Normal -> "Normal"
    PlaybackSpeed.Slow -> "Slow"
    PlaybackSpeed.VerySlow -> "Very Slow"
}

private fun playbackSpeedFor(label: String): PlaybackSpeed = when (label) {
    "Slow" -> PlaybackSpeed.Slow
    "Very Slow" -> PlaybackSpeed.VerySlow
    else -> PlaybackSpeed.Normal
}
