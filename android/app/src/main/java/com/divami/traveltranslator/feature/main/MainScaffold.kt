package com.divami.traveltranslator.feature.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import com.divami.traveltranslator.LocalImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.AppBottomSheet
import com.divami.traveltranslator.designsystem.components.AppTab
import com.divami.traveltranslator.designsystem.components.BottomNavBar
import com.divami.traveltranslator.designsystem.components.CoralButton
import com.divami.traveltranslator.designsystem.components.CoralButtonVariant
import com.divami.traveltranslator.feature.chat.ChatScreen
import com.divami.traveltranslator.feature.chat.ChatViewModel
import com.divami.traveltranslator.feature.history.ConversationHistoryScreen
import com.divami.traveltranslator.feature.phrases.QuickPhrasesScreen
import com.divami.traveltranslator.feature.settings.SettingsScreen
import com.divami.traveltranslator.feature.voice.VoiceInputScreen

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    onLogout: () -> Unit,
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(AppTab.Chat) }
    var showVoiceInput by rememberSaveable { mutableStateOf(false) }
    var showMicDeniedSheet by rememberSaveable { mutableStateOf(false) }
    val systemInsets = WindowInsets.systemBars.asPaddingValues()
    // IME visibility is read once at the root (MainActivity) and provided here,
    // so reading it can't feed back into the root imePadding.
    val imeVisible = LocalImeVisible.current
    val chatViewModel: ChatViewModel = hiltViewModel()
    val chatState by chatViewModel.state.collectAsState()

    fun micPermissionGranted() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) showVoiceInput = true else showMicDeniedSheet = true
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Scaffold(
            containerColor = colors.background,
            bottomBar = {
                // Hide the bottom nav while typing so the chat input lands flush on
                // the keyboard (the root imePadding lifts the content).
                if (!imeVisible) {
                    BottomNavBar(selected = selected, onSelect = { selected = it })
                }
            },
        ) { padding ->
            val contentPadding = PaddingValues(
                top = systemInsets.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            )
            Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
                when (selected) {
                    AppTab.Chat -> ChatScreen(
                        viewModel = chatViewModel,
                        contentPadding = contentPadding,
                    )
                    AppTab.Phrases -> QuickPhrasesScreen(
                        onPhraseSelect = { phrase ->
                            chatViewModel.onInputChange(phrase)
                            selected = AppTab.Chat
                        },
                        contentPadding = contentPadding,
                    )
                    AppTab.History -> ConversationHistoryScreen(
                        onConversationOpen = { /* phase 7 wires real persistence */
                            selected = AppTab.Chat
                        },
                        contentPadding = contentPadding,
                    )
                    AppTab.Settings -> SettingsScreen(
                        onLogout = onLogout,
                        contentPadding = contentPadding,
                    )
                }
            }
        }

        if (showVoiceInput) {
            VoiceInputScreen(
                // Hint STT with the current source language for better accuracy.
                languageCode = chatState.sourceLanguage.code,
                onBack = { showVoiceInput = false },
                onTranslate = { transcript ->
                    chatViewModel.onInputChange(transcript)
                    selected = AppTab.Chat
                    showVoiceInput = false
                },
            )
        }

        if (showMicDeniedSheet) {
            AppBottomSheet(onDismissRequest = { showMicDeniedSheet = false }) {
                Text(
                    text = "Microphone access blocked",
                    color = colors.foreground,
                    fontSize = DesignTokens.Typography.Size.Lg,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.S3))
                Text(
                    text = "We need microphone access to record your voice for translation.",
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Base,
                )
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(DesignTokens.Spacing.S3),
                ) {
                    CoralButton(
                        text = "Open Settings",
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                            context.startActivity(intent)
                            showMicDeniedSheet = false
                        },
                    )
                    CoralButton(
                        text = "Not now",
                        variant = CoralButtonVariant.Secondary,
                        onClick = { showMicDeniedSheet = false },
                    )
                }
            }
        }
    }
}
