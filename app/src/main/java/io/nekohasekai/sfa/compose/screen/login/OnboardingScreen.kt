package io.nekohasekai.sfa.compose.screen.login

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.R
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
            
            // Dynamic Offsets for Background Blobs based on currentPage
            val blob1OffsetX by animateDpAsState(
                targetValue = when (pagerState.currentPage) {
                    0 -> (-150).dp
                    1 -> (-80).dp
                    else -> (-120).dp
                },
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "blob1x"
            )
            val blob1OffsetY by animateDpAsState(
                targetValue = when (pagerState.currentPage) {
                    0 -> 250.dp
                    1 -> 100.dp
                    else -> (-80).dp
                },
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "blob1y"
            )
            
            val blob2OffsetX by animateDpAsState(
                targetValue = when (pagerState.currentPage) {
                    0 -> 180.dp
                    1 -> 120.dp
                    else -> 150.dp
                },
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "blob2x"
            )
            val blob2OffsetY by animateDpAsState(
                targetValue = when (pagerState.currentPage) {
                    0 -> 100.dp
                    1 -> 320.dp
                    else -> 200.dp
                },
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "blob2y"
            )

            // Immersive Background Blobs
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedBlob(
                    shape = remember { WavyCookieShape(points = 12, waveDepth = 0.12f) },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                    size = 520.dp,
                    offset = DpOffset(x = blob1OffsetX, y = blob1OffsetY),
                    durationMillis = 30000
                )
                
                AnimatedBlob(
                    shape = remember { WavyCookieShape(points = 14, waveDepth = 0.1f) },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    size = 420.dp,
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
                        0 -> OnboardingCard(
                            icon = Icons.Rounded.Security,
                            title = stringResource(R.string.onboarding_title_1),
                            subtitle = stringResource(R.string.onboarding_subtitle_1)
                        )
                        1 -> OnboardingCard(
                            icon = Icons.Rounded.CardGiftcard,
                            title = stringResource(R.string.onboarding_title_2),
                            subtitle = stringResource(R.string.onboarding_subtitle_2)
                        )
                        2 -> OnboardingCard(
                            icon = Icons.Rounded.Speed,
                            title = stringResource(R.string.onboarding_title_3),
                            subtitle = stringResource(R.string.onboarding_subtitle_3)
                        )
                    }
                }
            }

            // Skip Button (Top Right)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, end = 24.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = pagerState.currentPage < 2,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    WavyButton(
                        onClick = onNavigateToLogin,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(stringResource(R.string.onboarding_skip), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Bottom Control Area (Page Indicators + Navigation)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            animationSpec = tween(durationMillis = 300),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                // Navigation Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Back Arrow
                    Box(modifier = Modifier.size(56.dp)) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = pagerState.currentPage > 0,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it / 2 }),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it / 2 })
                        ) {
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
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.previous))
                            }
                        }
                    }

                    // Right Forward Button or Finish Button
                    Box(contentAlignment = Alignment.CenterEnd) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = pagerState.currentPage < 2,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 })
                        ) {
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
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.next))
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = pagerState.currentPage == 2,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 })
                        ) {
                            Button(
                                onClick = onNavigateToLogin,
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_start),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
