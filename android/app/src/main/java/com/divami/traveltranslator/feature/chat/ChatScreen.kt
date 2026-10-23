package com.divami.traveltranslator.feature.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.divami.traveltranslator.R
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.AppBottomSheet
import com.divami.traveltranslator.designsystem.components.CoralButton
import com.divami.traveltranslator.designsystem.components.CoralButtonVariant
import com.divami.traveltranslator.designsystem.components.ToastHost
import com.divami.traveltranslator.designsystem.components.ToastKind
import com.divami.traveltranslator.designsystem.components.ToastModel
import com.divami.traveltranslator.designsystem.components.rememberToastState
import com.divami.traveltranslator.designsystem.minimumTouchTarget
import com.divami.traveltranslator.designsystem.rememberReducedMotion
import com.divami.traveltranslator.feature.audio.AudioPlayerViewModel
import com.divami.traveltranslator.feature.voice.VoicePhase
import com.divami.traveltranslator.feature.voice.VoiceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MaxRecordingMs = 60_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val colors = AppTheme.colors
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    // One shared player for the whole chat so only one message speaks at a time.
    val audioViewModel: AudioPlayerViewModel = hiltViewModel()
    val audioState by audioViewModel.state.collectAsState()
    var activePlayId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(audioState.playing, audioState.preparing) {
        if (!audioState.playing && !audioState.preparing) activePlayId = null
    }

    val clipboardManager = LocalClipboardManager.current
    val toastState = rememberToastState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val inputFocus = remember { FocusRequester() }
    val context = LocalContext.current

    var showNewChatConfirm by remember { mutableStateOf(false) }
    var showMicPrimer by remember { mutableStateOf(false) }
    var showToLocal by remember { mutableStateOf<ChatMessage?>(null) }

    // Announce a finished translation once, rather than every streamed token.
    var streamingMessageId by remember { mutableStateOf<String?>(null) }
    val translationReady = stringResource(R.string.chat_translation_ready)
    val lastMessage = state.messages.lastOrNull()
    LaunchedEffect(lastMessage?.id, lastMessage?.isStreaming) {
        val message = lastMessage ?: return@LaunchedEffect
        if (message.isStreaming) {
            streamingMessageId = message.id
        } else if (streamingMessageId == message.id && message.translatedText.isNotBlank()) {
            view.announceForAccessibility(
                "$translationReady. ${message.translatedLanguage.displayName}: ${message.translatedText}",
            )
            streamingMessageId = null
        }
    }

    // Keep the newest message in view, but never fight a user who scrolled up.
    val lastMessageText = lastMessage?.translatedText
    val atBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= state.messages.lastIndex - 1
        }
    }
    LaunchedEffect(state.messages.size, lastMessageText, atBottom) {
        if (state.messages.isNotEmpty() && atBottom) {
            if (reducedMotion) listState.scrollToItem(state.messages.size - 1)
            else listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Inline voice recording.
    val voiceViewModel: VoiceViewModel = hiltViewModel()
    val voiceState by voiceViewModel.state.collectAsState()
    var voiceActive by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    val amplitudes = remember { mutableStateListOf<Float>() }

    fun beginVoice() {
        voiceError = null
        amplitudes.clear()
        elapsedMs = 0L
        voiceActive = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        voiceViewModel.startRecording(state.sourceLanguage.code)
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showMicPrimer = false
            beginVoice()
        } else {
            showMicPrimer = false
            voiceError = context.getString(R.string.voice_mic_error)
        }
    }

    fun onMicTap() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) beginVoice() else showMicPrimer = true
    }

    // Poll mic amplitude + tick the timer while recording (drives the waveform).
    LaunchedEffect(voiceActive, voiceState.phase) {
        if (voiceActive && voiceState.phase == VoicePhase.Recording) {
            while (voiceActive && voiceState.phase == VoicePhase.Recording) {
                val level = (voiceViewModel.amplitude().toFloat() / 32767f).coerceIn(0f, 1f)
                amplitudes.add(level)
                if (amplitudes.size > 48) amplitudes.removeAt(0)
                elapsedMs += 90
                if (elapsedMs >= MaxRecordingMs) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    voiceViewModel.stopAndTranscribe()
                    break
                }
                delay(90)
            }
        }
    }

    // Deliver the transcript, or show an actionable error.
    LaunchedEffect(voiceState.phase) {
        if (voiceState.phase == VoicePhase.Review) {
            when {
                voiceState.error != null -> voiceError = voiceState.error
                voiceState.transcript.isNotBlank() -> {
                    viewModel.onInputChange(voiceState.transcript)
                    inputFocus.requestFocus()
                    keyboard?.show()
                }
            }
            voiceActive = false
            amplitudes.clear()
            voiceViewModel.clear()
        }
    }

    // When an inline error/voice bar appears it shrinks the list viewport.
    // Keep the newest message visible instead of leaving its bottom clipped.
    LaunchedEffect(voiceError, state.error) {
        if ((voiceError != null || state.error != null) && state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }

    LaunchedEffect(audioState.error) {
        audioState.error?.let { message ->
            toastState.show(ToastModel(message, ToastKind.Error), durationMillis = 5_000L)
            audioViewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            ChatTopBar(
                onNewChat = {
                    if (state.messages.isEmpty()) viewModel.newConversation()
                    else showNewChatConfirm = true
                },
                canClear = state.messages.isNotEmpty(),
            )

            LanguageSelectorBar(
                source = state.sourceLanguage,
                target = state.targetLanguage,
                onSourceChange = viewModel::onSourceLanguageChange,
                onTargetChange = viewModel::onTargetLanguageChange,
                onSwap = viewModel::swapLanguages,
            )

            if (state.messages.isEmpty()) {
                EmptyChatState(
                    modifier = Modifier.weight(1f),
                    onSuggestion = { phrase ->
                        viewModel.onInputChange(phrase)
                        inputFocus.requestFocus()
                        keyboard?.show()
                    },
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S4),
                        contentPadding = PaddingValues(vertical = DesignTokens.Spacing.S6),
                    ) {
                        val lastId = state.messages.lastOrNull()?.id
                        items(state.messages, key = { it.id }) { message ->
                            val playback = when {
                                activePlayId != message.id -> PlaybackState.Idle
                                audioState.preparing -> PlaybackState.Preparing
                                audioState.playing -> PlaybackState.Playing
                                else -> PlaybackState.Idle
                            }
                            MessageBubble(
                                message = message,
                                flipped = state.flippedIds.contains(message.id),
                                playback = playback,
                                isOwnLatest = message.sender == Sender.You &&
                                    message.id == lastId && !message.isStreaming,
                                onFlip = { viewModel.toggleFlip(message.id) },
                                onPlayToggle = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (activePlayId == message.id &&
                                        (audioState.playing || audioState.preparing)
                                    ) {
                                        audioViewModel.stop()
                                        activePlayId = null
                                    } else {
                                        activePlayId = message.id
                                        audioViewModel.play(
                                            message.translatedText,
                                            message.translatedLanguage.code,
                                            slow = false,
                                        )
                                    }
                                },
                                onSlow = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    activePlayId = message.id
                                    audioViewModel.play(
                                        message.translatedText,
                                        message.translatedLanguage.code,
                                        slow = true,
                                    )
                                },
                                onEdit = {
                                    viewModel.onInputChange(message.originalText)
                                    inputFocus.requestFocus()
                                    keyboard?.show()
                                },
                                onCopy = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    clipboardManager.setText(AnnotatedString(message.translatedText))
                                    scope.launch {
                                        toastState.show(
                                            ToastModel(
                                                context.getString(R.string.chat_copied),
                                                ToastKind.Success,
                                            ),
                                        )
                                    }
                                },
                                onShow = { showToLocal = message },
                            )
                        }
                    }

                    if (!atBottom) {
                        JumpToLatest(
                            onClick = {
                                scope.launch {
                                    if (reducedMotion) listState.scrollToItem(state.messages.lastIndex)
                                    else listState.animateScrollToItem(state.messages.lastIndex)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = DesignTokens.Spacing.S4),
                        )
                    }
                }
            }

            if (voiceError != null) {
                VoiceErrorBar(
                    message = voiceError.orEmpty(),
                    onRetry = {
                        voiceError = null
                        beginVoice()
                    },
                    onDismiss = { voiceError = null },
                    onTypeInstead = {
                        voiceError = null
                        inputFocus.requestFocus()
                        keyboard?.show()
                    },
                )
            }

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                state.error?.let { errorMsg ->
                    ErrorBar(
                        message = errorMsg,
                        onRetry = viewModel::retry,
                        canRetry = state.failedText != null,
                        onDismiss = viewModel::dismissError,
                    )
                }
            }

            ChatInputArea(
                input = state.input,
                context = state.contextMode,
                onContextChange = viewModel::onContextChange,
                onInputChange = viewModel::onInputChange,
                onMicTap = { onMicTap() },
                isRecording = voiceActive && voiceState.phase == VoicePhase.Recording,
                isTranscribing = voiceState.phase == VoicePhase.Transcribing,
                elapsedMs = elapsedMs,
                amplitudes = amplitudes,
                onCancelVoice = {
                    if (voiceState.phase == VoicePhase.Transcribing) voiceViewModel.cancelTranscription()
                    else voiceViewModel.cancel()
                    voiceActive = false
                    amplitudes.clear()
                    voiceError = null
                    voiceViewModel.clear()
                },
                onSendVoice = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    voiceViewModel.stopAndTranscribe()
                },
                onSend = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    keyboard?.hide()
                    viewModel.onSend()
                },
                bottomPadding = contentPadding.calculateBottomPadding(),
                inputFocus = inputFocus,
            )
        }

        ToastHost(state = toastState)
    }

    if (showNewChatConfirm) {
        AlertDialog(
            onDismissRequest = { showNewChatConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.chat_new_confirm_title),
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = { Text(stringResource(R.string.chat_new_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNewChatConfirm = false
                        viewModel.newConversation()
                    },
                ) {
                    Text(stringResource(R.string.chat_new_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            containerColor = colors.card,
        )
    }

    if (showMicPrimer) {
        AppBottomSheet(onDismissRequest = { showMicPrimer = false }) {
            Text(
                text = stringResource(R.string.voice_permission_title),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.voice_permission_body),
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S3),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DesignTokens.Spacing.S6),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
            ) {
                CoralButton(
                    text = stringResource(R.string.voice_permission_allow),
                    onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                )
                CoralButton(
                    text = stringResource(R.string.voice_type_instead),
                    variant = CoralButtonVariant.Secondary,
                    onClick = {
                        showMicPrimer = false
                        inputFocus.requestFocus()
                        keyboard?.show()
                    },
                )
            }
        }
    }

    showToLocal?.let { message ->
        PointToSpeakDialog(
            message = message,
            isPlaying = activePlayId == message.id && (audioState.playing || audioState.preparing),
            onPlay = {
                activePlayId = message.id
                audioViewModel.play(message.translatedText, message.translatedLanguage.code, slow = false)
            },
            onSlow = {
                activePlayId = message.id
                audioViewModel.play(message.translatedText, message.translatedLanguage.code, slow = true)
            },
            onStop = {
                audioViewModel.stop()
                activePlayId = null
            },
            onDismiss = { showToLocal = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChatState(
    modifier: Modifier = Modifier,
    onSuggestion: (String) -> Unit,
) {
    val colors = AppTheme.colors
    val fontScale = LocalDensity.current.fontScale
    val compact = fontScale >= 1.3f
    val suggestions = listOf(
        stringResource(R.string.chat_suggestion_train),
        stringResource(R.string.chat_suggestion_allergy),
        stringResource(R.string.chat_suggestion_price),
    )
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                if (compact) DesignTokens.Spacing.S2 else DesignTokens.Spacing.S4,
            ),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S6),
        ) {
            if (!compact) {
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
            }
            Text(
                text = stringResource(R.string.chat_empty_title),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.chat_empty_subtitle),
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
                textAlign = TextAlign.Center,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    DesignTokens.Spacing.S2,
                    Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
            ) {
                suggestions.forEach { phrase ->
                    SuggestionChip(phrase = phrase, onClick = { onSuggestion(phrase) })
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(phrase: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Text(
        text = phrase,
        color = colors.primary,
        fontSize = DesignTokens.Typography.Size.Sm,
        modifier = Modifier
            .minimumTouchTarget()
            .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
            .background(colors.primaryLight)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
    )
}

@Composable
private fun ChatTopBar(
    onNewChat: () -> Unit,
    canClear: Boolean,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.chat_title),
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Xl,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Row(
            modifier = Modifier
                .minimumTouchTarget()
                .heightIn(min = DesignTokens.TouchTarget.Min)
                .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
                .background(if (canClear) colors.primaryLight else colors.muted)
                .clickable(enabled = canClear, onClick = onNewChat)
                .semantics { role = Role.Button }
                .padding(horizontal = DesignTokens.Spacing.S3, vertical = DesignTokens.Spacing.S2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = if (canClear) colors.primary else colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.chat_new),
                color = if (canClear) colors.primary else colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Sm,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ErrorBar(
    message: String,
    onRetry: () -> Unit,
    canRetry: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.destructive.copy(alpha = 0.12f))
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
    ) {
        Text(
            text = message,
            color = colors.destructive,
            fontSize = DesignTokens.Typography.Size.Sm,
            modifier = Modifier.weight(1f),
        )
        if (canRetry) {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.chat_retry), color = colors.destructive)
            }
        }
        Box(
            modifier = Modifier
                .minimumTouchTarget()
                .size(DesignTokens.TouchTarget.Min)
                .clip(CircleShape)
                .clickable(onClick = onDismiss)
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.chat_dismiss_error),
                tint = colors.destructive,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceErrorBar(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onTypeInstead: () -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.destructive.copy(alpha = 0.12f))
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S2),
    ) {
        Text(
            text = stringResource(R.string.voice_error_title),
            color = colors.destructive,
            fontSize = DesignTokens.Typography.Size.Sm,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = message,
            color = colors.destructive,
            fontSize = DesignTokens.Typography.Size.Sm,
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.Spacing.S2),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
        ) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.voice_retry)) }
            TextButton(onClick = onTypeInstead) { Text(stringResource(R.string.voice_type_instead)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        }
    }
}

