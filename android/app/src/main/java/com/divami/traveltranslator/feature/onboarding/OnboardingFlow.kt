package com.divami.traveltranslator.feature.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.CoralButton
import com.divami.traveltranslator.designsystem.components.CoralButtonVariant
import com.divami.traveltranslator.designsystem.components.RadioCard

private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val Slides = listOf(
    OnboardingSlide(
        Icons.Outlined.ChatBubbleOutline,
        "Speak Instantly",
        "Communicate with locals in real-time using AI-powered translation",
    ),
    OnboardingSlide(
        Icons.AutoMirrored.Outlined.VolumeUp,
        "Translate Naturally",
        "Get accurate, context-aware translations that sound natural",
    ),
    OnboardingSlide(
        Icons.Outlined.Public,
        "Play Audio to Locals",
        "Let the app speak for you with native pronunciation",
    ),
)

private data class LanguageOption(val name: String, val available: Boolean, val note: String? = null)

private val Languages = listOf(
    LanguageOption("English", true),
    LanguageOption("Hindi", true),
    LanguageOption("Telugu", false, "Coming soon"),
    LanguageOption("Thai", true),
)

// NOTE(audit-#3): AppNavHost still calls onComplete without arg; update callsite separately
@Composable
fun OnboardingFlow(onComplete: (selectedLanguage: String) -> Unit) {
    // #11 — rememberSaveable so rotation preserves progress
    var step by rememberSaveable { mutableIntStateOf(0) }
    var selectedLanguage by rememberSaveable { mutableStateOf("English") }

    // #41 — BackHandler: enabled only when past slide 0 so slide-0 back exits normally
    BackHandler(enabled = step > 0) {
        step -= 1
    }

    when {
        step < Slides.size -> SlideStep(
            slide = Slides[step],
            index = step,
            onNext = { step += 1 },
            onSkip = { onComplete(selectedLanguage) },
            onBack = { step -= 1 },
        )
        step == Slides.size -> LanguageStep(
            selected = selectedLanguage,
            onSelect = { selectedLanguage = it },
            onNext = { step += 1 },
        )
        else -> MicrophoneStep(
            onEnable = { onComplete(selectedLanguage) },
            onSkip = { onComplete(selectedLanguage) },
        )
    }
}

@Composable
private fun SlideStep(
    slide: OnboardingSlide,
    index: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AppTheme.colors
    val systemInsets = WindowInsets.systemBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(
                top = systemInsets.calculateTopPadding(),
                bottom = systemInsets.calculateBottomPadding(),
            ),
    ) {
        // #41 — SpaceBetween: back arrow on left (only when index > 0), Skip on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.S6),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (index > 0) {
                // 48 dp tap target wrapping a 24 dp icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Previous slide",
                        tint = colors.foreground,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                // Invisible spacer keeps Skip pinned to the right on slide 0
                Spacer(Modifier.size(40.dp))
            }

            CoralButton(
                text = "Skip",
                onClick = onSkip,
                variant = CoralButtonVariant.Text,
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
                    .size(120.dp)
                    .clip(RoundedCornerShape(DesignTokens.Radius.Modal))
                    .background(colors.muted),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = slide.icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.S8))
            Text(
                text = slide.title,
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xxl,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.S4))
            Text(
                text = slide.description,
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.S4),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S8),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S6),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    DesignTokens.Spacing.S2,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Slides.indices.forEach { i ->
                    val active = i == index
                    val width by animateDpAsState(
                        targetValue = if (active) 32.dp else 8.dp,
                        label = "dotWidth",
                    )
                    Box(
                        modifier = Modifier
                            .size(width = width, height = 8.dp)
                            .clip(CircleShape)
                            .background(if (active) colors.primary else colors.border),
                    )
                }
            }
            CoralButton(
                text = "Next",
                onClick = onNext,
                trailingIcon = Icons.Outlined.ChevronRight,
            )
        }
    }
}

@Composable
private fun LanguageStep(
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    val colors = AppTheme.colors
    val systemInsets = WindowInsets.systemBars.asPaddingValues()

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
                .padding(top = DesignTokens.Spacing.S6 + DesignTokens.Spacing.S8, bottom = DesignTokens.Spacing.S8),
        ) {
            Text(
                text = "Choose Your Language",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xxl,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Select your primary language for translation",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
                modifier = Modifier.padding(top = DesignTokens.Spacing.S2),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S6),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
        ) {
            Languages.forEach { lang ->
                if (lang.available) {
                    RadioCard(
                        title = lang.name,
                        isSelected = selected == lang.name,
                        onClick = { onSelect(lang.name) },
                    )
                } else {
                    // Disabled card for unavailable languages (e.g. Telugu)
                    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(colors.muted)
                            .border(width = 2.dp, color = colors.border, shape = shape)
                            .defaultMinSize(minHeight = DesignTokens.TouchTarget.Min)
                            .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S4),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lang.name,
                                color = colors.foreground.copy(alpha = 0.5f),
                                fontSize = DesignTokens.Typography.Size.Base,
                            )
                            if (lang.note != null) {
                                Text(
                                    text = lang.note,
                                    color = colors.textSecondary,
                                    fontSize = DesignTokens.Typography.Size.Sm,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(width = 2.dp, color = colors.border, shape = CircleShape),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S8),
        ) {
            CoralButton(text = "Continue", onClick = onNext)
        }
    }
}

@Composable
private fun MicrophoneStep(
    onEnable: () -> Unit,
    onSkip: () -> Unit,
) {
    val colors = AppTheme.colors
    val systemInsets = WindowInsets.systemBars.asPaddingValues()
    val context = LocalContext.current

    // #11 — rememberSaveable so denied state survives rotation
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    // #4 — Honor grant/deny result
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onEnable() else permissionDenied = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(
                top = systemInsets.calculateTopPadding(),
                bottom = systemInsets.calculateBottomPadding(),
            )
            .padding(horizontal = DesignTokens.Spacing.S6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (permissionDenied) {
            // #4 — Denied state UI
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(DesignTokens.Radius.Modal))
                    .background(colors.destructive.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = colors.destructive,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.S8))
            Text(
                text = "Microphone access blocked",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xxl,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.S4))
            Text(
                text = "Enable microphone in Settings to use voice translation",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.S8))
            CoralButton(
                text = "Open Settings",
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                },
            )
            Spacer(Modifier.height(DesignTokens.Spacing.S3))
            CoralButton(
                text = "Maybe Later",
                onClick = onSkip,
                variant = CoralButtonVariant.Text,
            )
        } else {
            // Original enable-mic UI
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(DesignTokens.Radius.Modal))
                    .background(colors.muted),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.S8))
            Text(
                text = "Enable Microphone",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Xxl,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.S4))
            Text(
                text = "Allow microphone access to use voice translation features",
                color = colors.textSecondary,
                fontSize = DesignTokens.Typography.Size.Base,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.S8))
            CoralButton(
                text = "Enable Microphone",
                onClick = { permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) },
            )
            Spacer(Modifier.height(DesignTokens.Spacing.S3))
            CoralButton(
                text = "Maybe Later",
                onClick = onSkip,
                variant = CoralButtonVariant.Text,
            )
        }
    }
}
