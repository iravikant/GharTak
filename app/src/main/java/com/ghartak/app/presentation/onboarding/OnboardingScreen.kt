package com.ghartak.app.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghartak.app.presentation.components.PrimaryButton

private data class OnboardingPage(val title: String, val description: String)

private val pages = listOf(
    OnboardingPage("Send parcels in minutes", "Book a rider for any local delivery, big or small."),
    OnboardingPage("Track in real time", "Watch your rider move on the map from pickup to drop."),
    OnboardingPage("Delivered securely", "OTP verification confirms your parcel reaches the right hands.")
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onFinished, modifier = Modifier.align(Alignment.End)) {
            Text("Skip")
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val item = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(item.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        PrimaryButton(
            text = if (pagerState.currentPage == pages.lastIndex) "Get started" else "Next",
            onClick = {
                if (pagerState.currentPage == pages.lastIndex) onFinished()
                // Advancing the pager on intermediate pages is handled by user swipe;
                // a real implementation would animateScrollToPage here via a coroutine scope.
            }
        )
    }
}
