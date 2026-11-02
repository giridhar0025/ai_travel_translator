package com.divami.traveltranslator.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.divami.traveltranslator.LocalImeVisible
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.components.AppTab
import com.divami.traveltranslator.designsystem.components.BottomNavBar
import com.divami.traveltranslator.feature.chat.ChatScreen
import com.divami.traveltranslator.feature.chat.ChatViewModel
import com.divami.traveltranslator.feature.history.ConversationHistoryScreen
import com.divami.traveltranslator.feature.phrases.QuickPhrasesScreen
import com.divami.traveltranslator.feature.settings.SettingsScreen

@Composable
fun MainScaffold(
    onLogout: () -> Unit,
) {
    val colors = AppTheme.colors
    var selected by rememberSaveable { mutableStateOf(AppTab.Chat) }
    val systemInsets = WindowInsets.systemBars.asPaddingValues()
    // Read at the root so reading IME state can't feed back into imePadding.
    val imeVisible = LocalImeVisible.current
    val chatViewModel: ChatViewModel = hiltViewModel()
    val chatState by chatViewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Scaffold(
            containerColor = colors.background,
            // Insets are applied once below. Letting Scaffold apply its default
            // contentWindowInsets as well caused the content to be padded twice.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // Hide the nav bar while typing so the chat input lands flush on
                // the keyboard (the root imePadding lifts the content).
                if (!imeVisible) {
                    BottomNavBar(selected = selected, onSelect = { selected = it })
                }
            },
        ) { padding ->
            // One source of truth for the window insets: status bar on top and
            // the measured bottom bar (which already includes the nav-bar inset)
            // at the bottom. Screens receive zero content padding so they cannot
            // add a second copy.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(
                        top = systemInsets.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
            ) {
                val screenPadding = PaddingValues(0.dp)
                when (selected) {
                    AppTab.Chat -> ChatScreen(
                        viewModel = chatViewModel,
                        contentPadding = screenPadding,
                    )
                    AppTab.Phrases -> QuickPhrasesScreen(
                        targetLanguageCode = chatState.targetLanguage.code,
                        onPhraseSelect = { phrase ->
                            chatViewModel.onInputChange(phrase)
                            selected = AppTab.Chat
                        },
                        contentPadding = screenPadding,
                    )
                    AppTab.History -> ConversationHistoryScreen(
                        onConversationOpen = { conversationId ->
                            chatViewModel.loadConversation(conversationId)
                            selected = AppTab.Chat
                        },
                        contentPadding = screenPadding,
                    )
                    AppTab.Settings -> SettingsScreen(
                        onLogout = onLogout,
                        contentPadding = screenPadding,
                    )
                }
            }
        }
    }
}
