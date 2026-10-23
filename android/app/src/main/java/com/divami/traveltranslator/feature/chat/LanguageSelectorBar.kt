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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.divami.traveltranslator.R
import com.divami.traveltranslator.designsystem.components.SearchBar
import com.divami.traveltranslator.designsystem.minimumTouchTarget
import com.divami.traveltranslator.designsystem.rememberReducedMotion
import com.divami.traveltranslator.designsystem.components.LanguageFlag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val reducedMotion = rememberReducedMotion()
    var query by rememberSaveable { mutableStateOf("") }

    // #11: rememberSaveable so the sheet survives rotation; Saver serialises to String
    var picker by rememberSaveable(stateSaver = PickerModeSaver) { mutableStateOf<PickerMode?>(null) }

    // #40: accumulates 180° increments; animateFloatAsState smoothly interpolates the rotation
    var swapRotation by rememberSaveable { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = swapRotation,
        animationSpec = tween(260),
        label = "swap-arrow-rotation",
    )
    val rotation = if (reducedMotion) swapRotation else animatedRotation

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2),
        ) {
            LangPill(
                language = source,
                modifier = Modifier.weight(1f),
                onClick = {
                    query = ""
                    picker = PickerMode.Source
                },
            )
            Box(
                modifier = Modifier
                    .minimumTouchTarget()
                    .size(48.dp)
                    .clip(RoundedCornerShape(DesignTokens.Radius.Md))
                    .clickable {
                        swapRotation += 180f
                        onSwap()
                    }
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.SwapVert,
                    contentDescription = stringResource(R.string.language_swap),
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                )
            }
            LangPill(
                language = target,
                modifier = Modifier.weight(1f),
                onClick = {
                    query = ""
                    picker = PickerMode.Target
                },
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }

    val currentMode = picker
    if (currentMode != null) {
        AppBottomSheet(onDismissRequest = { picker = null }) {
            Text(
                text = stringResource(
                    if (currentMode == PickerMode.Source) R.string.language_select_source
                    else R.string.language_select_target,
                ),
                color = colors.foreground,
                fontSize = DesignTokens.Typography.Size.Lg,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.S3),
            )
            SearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.language_search),
            )
            // The language already chosen on the opposite picker is excluded from
            // the list entirely — you can never build a same-language pair.
            val otherSide = if (currentMode == PickerMode.Source) target else source
            val options = AppLanguage.Supported
                .filter { it != otherSide }
                .filter { query.isBlank() || it.searchableText.contains(query, ignoreCase = true) }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = DesignTokens.Spacing.S3),
            ) {
                if (options.isEmpty()) {
                    Text(
                        text = stringResource(R.string.language_no_results, query),
                        color = colors.textSecondary,
                        fontSize = DesignTokens.Typography.Size.Sm,
                        modifier = Modifier.padding(vertical = DesignTokens.Spacing.S4),
                    )
                }
                options.forEach { lang ->
                    val selected = lang == (if (currentMode == PickerMode.Source) source else target)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumTouchTarget()
                            .heightIn(min = DesignTokens.TouchTarget.Min)
                            .clickable {
                                if (currentMode == PickerMode.Source) onSourceChange(lang)
                                else onTargetChange(lang)
                                picker = null
                            }
                            .semantics {
                                role = Role.RadioButton
                                this.selected = selected
                            }
                            .padding(vertical = DesignTokens.Spacing.S3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
                    ) {
                        LanguageFlag(language = lang, compact = true)
                        Text(
                            text = lang.pickerLabel,
                            color = if (selected) colors.primary else colors.foreground,
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
            .minimumTouchTarget()
            .heightIn(min = DesignTokens.TouchTarget.Min)
            .clip(shape)
            .background(colors.muted)
            .border(width = 1.dp, color = colors.borderStrong, shape = shape)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
