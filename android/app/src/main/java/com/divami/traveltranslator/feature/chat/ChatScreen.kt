package com.divami.traveltranslator.feature.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.divami.traveltranslator.designsystem.components.ToastHost
import com.divami.traveltranslator.designsystem.components.ToastKind
import com.divami.traveltranslator.designsystem.components.ToastModel
import com.divami.traveltranslator.designsystem.components.rememberToastState
import com.divami.traveltranslator.feature.audio.AudioPlayerViewModel
import com.divami.traveltranslator.feature.voice.VoicePhase
import com.divami.traveltranslator.feature.voice.VoiceViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val colors = AppTheme.colors
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Inline audio playback — one shared player for the whole chat so only one
    // message speaks at a time and each bubble reflects play/stop/loading.
    val audioViewModel: AudioPlayerViewModel = hiltViewModel()
    val audioState by audioViewModel.state.collectAsState()
    var activePlayId by remember { mutableStateOf<String?>(null) }

    // Clear the "which bubble is playing" marker once audio stops/finishes.
    LaunchedEffect(audioState.playing, audioState.preparing) {
        if (!audioState.playing && !audioState.preparing) activePlayId = null
    }

    // #6: clipboard + toast
    val clipboardManager = LocalClipboardManager.current
    val toastState = rememberToastState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // #20: keyboard controller — dismiss on send
    val keyboard = LocalSoftwareKeyboardController.current

    // #21: focus requester for the input field — Edit chip focuses it
    val inputFocus = remember { FocusRequester() }

    // Keep the newest message in view — including while its translation streams
    // in token-by-token (keyed on the last message's growing text).
    val lastMessageText = state.messages.lastOrNull()?.translatedText
    LaunchedEffect(state.messages.size, lastMessageText) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Inline voice recording (tap-to-record) — replaces the full-screen recorder.
    val context = LocalContext.current
    val voiceViewModel: VoiceViewModel = hiltViewModel()
    val voiceState by voiceViewModel.state.collectAsState()
    var voiceActive by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableStateOf(0L) }
    val amplitudes = remember { mutableStateListOf<Float>() }

    fun beginVoice() {
        amplitudes.clear()
        elapsedMs = 0L
        voiceActive = true
        voiceViewModel.startRecording(state.sourceLanguage.code)
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) beginVoice() }

    fun onMicTap() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) beginVoice() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Poll mic amplitude + tick the timer while recording (drives the waveform).
    LaunchedEffect(voiceActive, voiceState.phase) {
        if (voiceActive && voiceState.phase == VoicePhase.Recording) {
            while (voiceActive && voiceState.phase == VoicePhase.Recording) {
                val level = (voiceViewModel.amplitude().toFloat() / 32767f).coerceIn(0f, 1f)
                amplitudes.add(level)
                if (amplitudes.size > 48) amplitudes.removeAt(0)
                elapsedMs += 90
                delay(90)
            }
        }
    }

    // Deliver the transcript into the input once transcription finishes.
    LaunchedEffect(voiceState.phase) {
        if (voiceState.phase == VoicePhase.Review) {
            if (voiceState.transcript.isNotBlank()) viewModel.onInputChange(voiceState.transcript)
            voiceState.error?.let { msg ->
                scope.launch { toastState.show(ToastModel(msg, ToastKind.Error)) }
            }
            voiceActive = false
            amplitudes.clear()
            voiceViewModel.clear()
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
            ChatTopBar(
                onNewChat = viewModel::newConversation,
                canClear = state.messages.isNotEmpty(),
            )

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
                            // Inline play: tap toggles speak/stop on this bubble.
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
                            // Long-press the play chip → speak slowly.
                            onSlow = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                activePlayId = message.id
                                audioViewModel.play(
                                    message.translatedText,
                                    message.translatedLanguage.code,
                                    slow = true,
                                )
                            },
                            // #21: Edit sets text, focuses input, shows keyboard
                            onEdit = {
                                viewModel.onInputChange(message.originalText)
                                inputFocus.requestFocus()
                                keyboard?.show()
                            },
                            // #6: Copy writes translatedText to clipboard and shows a toast
                            onCopy = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                onMicTap = { onMicTap() },
                isRecording = voiceActive && voiceState.phase == VoicePhase.Recording,
                isTranscribing = voiceState.phase == VoicePhase.Transcribing,
                elapsedMs = elapsedMs,
                amplitudes = amplitudes,
                onCancelVoice = {
                    voiceViewModel.cancel()
                    voiceActive = false
                    amplitudes.clear()
                    voiceViewModel.clear()
                },
                onSendVoice = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    voiceViewModel.stopAndTranscribe()
                },
                // #20: hide keyboard before delegating to ViewModel
                onSend = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
            .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Conversation",
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Lg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
                .background(if (canClear) colors.primaryLight else colors.muted)
                .clickable(enabled = canClear, onClick = onNewChat)
                .padding(horizontal = DesignTokens.Spacing.S3, vertical = DesignTokens.Spacing.S2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "New chat",
                tint = if (canClear) colors.primary else colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "New",
                color = if (canClear) colors.primary else colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Sm,
                fontWeight = FontWeight.Medium,
            )
        }
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
    inputFocus: FocusRequester, // #21: passed down so InputBox can attach focusRequester
) {
    val colors = AppTheme.colors
    val canSend = input.isNotBlank()
    val voiceMode = isRecording || isTranscribing

    // Classic adjustResize (no edge-to-edge): the window physically shrinks above
    // the keyboard, so this row rises with it. bottomPadding clears the nav bar.
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
            InputBox(
                value = input,
                onValueChange = onInputChange,
                onSend = onSend,
                focusRequester = inputFocus, // #21
                modifier = Modifier.weight(1f),
            )

            // One trailing button that morphs: mic when the field is empty (starts
            // inline voice recording), send arrow once you've typed something.
            CircleIconButton(
                icon = if (canSend) Icons.AutoMirrored.Outlined.Send else Icons.Outlined.Mic,
                background = if (canSend) colors.primary else colors.primaryLight,
                tint = if (canSend) colors.onPrimary else colors.primary,
                onClick = if (canSend) onSend else onMicTap,
                contentDescription = if (canSend) "Send message" else "Record voice",
            )
        }
    }
}

