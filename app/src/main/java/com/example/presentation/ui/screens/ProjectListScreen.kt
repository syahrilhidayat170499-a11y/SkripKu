package com.example.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProjectEntity
import com.example.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectListScreen(
    viewModel: MainViewModel,
    onProjectSelected: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedProjectForDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ScripKu",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "AI Screenwriting & Script Companion",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat Naskah Baru", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (projects.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Movie Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Belum Ada Naskah",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ketuk tombol + di bawah untuk merancang naskah film atau skrip video baru dengan asisten AI.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "NASKAH AKTIF ANDA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                    }

                    items(projects, key = { it.id }) { item ->
                        ProjectItemCard(
                            project = item,
                            onClick = {
                                viewModel.selectProject(item)
                                onProjectSelected()
                            },
                            onLongClick = {
                                selectedProjectForDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Create Project Dialog ---
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, logline, genre, duration, structure ->
                viewModel.createProject(title, logline, genre, duration, structure)
                showCreateDialog = false
                onProjectSelected()
            }
        )
    }

    // --- Delete Confirm Dialog ---
    if (selectedProjectForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedProjectForDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Naskah?") },
            text = { Text("Apakah Anda yakin ingin menghapus proyek naskah \"${selectedProjectForDelete?.title}\"? Seluruh scene, dialog, karakter, dan data lokal akan hilang selamanya.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedProjectForDelete?.let {
                            viewModel.deleteProject(it)
                        }
                        selectedProjectForDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProjectForDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectItemCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val timeString = formatter.format(Date(project.createdAt))

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.genre.uppercase(Locale.getDefault()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = timeString,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = project.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = project.logline,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Durasi",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${project.targetDuration} Menit Film",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                val templateLabel = when (project.structureType) {
                    "3_act" -> "3 Act Structure"
                    "heros_journey" -> "Hero's Journey"
                    "save_the_cat" -> "Save the Cat!"
                    else -> "Blank / Bebas"
                }

                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = templateLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var logline by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Drama") }
    var durationText by remember { mutableStateOf("90") }
    var selectedStructure by remember { mutableStateOf("blank") }

    val genres = listOf("Drama", "Thriller", "Komedi", "Aksi", "Sci-Fi", "Romantis", "Horor", "Edukasi")
    var genreDropdownExpanded by remember { mutableStateOf(false) }

    val structures = listOf(
        "blank" to "Blank Template (Mulai Bebas)",
        "3_act" to "Three-Act Structure (Klimaks Klasik)",
        "heros_journey" to "Hero's Journey (Perjalanan Pahlawan)",
        "save_the_cat" to "Save the Cat! (Beat Screenplay Populer)"
    )
    var structureDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rancang Project Naskah Baru", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Film / Skrip") },
                        placeholder = { Text("Contoh: Hujan Di Bulan Juni") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = logline,
                        onValueChange = { logline = it },
                        label = { Text("Logline (Ringkasan Cerita)") },
                        placeholder = { Text("Contoh: Seorang gadis desa berusaha mengungkap rahasia...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Genre Dropdown Box
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = genreDropdownExpanded,
                                onExpandedChange = { genreDropdownExpanded = !genreDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedGenre,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Genre") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreDropdownExpanded) },
                                    modifier = Modifier.menuAnchor(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = genreDropdownExpanded,
                                    onDismissRequest = { genreDropdownExpanded = false }
                                ) {
                                    genres.forEach { genre ->
                                        DropdownMenuItem(
                                            text = { Text(genre) },
                                            onClick = {
                                                selectedGenre = genre
                                                genreDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Target duration
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { durationText = it.filter { c -> c.isDigit() } },
                            label = { Text("Durasi (menit)") },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true
                        )
                    }
                }

                item {
                    // Structure Dropdown Box
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = structureDropdownExpanded,
                            onExpandedChange = { structureDropdownExpanded = !structureDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = structures.firstOrNull { it.first == selectedStructure }?.second ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Template Struktur Naskah") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = structureDropdownExpanded) },
                                modifier = Modifier.menuAnchor(),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = structureDropdownExpanded,
                                onDismissRequest = { structureDropdownExpanded = false }
                            ) {
                                structures.forEach { pair ->
                                    DropdownMenuItem(
                                        text = { Text(pair.second) },
                                        onClick = {
                                            selectedStructure = pair.first
                                            structureDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDuration = durationText.toIntOrNull() ?: 90
                    onCreate(title, logline, selectedGenre, finalDuration, selectedStructure)
                },
                enabled = title.trim().isNotEmpty()
            ) {
                Text("Buat Sekarang")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
