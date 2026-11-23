package com.lydiaperspective.lydslice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lydiaperspective.lydslice.ui.LydSliceApp
import com.lydiaperspective.lydslice.ui.theme.LydSliceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LydSliceTheme {
                LydSliceApp()
            }
        }
    }
}
