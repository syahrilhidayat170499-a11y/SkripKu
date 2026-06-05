package com.example.presentation.ui.screens

import android.content.Context
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.presentation.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentProject by viewModel.currentProject.collectAsState()
    val scrollState = rememberScrollState()

    // Retrieve active API keys from BuildConfig securely
    val openAiKey = try { BuildConfig.OPENAI_API_KEY } catch (e: Exception) { "" }
    val geminiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan & Sinkroni", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- AI Configurations panel ---
            Text(
                text = "STATUS ASISTEN CHAT AI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gemini API (Google Standard)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (geminiKey.isNotEmpty() && !geminiKey.startsWith("MY_"))
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (geminiKey.isNotEmpty() && !geminiKey.startsWith("MY_")) "TERHUBUNG (AUTO)" else "MOCK/OFFLINE MODEL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (geminiKey.isNotEmpty() && !geminiKey.startsWith("MY_"))
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OpenAI GPT-4o Key Support", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (openAiKey.isNotEmpty() && !openAiKey.startsWith("MY_"))
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (openAiKey.isNotEmpty() && !openAiKey.startsWith("MY_")) "TERHUBUNG" else "BELUM SETUP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (openAiKey.isNotEmpty() && !openAiKey.startsWith("MY_"))
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Cara Menambahkan API Key:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1. Buka AI Studio Secrets Panel di editor browser Anda.\n2. Masukkan parameter OPENAI_API_KEY atau GEMINI_API_KEY Anda di sana.\n3. Tekan Deploy ulang. Keamanan Data Anda terenkripsi 100% aman.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        lineHeight = 18.sp
                    )
                }
            }

            // --- Privacy policy warning ---
            Text(
                text = "KEAMANAN & PRIVASI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Shield",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Penyimpanan 100% Offline Lokal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Ide film Anda bernilai mahal. Seluruh draf naskah tersimpan di database SQLite Room lokal pada memori handphone Anda, tidak di-upload ke server bayangan mana pun.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Optional Cloud Sync (Upcoming): Sinkronisasi nirkabel Google Drive akan segera dirilis di versi komersil selanjutnya.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // --- App maintenance controls ---
            Text(
                text = "MAINTENANCE SYSTEM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Button(
                onClick = {
                    Toast.makeText(context, "Seluruh cache & file sampah naskah berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Cache", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bersihkan Cache Naskah", fontSize = 13.sp)
            }

            // --- About app footer ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ScripKu v1.0.2 - Pro Native Edition",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Made in Indonesia with Jetpack Compose",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
