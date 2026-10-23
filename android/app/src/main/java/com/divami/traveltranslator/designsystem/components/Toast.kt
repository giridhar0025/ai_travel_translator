package com.divami.traveltranslator.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens
import kotlinx.coroutines.delay

enum class ToastKind { Info, Success, Error }

data class ToastModel(val message: String, val kind: ToastKind = ToastKind.Info)

class ToastState {
    var current by mutableStateOf<ToastModel?>(null)
        private set

    suspend fun show(model: ToastModel, durationMillis: Long = 5_000L) {
        current = model
        delay(durationMillis)
        current = null
    }

    fun dismiss() { current = null }
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val toast = state.current
    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)

    Box(modifier = modifier.fillMaxSize()) {
        if (toast != null) {
            LaunchedEffect(toast) {
                // auto-dismiss safety net if caller used `current = ...` directly
                delay(5_000L)
                if (state.current === toast) state.dismiss()
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = DesignTokens.Spacing.S6, vertical = DesignTokens.Spacing.S8)
                    .shadow(elevation = 6.dp, shape = shape)
                    .clip(shape)
                    .background(colors.card)
                    .border(width = 1.dp, color = colors.borderStrong, shape = shape)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
            ) {
                Icon(
                    imageVector = when (toast.kind) {
                        ToastKind.Info    -> Icons.Filled.Info
                        ToastKind.Success -> Icons.Filled.CheckCircle
                        ToastKind.Error   -> Icons.Filled.Error
                    },
                    contentDescription = null,
                    tint = when (toast.kind) {
                        ToastKind.Info    -> colors.primary
                        ToastKind.Success -> colors.success
                        ToastKind.Error   -> colors.destructive
                    },
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = toast.message,
                    color = colors.cardForeground,
                    fontSize = DesignTokens.Typography.Size.Base,
                )
            }
        }
    }
}
