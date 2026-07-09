package com.finemooll.shelka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.finemooll.shelka.presentation.navigation.ShelkaNavHost
import com.finemooll.shelka.presentation.theme.ShelkaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShelkaTheme {
                ShelkaNavHost()
            }
        }
    }
}
