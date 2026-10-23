package com.divami.traveltranslator.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.R
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens

enum class AppTab(@StringRes val labelRes: Int) {
    Chat(R.string.nav_chat),
    Phrases(R.string.nav_phrases),
    History(R.string.nav_history),
    Settings(R.string.nav_settings),
}

private fun iconFor(tab: AppTab, active: Boolean): ImageVector = when (tab) {
    AppTab.Chat     -> if (active) Icons.AutoMirrored.Filled.Chat   else Icons.AutoMirrored.Outlined.Chat
    AppTab.Phrases  -> if (active) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
    AppTab.History  -> if (active) Icons.Filled.History             else Icons.Outlined.History
    AppTab.Settings -> if (active) Icons.Filled.Settings            else Icons.Outlined.Settings
}

@Composable
fun BottomNavBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.S2, vertical = DesignTokens.Spacing.S2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            for (tab in AppTab.entries) {
                val active = tab == selected
                val label = stringResource(tab.labelRes)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = { onSelect(tab) })
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            this.selected = active
                        }
                        .defaultMinSize(
                            minWidth = 48.dp,
                            minHeight = DesignTokens.TouchTarget.Min,
                        )
                        .padding(vertical = DesignTokens.Spacing.S2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S1),
                ) {
                    Icon(
                        imageVector = iconFor(tab, active),
                        contentDescription = null,
                        tint = if (active) colors.primary else colors.textSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                    // Subtle 4dp selection pill beneath the icon; invisible when not active.
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                color = if (active) colors.primary else androidx.compose.ui.graphics.Color.Transparent,
                                shape = RoundedCornerShape(2.dp),
                            ),
                    )
                    Text(
                        text = label,
                        color = if (active) colors.primary else colors.textSecondary,
                        fontSize = DesignTokens.Typography.Size.Xs,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