/** Inline recorder that replaces the input row while capturing voice — a red
 *  timer + live waveform with tap Cancel / tap Send (accessible, no hold). */
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
            .padding(horizontal = DesignTokens.Spacing.S4)
            .padding(top = DesignTokens.Spacing.S3, bottom = DesignTokens.Spacing.S4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
    ) {
        // Cancel / discard
        CircleIconButton(
            icon = Icons.Outlined.Delete,
            background = colors.muted,
            tint = colors.destructive,
            enabled = !isTranscribing,
            onClick = onCancel,
            contentDescription = "Cancel recording",
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
                .background(colors.background)
                .border(1.dp, colors.border, RoundedCornerShape(DesignTokens.Radius.Lg))
                .padding(horizontal = DesignTokens.Spacing.S4),
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
                    text = "Transcribing…",
                    color = colors.textSecondary,
                    fontSize = DesignTokens.Typography.Size.Base,
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

        // Stop + transcribe
        CircleIconButton(
            icon = Icons.AutoMirrored.Outlined.Send,
            background = colors.primary,
            tint = colors.onPrimary,
            enabled = !isTranscribing,
            onClick = onSend,
            contentDescription = "Stop and transcribe",
        )
    }
}

/** Live audio waveform — recent amplitude samples as vertical bars. */
@Composable
private fun Waveform(amplitudes: List<Float>, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        amplitudes.takeLast(40).forEach { level ->
            val barHeight = (4f + level * 22f).dp
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

/** Small red dot that pulses while recording. */
@Composable
private fun PulsingDot() {
    val colors = AppTheme.colors
    val transition = rememberInfiniteTransition(label = "rec")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "rec-alpha",
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .alpha(dotAlpha)
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
