package com.divami.traveltranslator.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = colors.card,
        contentColor = colors.cardForeground,
        shape = RoundedCornerShape(
            topStart = DesignTokens.Radius.Modal,
            topEnd = DesignTokens.Radius.Modal,
            bottomStart = androidx.compose.ui.unit.Dp(0f),
            bottomEnd = androidx.compose.ui.unit.Dp(0f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // ModalBottomSheet draws edge-to-edge; keep the last row and
                // any primary action above the gesture/3-button navigation bar.
                .navigationBarsPadding()
                .padding(
                    start = DesignTokens.Spacing.S6,
                    end = DesignTokens.Spacing.S6,
                    top = DesignTokens.Spacing.S4,
                    bottom = DesignTokens.Spacing.S6,
                ),
        ) {
            content()
        }
    }
}
