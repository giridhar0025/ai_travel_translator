package com.divami.traveltranslator.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.divami.traveltranslator.designsystem.components.AppBottomSheet
import com.divami.traveltranslator.designsystem.components.AppLanguage
import com.divami.traveltranslator.designsystem.components.AppTab
import com.divami.traveltranslator.designsystem.components.BottomNavBar
import com.divami.traveltranslator.designsystem.components.CoralButton
import com.divami.traveltranslator.designsystem.components.CoralButtonVariant
import com.divami.traveltranslator.designsystem.components.CoralCard
import com.divami.traveltranslator.designsystem.components.CoralTextField
import com.divami.traveltranslator.designsystem.components.CoralTextFieldKind
import com.divami.traveltranslator.designsystem.components.LanguageFlag
import com.divami.traveltranslator.designsystem.components.RadioCard
import com.divami.traveltranslator.designsystem.components.SearchBar
import com.divami.traveltranslator.designsystem.components.ToastHost
import com.divami.traveltranslator.designsystem.components.ToastKind
import com.divami.traveltranslator.designsystem.components.ToastModel
import com.divami.traveltranslator.designsystem.components.Waveform
import com.divami.traveltranslator.designsystem.components.rememberToastState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentGallery() {
    val colors = AppTheme.colors
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(AppLanguage.English) }
    var tab by remember { mutableStateOf(AppTab.Chat) }
    var sheetOpen by remember { mutableStateOf(false) }
    val toast = rememberToastState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(DesignTokens.Spacing.S6),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S6),
            ) {
                Section("Buttons") {
                    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3)) {
                        CoralButton("Sign In", onClick = { scope.launch { toast.show(ToastModel("Primary tapped")) } })
                        CoralButton("Continue with Email", onClick = {}, leadingIcon = Icons.Outlined.Email)
                        CoralButton("Continue with Google", onClick = {}, variant = CoralButtonVariant.Secondary)
                        CoralButton("Loading", onClick = {}, isLoading = true)
                        CoralButton("Disabled", onClick = {}, enabled = false)
                        CoralButton("Skip for now", onClick = {}, variant = CoralButtonVariant.Text)
                    }
                }
                Section("Text fields") {
                    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3)) {
                        CoralTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "your@email.com",
                            label = "Email",
                            kind = CoralTextFieldKind.Email,
                            leadingIcon = Icons.Outlined.Email,
                        )
                        CoralTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "••••••••",
                            label = "Password",
                            kind = CoralTextFieldKind.Password,
                            leadingIcon = Icons.Outlined.Lock,
                        )
                    }
                }
                Section("Search") {
                    SearchBar(value = search, onValueChange = { search = it }, placeholder = "Search conversations…")
                }
                Section("Card") {
                    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3)) {
                        CoralCard {
                            Text("Static card", color = colors.foreground, fontSize = DesignTokens.Typography.Size.Lg, fontWeight = FontWeight.Medium)
                            Text("Used as a surface around content.", color = colors.textSecondary, fontSize = DesignTokens.Typography.Size.Base)
                        }
                        CoralCard(onClick = { scope.launch { toast.show(ToastModel("Card tapped")) } }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tappable card", color = colors.foreground, fontSize = DesignTokens.Typography.Size.Base, modifier = Modifier.weight(1f))
                                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textSecondary)
                            }
                        }
                    }
                }
                Section("Radio cards (language)") {
                    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3)) {
                        for (lang in AppLanguage.Supported) {
                            RadioCard(
                                title = "${lang.flag}  ${lang.displayName}",
                                isSelected = lang == selectedLanguage,
                                onClick = { selectedLanguage = lang },
                            )
                        }
                    }
                }
                Section("Language flags") {
                    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2)) {
                        for (lang in AppLanguage.Supported) LanguageFlag(lang)
                    }
                }
                Section("Waveforms") {
                    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3)) {
                        Text("8-bar live (animated)", color = colors.textSecondary, fontSize = DesignTokens.Typography.Size.Sm)
                        Waveform(barCount = 8, isAnimated = true)
                        Text("40-bar player (animated)", color = colors.textSecondary, fontSize = DesignTokens.Typography.Size.Sm)
                        Waveform(barCount = 40, isAnimated = true, height = androidx.compose.ui.unit.Dp(80f))
                        Text("40-bar idle", color = colors.textSecondary, fontSize = DesignTokens.Typography.Size.Sm)
                        Waveform(barCount = 40, isAnimated = false, height = androidx.compose.ui.unit.Dp(80f))
                    }
                }
                Section("Bottom sheet") {
                    CoralButton("Open bottom sheet", onClick = { sheetOpen = true })
                }
                Section("Toast") {
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S2)) {
                        CoralButton("Info",    onClick = { scope.launch { toast.show(ToastModel("Info message", ToastKind.Info)) } },    variant = CoralButtonVariant.Secondary, modifier = Modifier.weight(1f))
                        CoralButton("Success", onClick = { scope.launch { toast.show(ToastModel("Saved!",        ToastKind.Success)) } }, variant = CoralButtonVariant.Secondary, modifier = Modifier.weight(1f))
                        CoralButton("Error",   onClick = { scope.launch { toast.show(ToastModel("Broken",        ToastKind.Error)) } },  variant = CoralButtonVariant.Secondary, modifier = Modifier.weight(1f))
                    }
                }
            }
            BottomNavBar(selected = tab, onSelect = { tab = it })
        }
        ToastHost(state = toast, modifier = Modifier.fillMaxSize())
        if (sheetOpen) {
            AppBottomSheet(onDismissRequest = { sheetOpen = false }) {
                Text("Pick a language", color = colors.foreground, fontSize = DesignTokens.Typography.Size.Xl, fontWeight = FontWeight.SemiBold)
                Column(
                    modifier = Modifier.padding(top = DesignTokens.Spacing.S4),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3),
                ) {
                    for (lang in AppLanguage.Supported) {
                        RadioCard(
                            title = "${lang.flag}  ${lang.displayName}",
                            isSelected = lang == selectedLanguage,
                            onClick = {
                                selectedLanguage = lang
                                sheetOpen = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S3)) {
        Text(
            text = title,
            color = colors.foreground,
            fontSize = DesignTokens.Typography.Size.Xl,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}
