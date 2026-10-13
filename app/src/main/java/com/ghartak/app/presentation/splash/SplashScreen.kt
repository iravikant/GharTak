package com.ghartak.app.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ghartak.app.presentation.components.YeLoLogo
import com.ghartak.app.presentation.components.SystemBarsStyle
import com.ghartak.app.presentation.theme.BrandGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    SystemBarsStyle(darkIcons = false)

    val scaleAnim = remember { Animatable(0.7f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scaleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        launch { alphaAnim.animateTo(1f, tween(600)) }
        delay(1200) // brief brand moment before routing
        when (viewModel.resolveDestination()) {
            StartDestination.HOME -> onNavigateToHome()
            StartDestination.ONBOARDING -> onNavigateToOnboarding()
            StartDestination.AUTH -> onNavigateToAuth()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                alpha = alphaAnim.value
            }
        ) {
            YeLoLogo(size = 92.dp, onGradient = true)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Ye Lo !!",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Bas Bolo, Ye Lo !!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
