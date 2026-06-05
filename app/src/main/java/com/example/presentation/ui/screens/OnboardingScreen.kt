package com.example.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal slide pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingPageContent(
                        title = "Smart Script Editor",
                        description = "Tulis naskah berstandard industri film Hollywood langsung dari smartphone. Pemformatan otomatis: Scene Heading, Action, Dialogue, dan Shortcut cerdas.",
                        icon = Icons.Default.Create
                    )
                    1 -> OnboardingPageContent(
                        title = "AI Co-Writer Berdaya Tinggi",
                        description = "Bantu kembangkan ide cerita Anda. Lanjutkan scene aktif, perbaiki dialog agar lebih natural, atau dapati plot twist tak terduga dalam sekali ketukan.",
                        icon = Icons.Default.AutoAwesome
                    )
                    2 -> OnboardingPageContent(
                        title = "Manajemen Cast & Outline",
                        description = "Rancang relasi antartokoh di database karakter. Ekspor draf naskah Anda langsung ke format Fountain baku atau cetak PDF naskah siap syuting.",
                        icon = Icons.Default.MenuBook
                    )
                }
            }

            // Indicator dots and next buttons at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Next or start button
                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinished()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (pagerState.currentPage < 2) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Lanjut")
                    } else {
                        Text("Mulai Menulis", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(
    title: String,
    description: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}
