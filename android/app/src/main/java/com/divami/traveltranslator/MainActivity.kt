package com.divami.traveltranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.divami.traveltranslator.designsystem.ComponentGallery
import com.divami.traveltranslator.designsystem.TravelTranslatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TravelTranslatorTheme { ComponentGallery() }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewGallery() {
    TravelTranslatorTheme { ComponentGallery() }
}
