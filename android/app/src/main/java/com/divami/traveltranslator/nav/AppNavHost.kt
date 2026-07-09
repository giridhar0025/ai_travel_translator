package com.divami.traveltranslator.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.divami.traveltranslator.designsystem.AppTheme
import com.divami.traveltranslator.feature.auth.EmailLoginScreen
import com.divami.traveltranslator.feature.auth.ForgotPasswordScreen
import com.divami.traveltranslator.feature.auth.LoginScreen
import com.divami.traveltranslator.feature.auth.SignUpScreen
import com.divami.traveltranslator.feature.main.MainScaffold
import com.divami.traveltranslator.feature.onboarding.OnboardingFlow
import com.divami.traveltranslator.feature.onboarding.OnboardingViewModel
import com.divami.traveltranslator.feature.session.SessionUiState
import com.divami.traveltranslator.feature.session.SessionViewModel
import com.divami.traveltranslator.feature.session.StartRoute
import com.divami.traveltranslator.feature.splash.SplashScreen
import com.divami.traveltranslator.feature.welcome.WelcomeScreen

@Composable
fun AppNavHost(
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val sessionState by sessionViewModel.uiState.collectAsState()

    // Hold on the brand background until the persisted session is read, so an
    // already-authenticated user never flashes the login screen.
    when (val state = sessionState) {
        is SessionUiState.Loading ->
            Box(Modifier.fillMaxSize().background(AppTheme.colors.background))
        is SessionUiState.Ready ->
            AppNavGraph(startRoute = state.start, sessionViewModel = sessionViewModel)
    }
}

@Composable
private fun AppNavGraph(
    startRoute: StartRoute,
    sessionViewModel: SessionViewModel,
) {
    val navController = rememberNavController()
    val startDestination: Route = when (startRoute) {
        StartRoute.Welcome -> Route.Welcome
        StartRoute.Login -> Route.Splash
        StartRoute.Onboarding -> Route.Onboarding
        StartRoute.Home -> Route.Chat
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Welcome> {
            WelcomeScreen(onStart = {
                sessionViewModel.markWelcomeSeen()
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
                onAuthenticated = { navController.navigateToOnboarding() },
                onEmailLogin = { navController.navigate(Route.EmailLogin) },
            )
        }
        composable<Route.EmailLogin> {
            EmailLoginScreen(
                onBack = { navController.popBackStack() },
                onAuthenticated = { navController.navigateToOnboarding() },
                onForgotPassword = { navController.navigate(Route.ForgotPassword) },
                onSignUp = { navController.navigate(Route.SignUp) },
            )
        }
        composable<Route.SignUp> {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onAuthenticated = { navController.navigateToOnboarding() },
            )
        }
        composable<Route.ForgotPassword> {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable<Route.Onboarding> {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            OnboardingFlow(onComplete = { selectedLanguage ->
                onboardingViewModel.complete(selectedLanguage) {
                    navController.navigate(Route.Chat) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            })
        }
        composable<Route.Chat> {
            MainScaffold(
                onLogout = {
                    sessionViewModel.logout()
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}

private fun NavController.navigateToOnboarding() {
    navigate(Route.Onboarding) {
        popUpTo(0) { inclusive = true }
    }
}
