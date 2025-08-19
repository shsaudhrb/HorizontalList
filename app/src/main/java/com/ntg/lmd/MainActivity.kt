package com.ntg.lmd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.ntg.lmd.navigation.appNavGraph
import com.ntg.lmd.ui.theme.lmdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            lmdTheme {
                val rootNavController = rememberNavController()
                appNavGraph(
                    rootNavController,
                )
            }
        }
    }
}
