package com.divami.traveltranslator.feature.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Shared side-effect plumbing for the auth screens (DRY): forwards
 * [AuthEffect.Authenticated] to navigation and shows errors as a toast. Each
 * screen calls this once with its own [AuthViewModel] instance.
 *
 * @param onAuthenticated invoked when sign-in/sign-up succeeds.
 * @param onPasswordResetSent optional handler for the forgot-password screen.
 */
@Composable
fun LaunchedAuthEffects(
    viewModel: AuthViewModel,
    context: Context,
    onAuthenticated: () -> Unit = {},
    onPasswordResetSent: () -> Unit = {},
) {
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AuthEffect.Authenticated -> onAuthenticated()
                AuthEffect.PasswordResetSent -> onPasswordResetSent()
            }
        }
    }

    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.clearError()
    }
}