@Composable
private fun JumpToLatest(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .minimumTouchTarget()
            .clip(RoundedCornerShape(DesignTokens.Radius.Modal))
            .background(colors.card)
            .border(1.dp, colors.borderStrong, RoundedCornerShape(DesignTokens.Radius.Modal))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
    ) {
        Text(
            text = stringResource(R.string.chat_jump_latest),
            color = colors.primary,
            fontSize = DesignTokens.Typography.Size.Sm,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ChatInputArea(
    input: String,
    context: ContextMode,
    onContextChange: (ContextMode) -> Unit,
    onInputChange: (String) -> Unit,
    onMicTap: () -> Unit,
    isRecording: Boolean,
    isTranscribing: Boolean,
    elapsedMs: Long,
    amplitudes: List<Float>,
    onCancelVoice: () -> Unit,
    onSendVoice: () -> Unit,
    onSend: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    inputFocus: FocusRequester,
) {
    val colors = AppTheme.colors
    val canSend = input.isNotBlank()
    val voiceMode = isRecording || isTranscribing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .padding(bottom = bottomPadding),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        if (voiceMode) {
            RecordingBar(
                isTranscribing = isTranscribing,
                elapsedMs = elapsedMs,
                amplitudes = amplitudes,
                onCancel = onCancelVoice,
                onSend = onSendVoice,
            )
            return@Column
        }

        ContextToggleBar(
            selected = context,
            onSelect = onContextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S3),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S6)
                .padding(bottom = DesignTokens.Spacing.S4),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
        ) {
            InputBox(
                value = input,
                onValueChange = onInputChange,
                onSend = onSend,
                focusRequester = inputFocus,
                modifier = Modifier.weight(1f),
            )

            CircleIconButton(
                icon = if (canSend) Icons.AutoMirrored.Outlined.Send else Icons.Outlined.Mic,
                background = if (canSend) colors.primary else colors.primaryLight,
                tint = if (canSend) colors.onPrimary else colors.primary,
                onClick = if (canSend) onSend else onMicTap,
                contentDescription = stringResource(
                    if (canSend) R.string.chat_send_message else R.string.chat_record_voice,
                ),
            )
        }
    }
}

