package com.op1sync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.op1sync.app.ui.theme.OP1SyncTheme
import com.op1sync.app.navigation.OP1SyncNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OP1SyncTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OP1SyncNavHost()
                }
            }
        }
    }
}
