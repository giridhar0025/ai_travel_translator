package com.divami.traveltranslator.feature.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.CoralButton
import com.divami.traveltranslator.designsystem.components.CoralButtonVariant
import com.divami.traveltranslator.designsystem.components.Waveform
import kotlinx.coroutines.delay

private val MockTranscripts = listOf(
    "Hello, can you help me find a restaurant nearby?",
    "How much does this cost?",
    "Where is the nearest train station?",
)
private const val MockDelayMs = 2000L

@Composable
fun VoiceInputScreen(
    onBack: () -> Unit,
    onTranslate: (String) -> Unit,
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val insets = WindowInsets.systemBars.asPaddingValues()

    val micGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    var isRecording by rememberSaveable { mutableStateOf(true) }
    var transcript by rememberSaveable { mutableStateOf("") }
    var attempt by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(onBack = onBack)

    LaunchedEffect(isRecording, micGranted) {
        if (isRecording && micGranted) {
            delay(MockDelayMs)
            transcript = MockTranscripts[attempt % MockTranscripts.size]
            isRecording = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding()),
    ) {
        Header(onBack = onBack)

        if (!micGranted) {
            PermissionDeniedBody(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                },
                onCancel = onBack,
            )
        } else {
            AnimatedContent(
                targetState = isRecording,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "voiceInputState",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { recording ->
                if (recording) {
                    RecordingBody()
                } else {
                    ReviewBody(
                        transcript = transcript,
                        onTranscriptChange = { transcript = it },
                    )
                }
            }

            Actions(
                isRecording = isRecording,
                canTranslate = transcript.isNotBlank(),
                onStop = { isRecording = false },
                onTranslate = {
                    onTranslate(transcript.trim())
                },
                onRecordAgain = {
                    transcript = ""
                    attempt += 1
                    isRecording = true
                },
            )
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = DesignTokens.Spacing.S6,
                vertical = DesignTokens.Spacing.S4,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
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
        Text(
            text = "Voice Input",
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Lg,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun RecordingBody() {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DesignTokens.Spacing.S6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PulsingMic()

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S8))

        Waveform(
            barCount = 8,
            isAnimated = true,
            activeColor = colors.primary,
            height = 64.dp,
        )

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))

        Text(
            text = "Listening…",
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Lg,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S2))
        Text(
            text = "Speak clearly into your device",
            color = colors.textSecondary,
            fontSize = DesignTokens.Typography.Size.Sm,
        )
    }
}

@Composable
private fun PulsingMic() {
    val colors = AppTheme.colors
    val transition = rememberInfiniteTransition(label = "micPing")
    val pingScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pingScale",
    )
    val pingAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pingAlpha",
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pingScale)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = pingAlpha)),
        )
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(colors.primary, colors.primaryDark),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(72.dp),
            )
        }
    }
}

@Composable
private fun ReviewBody(
    transcript: String,
    onTranscriptChange: (String) -> Unit,
) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DesignTokens.Spacing.S6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(colors.success.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(64.dp),
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))
        Text(
            text = "Recording complete",
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Lg,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DesignTokens.Radius.Xl))
                .background(colors.card)
                .border(
                    width = 1.dp,
                    color = colors.border,
                    shape = RoundedCornerShape(DesignTokens.Radius.Xl),
                )
                .padding(DesignTokens.Spacing.S4),
        ) {
            Text(
                text = "Transcript",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Sm,
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.S2))
            BasicTextField(
                value = transcript,
                onValueChange = onTranscriptChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                cursorBrush = SolidColor(colors.primary),
                textStyle = LocalTextStyle.current.copy(
                    color = colors.foreground,
                    fontSize = DesignTokens.Typography.Size.Base,
                ),
                decorationBox = { inner ->
                    if (transcript.isEmpty()) {
                        Text(
                            text = "Your speech will appear here…",
                            color = colors.textSecondary,
                            fontSize = DesignTokens.Typography.Size.Base,
                        )
                    }
                    inner()
                },
            )
        }
    }
}

@Composable
private fun PermissionDeniedBody(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier.padding(horizontal = DesignTokens.Spacing.S6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(colors.destructive.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = null,
                tint = colors.destructive,
                modifier = Modifier.size(64.dp),
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S6))
        Text(
            text = "Microphone access required",
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Lg,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S3))
        Text(
            text = "Enable microphone in Settings to record your voice for translation.",
            color = colors.textSecondary,
            fontSize = DesignTokens.Typography.Size.Sm,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S8))
        CoralButton(text = "Open Settings", onClick = onOpenSettings)
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.S3))
        CoralButton(
            text = "Cancel",
            onClick = onCancel,
            variant = CoralButtonVariant.Secondary,
        )
    }
}

@Composable
private fun Actions(
    isRecording: Boolean,
    canTranslate: Boolean,
    onStop: () -> Unit,
    onTranslate: () -> Unit,
    onRecordAgain: () -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = DesignTokens.Spacing.S6,
                end = DesignTokens.Spacing.S6,
                bottom = DesignTokens.Spacing.S8,
                top = DesignTokens.Spacing.S4,
            ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(DesignTokens.Radius.Lg))
                    .background(colors.destructive)
                    .clickable(onClick = onStop)
                    .padding(vertical = DesignTokens.Spacing.S4),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Stop Recording",
                        color = colors.onPrimary,
                        fontSize = DesignTokens.Typography.Size.Base,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        } else {
            CoralButton(
                text = "Translate",
                onClick = onTranslate,
                enabled = canTranslate,
            )
            CoralButton(
                text = "Record Again",
                onClick = onRecordAgain,
                variant = CoralButtonVariant.Secondary,
            )
        }
    }
}
