package org.squalus.pnat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.squalus.pnat.ui.home.HomeScreen
import org.squalus.pnat.ui.splash.BrandIntroScreen
import org.squalus.pnat.ui.theme.Pnat_mobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Pnat_mobile)
        super.onCreate(savedInstanceState)

        val brandSystemBarStyle = SystemBarStyle.dark(getColor(R.color.pnat_blue))

        setContent {
            Pnat_mobileTheme {
                var showBrandIntro by rememberSaveable { mutableStateOf(true) }

                SideEffect {
                    if (showBrandIntro) {
                        enableEdgeToEdge(
                            statusBarStyle = brandSystemBarStyle,
                            navigationBarStyle = brandSystemBarStyle
                        )
                    } else {
                        enableEdgeToEdge()
                    }
                }

                if (showBrandIntro) {
                    BrandIntroScreen(
                        onFinished = { showBrandIntro = false }
                    )
                } else {
                    HomeScreen()
                }
            }
        }
    }
}
