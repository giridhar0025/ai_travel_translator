package com.divami.traveltranslator.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.divami.traveltranslator.feature.auth.EmailLoginScreen
import com.divami.traveltranslator.feature.auth.ForgotPasswordScreen
import com.divami.traveltranslator.feature.auth.LoginScreen
import com.divami.traveltranslator.feature.auth.SignUpScreen
import com.divami.traveltranslator.feature.main.MainScaffold
import com.divami.traveltranslator.feature.onboarding.OnboardingFlow
import com.divami.traveltranslator.feature.splash.SplashScreen
import com.divami.traveltranslator.feature.welcome.WelcomeScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    // NOTE(audit-#45): rememberSaveable survives rotation only — switch to DataStore when persistence lands
    var hasSeenWelcome by rememberSaveable { mutableStateOf(false) }
    val startDestination = if (hasSeenWelcome) Route.Splash else Route.Welcome

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Welcome> {
            WelcomeScreen(onStart = {
                hasSeenWelcome = true
                navController.navigate(Route.Splash)
            })
        }
        composable<Route.Splash> {
            SplashScreen(onComplete = {
                navController.navigate(Route.Login) {
                    popUpTo(Route.Splash) { inclusive = true }
                }
            })
        }
        composable<Route.Login> {
            LoginScreen(
                onGoogleLogin = { navController.navigateToOnboarding() },
                onAppleLogin = { navController.navigateToOnboarding() },
                onEmailLogin = { navController.navigate(Route.EmailLogin) },
                onSkip = { navController.navigateToOnboarding() },
            )
        }
        composable<Route.EmailLogin> {
            EmailLoginScreen(
                onBack = { navController.popBackStack() },
                onLogin = { navController.navigateToOnboarding() },
                onForgotPassword = { navController.navigate(Route.ForgotPassword) },
                onSignUp = { navController.navigate(Route.SignUp) },
            )
        }
        composable<Route.SignUp> {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onSignUp = { navController.navigateToOnboarding() },
            )
        }
        composable<Route.ForgotPassword> {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Onboarding> {
            // TODO(audit-#3): plumb selectedLanguage into ChatViewModel (DataStore in Phase 7)
            OnboardingFlow(onComplete = { _ ->
                navController.navigate(Route.Chat) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
        composable<Route.Chat> {
            MainScaffold(
                onLogout = {
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}

private fun androidx.navigation.NavController.navigateToOnboarding() {
    navigate(Route.Onboarding) {
        popUpTo(Route.Login) { inclusive = true }
    }
}