/** Inline recorder — a timer + live waveform with tap Cancel / tap Send (no hold). */
@Composable
private fun RecordingBar(
    isTranscribing: Boolean,
    elapsedMs: Long,
    amplitudes: List<Float>,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.S6)
            .padding(top = DesignTokens.Spacing.S3, bottom = DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
    ) {
        CircleIconButton(
            icon = Icons.Outlined.Close,
            background = colors.muted,
            tint = colors.destructive,
            onClick = onCancel,
            contentDescription = stringResource(
                if (isTranscribing) R.string.voice_cancel_transcription else R.string.voice_cancel_recording,
            ),
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
                .background(colors.background)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(DesignTokens.Radius.Lg))
                .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
        ) {
            if (isTranscribing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.primary,
                )
                Text(
                    text = stringResource(R.string.voice_transcribing),
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Base,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                PulsingDot()
                Text(
                    text = formatElapsed(elapsedMs),
                    color = colors.foreground,
                    fontSize = DesignTokens.Typography.Size.Base,
                    fontWeight = FontWeight.Medium,
                )
                Waveform(amplitudes = amplitudes, modifier = Modifier.weight(1f))
            }
        }

        CircleIconButton(
            icon = Icons.AutoMirrored.Outlined.Send,
            background = colors.primary,
            tint = colors.onPrimary,
            enabled = !isTranscribing,
            onClick = onSend,
            contentDescription = stringResource(R.string.voice_stop_transcribe),
        )
    }
}

