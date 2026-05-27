package com.divami.traveltranslator.feature.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.divami.traveltranslator.designsystem.components.LanguageFlag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import com.divami.traveltranslator.designsystem.components.AppBottomSheet
import com.divami.traveltranslator.designsystem.components.AppLanguage

private enum class PickerMode { Source, Target }

// Saver stores the enum name as a String; empty string means null.
private val PickerModeSaver: Saver<PickerMode?, String> = Saver(
    save = { it?.name ?: "" },
    restore = { if (it.isEmpty()) null else PickerMode.valueOf(it) },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectorBar(
    source: AppLanguage,
    target: AppLanguage,
    onSourceChange: (AppLanguage) -> Unit,
    onTargetChange: (AppLanguage) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    // #11: rememberSaveable so the sheet survives rotation; Saver serialises to String
    var picker by rememberSaveable(stateSaver = PickerModeSaver) { mutableStateOf<PickerMode?>(null) }

    // #40: accumulates 180° increments; animateFloatAsState smoothly interpolates the rotation
    var swapRotation by rememberSaveable { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = swapRotation,
        animationSpec = tween(260),
        label = "swap-arrow-rotation",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
        ) {
            LangPill(
                language = source,
                modifier = Modifier.weight(1f),
                onClick = { picker = PickerMode.Source },
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(DesignTokens.Radius.Md))
                    .clickable {
                        // #40: each tap adds 180° so the icon visually spins each time
                        swapRotation += 180f
                        onSwap()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = "Swap languages",
                    tint = colors.textSecondary,
                    // #40: apply animated rotation to the icon
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(animatedRotation),
                )
            }
            LangPill(
                language = target,
                modifier = Modifier.weight(1f),
                onClick = { picker = PickerMode.Target },
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }

    val currentMode = picker
    if (currentMode != null) {
        AppBottomSheet(onDismissRequest = { picker = null }) {
            Text(
                text = "Select ${if (currentMode == PickerMode.Source) "Source" else "Target"} Language",
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.S3),
            )
            AppLanguage.Supported.forEach { lang ->
                val selected = lang == (if (currentMode == PickerMode.Source) source else target)

                // #19: "other side" is the language already chosen on the opposite picker.
                // Auto-swap UX: if the user picks a language that conflicts with the other side,
                // we call onSwap() first to flip the pair and then apply the pick, giving a
                // smooth "swap on conflict" rather than blocking the selection with a disabled row.
                val otherSide = if (currentMode == PickerMode.Source) target else source
                val isConflict = lang == otherSide

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isConflict) {
                                // #19: conflict → auto-swap so source/target pair flips cleanly
                                onSwap()
                            } else {
                                if (currentMode == PickerMode.Source) onSourceChange(lang)
                                else onTargetChange(lang)
                            }
                            picker = null
                        }
                        .padding(vertical = DesignTokens.Spacing.S3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
                ) {
                    // Monogram chip — compact mode, dimmed when conflict
                    LanguageFlag(
                        language = lang,
                        compact = true,
                        modifier = if (isConflict) Modifier.alpha(0.38f) else Modifier,
                    )
                    Text(
                        text = lang.displayName,
                        color = when {
                            selected    -> colors.primary
                            isConflict  -> colors.foreground.copy(alpha = 0.38f)
                            else        -> colors.foreground
                        },
                        fontSize = DesignTokens.Typography.Size.Base,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LangPill(
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(DesignTokens.Radius.Md)
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.muted)
            .border(width = 1.dp, color = colors.border, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
    ) {
        Icon(
            imageVector = Icons.Outlined.Public,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = language.displayName,
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Sm,
        )
    }
}
