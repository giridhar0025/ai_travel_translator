package com.divami.traveltranslator.nav

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Welcome : Route
    @Serializable data object Splash : Route
    @Serializable data object Login : Route
    @Serializable data object EmailLogin : Route
    @Serializable data object SignUp : Route
    @Serializable data object ForgotPassword : Route
    @Serializable data object Onboarding : Route
    @Serializable data object Chat : Route
}