/** Live audio waveform — recent amplitude samples as vertical bars. */
@Composable
private fun Waveform(amplitudes: List<Float>, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val reducedMotion = rememberReducedMotion()
    Row(
        modifier = modifier.height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        amplitudes.takeLast(40).forEach { level ->
            val barHeight = if (reducedMotion) 12.dp else (4f + level * 22f).dp
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(colors.primary),
            )
        }
    }
}

/** Small red dot that pulses while recording; static under reduced motion. */
@Composable
private fun PulsingDot() {
    val colors = AppTheme.colors
    val reducedMotion = rememberReducedMotion()
    val alpha = if (reducedMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "rec")
        val animated by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "rec-alpha",
        )
        animated
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(colors.destructive),
    )
}

private fun formatElapsed(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .minimumTouchTarget()
            .size(48.dp)
            .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
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
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val inputLabel = stringResource(R.string.chat_input_label)

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.background)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) colors.ring else colors.borderStrong,
                shape = shape,
            )
            .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = stringResource(R.string.chat_input_hint),
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics { contentDescription = inputLabel },
            singleLine = false,
            maxLines = 4,
            minLines = 1,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(colors.primary),
            textStyle = LocalTextStyle.current.copy(
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Base,
                lineHeight = DesignTokens.Typography.Size.Base * 1.4f,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
    }
}

