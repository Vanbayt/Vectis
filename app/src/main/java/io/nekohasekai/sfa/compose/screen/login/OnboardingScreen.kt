package io.nekohasekai.sfa.compose.screen.login

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onNavigateToLogin: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Dynamic Offsets for Blobs based on currentPage
            val blob1OffsetX by animateDpAsState(
                targetValue = if (pagerState.currentPage == 2) (-100).dp else (-150).dp,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "blob1x"
            )
            val blob1OffsetY by animateDpAsState(
                targetValue = if (pagerState.currentPage == 2) (-100).dp else 250.dp,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "blob1y"
            )
            
            val blob2OffsetX by animateDpAsState(
                targetValue = if (pagerState.currentPage == 2) 100.dp else 180.dp,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "blob2x"
            )
            val blob2OffsetY by animateDpAsState(
                targetValue = if (pagerState.currentPage == 2) 300.dp else 100.dp,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "blob2y"
            )

            // Immersive Background
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Blob 1
                AnimatedBlob(
                    shape = remember { WavyCookieShape(points = 12, waveDepth = 0.12f) },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    size = 500.dp,
                    offset = DpOffset(x = blob1OffsetX, y = blob1OffsetY),
                    durationMillis = 30000
                )
                
                // Blob 2
                AnimatedBlob(
                    shape = remember { WavyCookieShape(points = 14, waveDepth = 0.1f) },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                    size = 400.dp,
                    offset = DpOffset(x = blob2OffsetX, y = blob2OffsetY),
                    durationMillis = 25000,
                    reverse = true
                )
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (page) {
                        0 -> {
                            Text(
                                text = "Welcome",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "TODO: Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        1 -> {
                            Text(
                                text = "TODO: Screen 2",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        2 -> {
                            Text(
                                text = "TODO: Screen 3",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Skip Button
            if (pagerState.currentPage < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Skip", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Arrow (Hidden on first page)
                if (pagerState.currentPage > 0) {
                    WavyButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage - 1,
                                    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
                                )
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous")
                    }
                } else {
                    Spacer(modifier = Modifier.size(56.dp))
                }

                // Right side controls
                if (pagerState.currentPage < 2) {
                    // Right Arrow
                    WavyButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage + 1,
                                    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
                                )
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next")
                    }
                } else {
                    // Login Button on the last page
                    WavyButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Login", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
