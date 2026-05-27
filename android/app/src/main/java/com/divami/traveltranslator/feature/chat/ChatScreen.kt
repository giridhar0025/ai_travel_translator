package com.divami.traveltranslator.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.divami.traveltranslator.designsystem.components.AppBottomSheet
import com.divami.traveltranslator.designsystem.components.ToastHost
import com.divami.traveltranslator.designsystem.components.ToastKind
import com.divami.traveltranslator.designsystem.components.ToastModel
import com.divami.traveltranslator.designsystem.components.rememberToastState
import com.divami.traveltranslator.feature.audio.AudioPlayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onVoiceInput: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val colors = AppTheme.colors
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var playerRequest by remember { mutableStateOf<PlayerRequest?>(null) }

    // #6: clipboard + toast
    val clipboardManager = LocalClipboardManager.current
    val toastState = rememberToastState()
    val scope = rememberCoroutineScope()

    // #20: keyboard controller — dismiss on send
    val keyboard = LocalSoftwareKeyboardController.current

    // #21: focus requester for the input field — Edit chip focuses it
    val inputFocus = remember { FocusRequester() }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Outer Box so ToastHost can overlay the full screen
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            LanguageSelectorBar(
                source = state.sourceLanguage,
                target = state.targetLanguage,
                onSourceChange = viewModel::onSourceLanguageChange,
                onTargetChange = viewModel::onTargetLanguageChange,
                onSwap = viewModel::swapLanguages,
            )

            // #16: empty state vs message list
            if (state.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S4),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(colors.primaryLight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                        Text(
                            text = "Translate anything",
                            color = colors.foreground,
                            fontSize = DesignTokens.Typography.Size.Lg,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Type or tap the mic to begin",
                            color = colors.textSecondary,
                            fontSize = DesignTokens.Typography.Size.Base,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S4),
                    contentPadding = PaddingValues(vertical = DesignTokens.Spacing.S6),
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            flipped = state.flippedIds.contains(message.id),
                            onFlip = { viewModel.toggleFlip(message.id) },
                            onPlay = { playerRequest = PlayerRequest(message, slow = false) },
                            onSlow = { playerRequest = PlayerRequest(message, slow = true) },
                            // #21: Edit chip sets text, focuses input, shows keyboard
                            onEdit = {
                                viewModel.onInputChange(message.originalText)
                                inputFocus.requestFocus()
                                keyboard?.show()
                            },
                            // #6: Copy chip writes translatedText to clipboard and shows a toast
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(message.translatedText))
                                scope.launch {
                                    toastState.show(ToastModel("Copied to clipboard", ToastKind.Success))
                                }
                            },
                        )
                    }
                }
            }

            // #17: inline error chip above the input area
            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                state.error?.let { errorMsg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.destructive.copy(alpha = 0.12f))
                            .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = errorMsg,
                            color = colors.destructive,
                            fontSize = DesignTokens.Typography.Size.Sm,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.dismissError() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Dismiss error",
                                tint = colors.destructive,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            ChatInputArea(
                input = state.input,
                context = state.contextMode,
                onContextChange = viewModel::onContextChange,
                onInputChange = viewModel::onInputChange,
                onVoiceInput = onVoiceInput,
                // #20: hide keyboard before delegating to ViewModel
                onSend = {
                    keyboard?.hide()
                    viewModel.onSend()
                },
                bottomPadding = contentPadding.calculateBottomPadding(),
                inputFocus = inputFocus,
            )
        }

        // #6: ToastHost overlays full screen
        ToastHost(state = toastState)
    }

    playerRequest?.let { req ->
        AppBottomSheet(onDismissRequest = { playerRequest = null }) {
            AudioPlayer(
                text = req.message.translatedText,
                pronunciation = req.message.pronunciation,
                initialSlow = req.slow,
                initialPlay = req.slow,
            )
        }
    }
}

private data class PlayerRequest(val message: ChatMessage, val slow: Boolean)

@Composable
private fun ChatInputArea(
    input: String,
    context: ContextMode,
    onContextChange: (ContextMode) -> Unit,
    onInputChange: (String) -> Unit,
    onVoiceInput: () -> Unit,
    onSend: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    inputFocus: FocusRequester, // #21: passed down so InputBox can attach focusRequester
) {
    val colors = AppTheme.colors
    val canSend = input.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .padding(bottom = bottomPadding),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        ContextToggleBar(
            selected = context,
            onSelect = onContextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S4)
                .padding(bottom = DesignTokens.Spacing.S4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
        ) {
            CircleIconButton(
                icon = Icons.Outlined.Mic,
                background = colors.primaryLight,
                tint = colors.primary,
                onClick = onVoiceInput,
                contentDescription = "Voice input",
            )

            InputBox(
                value = input,
                onValueChange = onInputChange,
                onSend = onSend,
                focusRequester = inputFocus, // #21
                modifier = Modifier.weight(1f),
            )

            CircleIconButton(
                icon = Icons.AutoMirrored.Outlined.Send,
                background = if (canSend) colors.primary else colors.muted,
                tint = if (canSend) colors.onPrimary else colors.textSecondary,
                enabled = canSend,
                onClick = onSend,
                contentDescription = "Send message",
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester, // #21
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.background)
            .border(width = 1.dp, color = colors.border, shape = shape)
            .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = "Type a message…",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            // #21: attach the focus requester so Edit chip can pull focus here
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            cursorBrush = SolidColor(colors.primary),
            textStyle = LocalTextStyle.current.copy(
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Base,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
    }
}
