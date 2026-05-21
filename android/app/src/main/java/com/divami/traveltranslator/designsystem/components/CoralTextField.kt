package com.divami.traveltranslator.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.designsystem.DesignTokens

enum class CoralTextFieldKind { Text, Email, Password }

@Composable
fun CoralTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    kind: CoralTextFieldKind = CoralTextFieldKind.Text,
    leadingIcon: ImageVector? = null,
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(DesignTokens.Radius.Lg)

    val visualTransformation: VisualTransformation =
        if (kind == CoralTextFieldKind.Password && !passwordVisible) PasswordVisualTransformation()
        else VisualTransformation.None

    val keyboardType = when (kind) {
        CoralTextFieldKind.Email -> KeyboardType.Email
        CoralTextFieldKind.Password -> KeyboardType.Password
        CoralTextFieldKind.Text -> KeyboardType.Text
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2)) {
        if (label != null) {
            Text(text = label, color = colors.foreground, fontSize = DesignTokens.Typography.Size.Sm)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.card)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) colors.ring else colors.border,
                    shape = shape,
                )
                .padding(horizontal = DesignTokens.Spacing.S4, vertical = DesignTokens.Spacing.S3)
                .defaultMinSize(minHeight = DesignTokens.TouchTarget.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 24.dp),
                singleLine = true,
                interactionSource = interactionSource,
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(colors.ring),
                textStyle = LocalTextStyle.current.copy(
                    color = colors.foreground,
                    fontSize = DesignTokens.Typography.Size.Base,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Next,
                ),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = colors.textSecondary,
                            fontSize = DesignTokens.Typography.Size.Base,
                        )
                    }
                    innerTextField()
                },
            )
            if (kind == CoralTextFieldKind.Password) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = colors.textSecondary,
                    )
                }
            }
        }
    }
}
