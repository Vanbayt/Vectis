package io.nekohasekai.sfa.compose.screen.login

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

    // Derived scroll position from 0f to 2f
    val scrollPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Dynamic Immersive Background
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Blob 1: Shifts downwards and changes color as we scroll
                AnimatedBlob(
                    shape = remember { WavyCookieShape(points = 12, waveDepth = 0.12f) },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f - (scrollPosition * 0.02f)),
                    size = 500.dp + (scrollPosition * 50).dp,
                    offset = DpOffset(
                        x = (-150).dp + (scrollPosition * 100).dp,
                        y = 250.dp - (scrollPosition * 50).dp
                    ),
                    durationMillis = 30000
                )
                
                // Blob 2: Moves left as we scroll
                AnimatedBlob(
                    shape = remember { WavyCookieShape(points = 14, waveDepth = 0.1f) },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f + (scrollPosition * 0.01f)),
                    size = 400.dp,
                    offset = DpOffset(
                        x = 180.dp - (scrollPosition * 80).dp,
                        y = 100.dp + (scrollPosition * 50).dp
                    ),
                    durationMillis = 25000,
                    reverse = true
                )

                // Blob 3: Appears dynamically on later pages
                val blob3Alpha = (scrollPosition - 0.5f).coerceIn(0f, 1f) * 0.06f
                if (blob3Alpha > 0f) {
                    AnimatedBlob(
                        shape = remember { WavyCookieShape(points = 10, waveDepth = 0.15f) },
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = blob3Alpha),
                        size = 350.dp,
                        offset = DpOffset(
                            x = 0.dp,
                            y = (-200).dp + (scrollPosition * 100).dp
                        ),
                        durationMillis = 20000
                    )
                }
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
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
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
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
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
