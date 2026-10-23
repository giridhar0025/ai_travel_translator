package com.divami.traveltranslator.feature.settings

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.BuildConfig
import com.divami.traveltranslator.R
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.AppBottomSheet
import com.divami.traveltranslator.designsystem.components.AppLanguage
import com.divami.traveltranslator.designsystem.components.CoralButton
import com.divami.traveltranslator.designsystem.components.CoralButtonVariant
import com.divami.traveltranslator.designsystem.components.LanguageFlag
import com.divami.traveltranslator.designsystem.components.RadioCard
import com.divami.traveltranslator.designsystem.minimumTouchTarget
import com.divami.traveltranslator.domain.model.PlaybackSpeed

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

    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    var showSpeedSheet by rememberSaveable { mutableStateOf(false) }
    var showPrivacySheet by rememberSaveable { mutableStateOf(false) }
    var showAccessibilitySheet by rememberSaveable { mutableStateOf(false) }
    var showOfflineSheet by rememberSaveable { mutableStateOf(false) }
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
                text = stringResource(R.string.settings_title),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xl,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + DesignTokens.Spacing.S6),
        ) {
            SettingsSection(title = stringResource(R.string.settings_account)) {
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
                        text = if (uiState.isGuest) stringResource(R.string.settings_guest_sync)
                        else uiState.email ?: stringResource(R.string.settings_signed_in),
                        color = colors.textSecondary,
                        fontSize = DesignTokens.Typography.Size.Sm,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.settings_translation)) {
                SettingsRow(
                    icon = Icons.Outlined.Public,
                    title = stringResource(R.string.settings_default_language),
                    subtitle = defaultLanguage,
                    onClick = { showLanguageSheet = true },
                )
                Divider()
                SettingsRow(
                    icon = Icons.Outlined.Speed,
                    title = stringResource(R.string.settings_playback_speed),
                    subtitle = playbackSpeed,
                    onClick = { showSpeedSheet = true },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_offline)) {
                SettingsRow(
                    icon = Icons.Outlined.Download,
                    title = stringResource(R.string.settings_offline_packs),
                    subtitle = if (uiState.downloadedPacks.isEmpty()) {
                        stringResource(R.string.settings_offline_packs_subtitle)
                    } else {
                        stringResource(R.string.settings_offline_ready) +
                            " · ${uiState.downloadedPacks.size}"
                    },
                    onClick = { showOfflineSheet = true },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_accessibility)) {
                SettingsRow(
                    icon = Icons.Outlined.Accessibility,
                    title = stringResource(R.string.settings_accessibility),
                    subtitle = stringResource(R.string.settings_text_size),
                    onClick = { showAccessibilitySheet = true },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_privacy)) {
                SettingsRow(
                    icon = Icons.Outlined.Security,
                    title = stringResource(R.string.settings_privacy),
                    subtitle = stringResource(R.string.settings_privacy_subtitle),
                    onClick = { showPrivacySheet = true },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_sign_out)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumTouchTarget()
                        .heightIn(min = DesignTokens.TouchTarget.Min)
                        .clickable(onClick = { showLogoutConfirm = true })
                        .semantics { role = Role.Button }
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
                        text = stringResource(R.string.settings_log_out),
                        color = colors.destructive,
                        fontSize = DesignTokens.Typography.Size.Base,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Xs,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showLanguageSheet) {
        AppBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Text(
                text = stringResource(R.string.settings_default_language),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S4))
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
            ) {
                for (lang in AppLanguage.Supported) {
                    RadioCard(
                        title = lang.pickerLabel,
                        isSelected = uiState.defaultLanguageCode == lang.code,
                        onClick = {
                            viewModel.setDefaultLanguage(lang.code)
                            showLanguageSheet = false
                        },
                    )
                }
            }
        }
    }

    if (showSpeedSheet) {
        AppBottomSheet(onDismissRequest = { showSpeedSheet = false }) {
            Text(
                text = stringResource(R.string.settings_playback_speed),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S4))
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3)) {
                for (speed in PlaybackSpeed.entries) {
                    RadioCard(
                        title = playbackSpeedLabel(speed),
                        isSelected = uiState.playbackSpeed == speed,
                        onClick = {
                            viewModel.setPlaybackSpeed(speed)
                            showSpeedSheet = false
                        },
                    )
                }
            }
        }
    }

    if (showOfflineSheet) {
        AppBottomSheet(
            onDismissRequest = { showOfflineSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Text(
                text = stringResource(R.string.settings_offline_packs),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.settings_offline_packs_subtitle),
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Sm,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
            )
            uiState.packError?.let { error ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DesignTokens.Spacing.S3)
                        .background(colors.destructive.copy(alpha = 0.12f))
                        .padding(horizontal = DesignTokens.Spacing.S3, vertical = DesignTokens.Spacing.S2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = error,
                        color = colors.destructive,
                        fontSize = DesignTokens.Typography.Size.Sm,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::clearPackError) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 360.dp)
                    .padding(top = DesignTokens.Spacing.S3),
            ) {
                for (lang in AppLanguage.Supported) {
                    OfflinePackRow(
                        language = lang,
                        downloaded = uiState.downloadedPacks.contains(lang.code),
                        downloading = uiState.downloadingPacks.contains(lang.code),
                        onDownload = { viewModel.downloadPack(lang.code) },
                        onDelete = { viewModel.deletePack(lang.code) },
                    )
                }
            }
        }
    }

    if (showAccessibilitySheet) {
        AppBottomSheet(onDismissRequest = { showAccessibilitySheet = false }) {
            Text(
                text = stringResource(R.string.settings_accessibility),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            AccessibilityItem(
                title = stringResource(R.string.settings_text_size),
                body = stringResource(R.string.settings_text_size_body),
            )
            AccessibilityItem(
                title = stringResource(R.string.settings_reduce_motion),
                body = stringResource(R.string.settings_reduce_motion_body),
            )
            Spacer(Modifier.height(DesignTokens.Spacing.S4))
            CoralButton(
                text = stringResource(R.string.settings_got_it),
                onClick = { showAccessibilitySheet = false },
            )
        }
    }

    if (showPrivacySheet) {
        AppBottomSheet(onDismissRequest = { showPrivacySheet = false }) {
            Text(
                text = stringResource(R.string.settings_privacy),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S4))
            Text(
                text = stringResource(R.string.settings_privacy_body),
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))
            CoralButton(
                text = stringResource(R.string.settings_got_it),
                onClick = { showPrivacySheet = false },
            )
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_log_out_title),
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_log_out_body),
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_log_out_confirm),
                        color = colors.destructive,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(
                        text = stringResource(R.string.common_cancel),
                        color = colors.textSecondary,
                    )
                }
            },
            containerColor = colors.card,
        )
    }
}

