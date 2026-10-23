package com.divami.traveltranslator.feature.phrases

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.divami.traveltranslator.designsystem.DesignTokens

enum class PhraseCategory(
    val id: String,
    val displayName: String,
    val icon: ImageVector,
    val colors: DesignTokens.CategoryColors,
    val phrases: List<String>,
) {
    Restaurants(
        id = "restaurants",
        displayName = "Restaurants",
        icon = Icons.Outlined.Restaurant,
        colors = DesignTokens.Category.Restaurants,
        phrases = listOf(
            "Table for two, please",
            "Can I see the menu?",
            "I would like to order",
            "The bill, please",
            "Do you have vegetarian options?",
            "Is this spicy?",
        ),
    ),
    Directions(
        id = "directions",
        displayName = "Directions",
        icon = Icons.Outlined.Navigation,
        colors = DesignTokens.Category.Directions,
        phrases = listOf(
            "Where is the nearest subway?",
            "How do I get to…?",
            "Can you show me on the map?",
            "Is it walking distance?",
            "Which way is north?",
            "How far is it?",
        ),
    ),
    Transport(
        id = "transport",
        displayName = "Transport",
        icon = Icons.Outlined.DirectionsCar,
        colors = DesignTokens.Category.Transport,
        phrases = listOf(
            "How much to the airport?",
            "One ticket, please",
            "Does this bus go to…?",
            "What time is the next train?",
            "Can you call me a taxi?",
            "Stop here, please",
        ),
    ),
    Shopping(
        id = "shopping",
        displayName = "Shopping",
        icon = Icons.Outlined.ShoppingBag,
        colors = DesignTokens.Category.Shopping,
        phrases = listOf(
            "How much does this cost?",
            "Can I try this on?",
            "Do you accept credit cards?",
            "I'm just looking",
            "Do you have a smaller size?",
            "Can I get a discount?",
        ),
    ),
    Emergencies(
        id = "emergencies",
        displayName = "Emergencies",
        icon = Icons.Outlined.Warning,
        colors = DesignTokens.Category.Emergencies,
        phrases = listOf(
            "I need help",
            "Where is the hospital?",
            "Call the police",
            "I lost my passport",
            "I need a doctor",
            "This is an emergency",
            "Call an ambulance",
            "I am lost",
            "Please write it down",
        ),
    ),
    Medical(
        id = "medical",
        displayName = "Medical",
        icon = Icons.Outlined.LocalHospital,
        colors = DesignTokens.Category.Emergencies,
        phrases = listOf(
            "I need a doctor",
            "Where is the pharmacy?",
            "I'm allergic to peanuts",
            "I have a medical condition",
            "I need medicine",
            "I am deaf or hard of hearing",
            "Please speak slowly",
        ),
    ),
}