@Composable
private fun PointToSpeakDialog(
    message: ChatMessage,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onSlow: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.card)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(DesignTokens.Spacing.S6)
                // Dialog windows on some OEM Android builds report no bottom
                // inset; this keeps the footer clear of 3-button navigation.
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.point_to_speak_title),
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Sm,
                    fontWeight = FontWeight.Medium,
                )
                CircleIconButton(
                    icon = Icons.Outlined.Close,
                    background = colors.muted,
                    tint = colors.foreground,
                    onClick = onDismiss,
                    contentDescription = stringResource(R.string.point_to_speak_close),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = DesignTokens.Spacing.S6),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                Text(
                    text = message.translatedText,
                    color = colors.foreground,
                    fontSize = 32.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = message.originalText,
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Base,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DesignTokens.Spacing.S6),
                )
                Text(
                    text = stringResource(R.string.point_to_speak_hint),
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Sm,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.S6),
                )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
            ) {
                CoralButton(
                    text = if (isPlaying) stringResource(R.string.chat_stop) else stringResource(R.string.audio_play_audio),
                    onClick = if (isPlaying) onStop else onPlay,
                    modifier = Modifier.weight(1f),
                )
                CoralButton(
                    text = stringResource(R.string.audio_slow),
                    onClick = onSlow,
                    variant = CoralButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                    leadingIcon = Icons.Outlined.Speed,
                )
            }
        }
    }
}