@Composable
private fun OfflinePackRow(
    language: AppLanguage,
    downloaded: Boolean,
    downloading: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumTouchTarget()
            .heightIn(min = DesignTokens.TouchTarget.Min)
            .padding(vertical = DesignTokens.Spacing.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
    ) {
        LanguageFlag(language = language, compact = true)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.pickerLabel,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Base,
            )
            if (downloaded) {
                Text(
                    text = stringResource(R.string.settings_offline_ready),
                    color = colors.success,
                    fontSize = DesignTokens.Typography.Size.Xs,
                )
            } else if (downloading) {
                Text(
                    text = stringResource(R.string.settings_offline_downloading),
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Xs,
                )
            }
        }
        when {
            downloading -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = colors.primary,
            )
            downloaded -> TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.settings_offline_delete),
                    color = colors.destructive,
                )
            }
            else -> TextButton(onClick = onDownload) {
                Text(
                    text = stringResource(R.string.settings_offline_download),
                    color = colors.primary,
                )
            }
        }
    }
}

@Composable
private fun AccessibilityItem(title: String, body: String) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DesignTokens.Spacing.S3),
    ) {
        Text(
            text = title,
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Base,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = body,
            color = colors.textSecondary,
            fontSize = DesignTokens.Typography.Size.Sm,
            modifier = Modifier.padding(top = DesignTokens.Spacing.S1),
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
                    color = colors.borderStrong,
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
            .minimumTouchTarget()
            .heightIn(min = DesignTokens.TouchTarget.Min)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
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
private fun playbackSpeedLabel(speed: PlaybackSpeed): String = when (speed) {
    PlaybackSpeed.Normal -> stringResource(R.string.settings_normal)
    PlaybackSpeed.Slow -> stringResource(R.string.settings_slow)
    PlaybackSpeed.VerySlow -> stringResource(R.string.settings_very_slow)
}
