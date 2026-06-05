package com.example.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.entity.SceneEntity
import com.example.domain.model.ElementType
import com.example.domain.model.ScriptElement
import com.example.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentProject by viewModel.currentProject.collectAsState()
    val scenes by viewModel.scenes.collectAsState()
    val activeScene by viewModel.activeScene.collectAsState()
    val elements by viewModel.editorElements.collectAsState()

    val characters by viewModel.characters.collectAsState()
    val locations by viewModel.locations.collectAsState()

    val editorTheme by viewModel.editorTheme.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiFeedbackContent by viewModel.aiError.collectAsState()
    val isAutoSaving by viewModel.isAutoSaving.collectAsState()

    var showThemeMenu by remember { mutableStateOf(false) }
    var showScenePicker by remember { mutableStateOf(false) }
    var focusedBlockIndex by remember { mutableIntStateOf(-1) }
    var showAiMenuIndex by remember { mutableStateOf<Int?>(null) }
    var showAiGeneralMenu by remember { mutableStateOf(false) }

    // Autocomplete mentions dialog support
    var showMentionSuggestionsForIndex by remember { mutableIntStateOf(-1) }
    var searchMentionQuery by remember { mutableStateOf("") }

    // Theme Color Tokens
    val themeBackground = when (editorTheme) {
        "dark" -> Color(0xFF1C1B1F) // Slate dark canvas
        "light" -> Color(0xFFF8FAFC)
        else -> Color(0xFFF4ECD8) // Sepia Paper
    }
    val themeSurface = when (editorTheme) {
        "dark" -> Color(0xFF2D2F31) // Slate dark toolbar
        "light" -> Color(0xFFFFFFFF)
        else -> Color(0xFFFCF8EC) // Sepia Card
    }
    val themeText = when (editorTheme) {
        "dark" -> Color(0xFFF4EFF4) // Elegant cream-grey text
        "light" -> Color(0xFF0F172A)
        else -> Color(0xFF3E2723) // Coffee brown
    }

    // Auto calculate pages: 1 Page is roughly equal to 850 characters
    val totalChars = elements.sumOf { it.text.length }
    val pageCount = max(1.0, totalChars / 850.0)
    val pageCountFormatted = String.format(Locale.US, "%.1f", pageCount)

    // Trigger auto-focus scroll
    LaunchedEffect(elements.size) {
        if (elements.isNotEmpty()) {
            listState.animateScrollToItem(elements.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeScene?.title ?: "Skrip Editor",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeText,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$pageCountFormatted Halm. (~$pageCountFormatted Menit Film)",
                            fontSize = 11.sp,
                            color = themeText.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = themeText)
                    }
                },
                actions = {
                    // Auto-saving status indicator
                    if (isAutoSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Tersimpan",
                            tint = Color(0xFF10B981),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(18.dp)
                        )
                    }

                    // Undo Button
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Undo", tint = themeText)
                    }

                    // Redo Button
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Redo", tint = themeText)
                    }

                    // Select Page Colors Theme Button
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(Icons.Default.Star, contentDescription = "Warna Halaman", tint = themeText)
                    }

                    // Dropdown theme selecting menu
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Vintage Sepia Paper") },
                            onClick = {
                                viewModel.setEditorTheme("sepia")
                                showThemeMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = "Sepia", tint = Color(0xFFF4ECD8)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Elegant Dark") },
                            onClick = {
                                viewModel.setEditorTheme("dark")
                                showThemeMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = "Dark", tint = Color(0xFF1C1B1F)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Clean Light") },
                            onClick = {
                                viewModel.setEditorTheme("light")
                                showThemeMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = "Light", tint = Color(0xFFF8FAFC)) }
                        )
                    }

                    // Export / Share Script Menu Button
                    IconButton(
                        onClick = {
                            exportAndPrintScreenplay(context, currentProject?.title ?: "Script", viewModel.getSceneFountainText())
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Ekspor", tint = themeText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeBackground
                )
            )
        },
        bottomBar = {
            // SOFT KEYBOARD / EDITING ASSISTANT TOOLBAR at bottom for fast element switching
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeSurface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Formatting bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(themeBackground.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.executeAiCoWriter("CONTINUE") },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "AI Co-Writer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Fast Insert Type Selector Button
                    FastInsertionTypeButton(label = "Scene", active = focusedBlockIndex >= 0 && elements.getOrNull(focusedBlockIndex)?.type == ElementType.SCENE_HEADING) {
                        if (focusedBlockIndex >= 0) viewModel.changeElementType(focusedBlockIndex, ElementType.SCENE_HEADING)
                    }

                    FastInsertionTypeButton(label = "Action", active = focusedBlockIndex >= 0 && elements.getOrNull(focusedBlockIndex)?.type == ElementType.ACTION) {
                        if (focusedBlockIndex >= 0) viewModel.changeElementType(focusedBlockIndex, ElementType.ACTION)
                    }

                    FastInsertionTypeButton(label = "Char", active = focusedBlockIndex >= 0 && elements.getOrNull(focusedBlockIndex)?.type == ElementType.CHARACTER) {
                        if (focusedBlockIndex >= 0) viewModel.changeElementType(focusedBlockIndex, ElementType.CHARACTER)
                    }

                    FastInsertionTypeButton(label = "Dialog", active = focusedBlockIndex >= 0 && elements.getOrNull(focusedBlockIndex)?.type == ElementType.DIALOGUE) {
                        if (focusedBlockIndex >= 0) viewModel.changeElementType(focusedBlockIndex, ElementType.DIALOGUE)
                    }

                    FastInsertionTypeButton(label = "Paren", active = focusedBlockIndex >= 0 && elements.getOrNull(focusedBlockIndex)?.type == ElementType.PARENTHETICAL) {
                        if (focusedBlockIndex >= 0) viewModel.changeElementType(focusedBlockIndex, ElementType.PARENTHETICAL)
                    }

                    FastInsertionTypeButton(label = "Trans", active = focusedBlockIndex >= 0 && elements.getOrNull(focusedBlockIndex)?.type == ElementType.TRANSITION) {
                        if (focusedBlockIndex >= 0) viewModel.changeElementType(focusedBlockIndex, ElementType.TRANSITION)
                    }
                }

                // Scene Selection & Creation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(themeBackground)
                            .clickable { showScenePicker = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Scenes", tint = themeText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scene Arus (" + (scenes.indexOfFirst { it.id == activeScene?.id } + 1) + "/" + scenes.size + ")",
                            fontSize = 12.sp,
                            color = themeText,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.addElement(ElementType.ACTION, "") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Element", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Baris Baru", fontSize = 12.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(themeBackground)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
            ) {
                if (elements.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Mulai baris baru. Ketuk tombol 'Baris Baru' di kanan bawah untuk mulai merangkai naskah.",
                                color = themeText.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                }

                itemsIndexed(elements) { index, element ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (focusedBlockIndex == index) themeSurface.copy(alpha = 0.4f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (focusedBlockIndex == index) 1.dp else 0.dp,
                                color = if (focusedBlockIndex == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp)
                    ) {
                        // Element Header controller panel
                        if (focusedBlockIndex == index) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = element.type.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Monospace
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Move Up
                                    IconButton(onClick = { viewModel.moveElementUp(index) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Naik", tint = themeText, modifier = Modifier.size(16.dp))
                                    }
                                    // Move Down
                                    IconButton(onClick = { viewModel.moveElementDown(index) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Turun", tint = themeText, modifier = Modifier.size(16.dp))
                                    }
                                    // IA Helper specific for dialogue
                                    if (element.type == ElementType.DIALOGUE) {
                                        IconButton(
                                            onClick = {
                                                viewModel.executeAiCoWriter("IMPROVE", index)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = "AI Poles Dialog", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    // Delete element block
                                    IconButton(onClick = { viewModel.removeElement(index) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Text input field configured precisely corresponding to the element type specifications
                        val paddingModifier = when (element.type) {
                            ElementType.SCENE_HEADING -> Modifier
                                .fillMaxWidth()
                                .background(themeText.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                            ElementType.CHARACTER -> Modifier
                                .fillMaxWidth(0.6f)
                                .align(Alignment.CenterHorizontally)
                            ElementType.DIALOGUE -> Modifier
                                .fillMaxWidth(0.82f)
                                .align(Alignment.CenterHorizontally)
                            ElementType.PARENTHETICAL -> Modifier
                                .fillMaxWidth(0.5f)
                                .align(Alignment.CenterHorizontally)
                            ElementType.TRANSITION -> Modifier
                                .fillMaxWidth(0.4f)
                                .align(Alignment.End)
                            ElementType.ACTION -> Modifier.fillMaxWidth()
                        }

                        val textAlign = when (element.type) {
                            ElementType.CHARACTER, ElementType.DIALOGUE, ElementType.PARENTHETICAL -> TextAlign.Center
                            ElementType.TRANSITION -> TextAlign.End
                            else -> TextAlign.Start
                        }

                        val textStyle = when (element.type) {
                            ElementType.SCENE_HEADING -> androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = themeText
                            )
                            ElementType.CHARACTER -> androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = themeText
                            )
                            ElementType.DIALOGUE -> androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = themeText
                            )
                            ElementType.PARENTHETICAL -> androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontStyle = FontStyle.Italic,
                                fontSize = 13.sp,
                                color = themeText.copy(alpha = 0.8f)
                            )
                            ElementType.TRANSITION -> androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = themeText
                            )
                            ElementType.ACTION -> androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = themeText
                            )
                        }

                        BasicTextField(
                            value = element.text,
                            onValueChange = { input ->
                                viewModel.updateElementText(index, input)
                                if (input.endsWith("@") && (element.type == ElementType.DIALOGUE || element.type == ElementType.CHARACTER)) {
                                    showMentionSuggestionsForIndex = index
                                    searchMentionQuery = ""
                                } else if (showMentionSuggestionsForIndex == index) {
                                    if (!input.contains("@")) {
                                        showMentionSuggestionsForIndex = -1
                                    } else {
                                        searchMentionQuery = input.substringAfterLast("@")
                                    }
                                }
                            },
                            textStyle = textStyle.copy(textAlign = textAlign),
                            keyboardOptions = KeyboardOptions(
                                capitalization = if (element.type == ElementType.CHARACTER || element.type == ElementType.SCENE_HEADING)
                                    KeyboardCapitalization.Characters
                                else
                                    KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Default
                            ),
                            modifier = paddingModifier.clickable {
                                focusedBlockIndex = index
                            }
                        )

                        // Trigger mention popups for fast matching characters insertion
                        if (showMentionSuggestionsForIndex == index && characters.isNotEmpty()) {
                            val filteredChars = characters.filter {
                                it.name.contains(searchMentionQuery, ignoreCase = true)
                            }
                            if (filteredChars.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredChars.take(3).forEach { character ->
                                        AssistChip(
                                            onClick = {
                                                val originalText = element.text.substringBeforeLast("@")
                                                viewModel.updateElementText(index, originalText + character.name + " ")
                                                showMentionSuggestionsForIndex = -1
                                            },
                                            label = { Text(character.name) },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = themeSurface)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI Loading progress overlay indicator
            if (aiLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .padding(32.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "ScripKu Sedang Mengetik...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // --- Dynamic Sidebar/Dialog Scene Picker list ---
    if (showScenePicker) {
        AlertDialog(
            onDismissRequest = { showScenePicker = false },
            title = { Text("Pilih Scene Naskah", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(scenes) { idx, item ->
                        Card(
                            onClick = {
                                viewModel.selectScene(item)
                                showScenePicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.id == activeScene?.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${idx + 1}. " + item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (item.summary.isNotEmpty()) {
                                        Text(
                                            text = item.summary,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (item.id == activeScene?.id) {
                                    Icon(Icons.Default.Check, contentDescription = "Dipilih", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showScenePicker = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // --- AI Brainstorming Result Popup ---
    if (aiFeedbackContent != null && !aiFeedbackContent!!.startsWith("Koneksi Bermasalah")) {
        AlertDialog(
            onDismissRequest = { viewModel.clearAiFeedback() },
            icon = { Icon(Icons.Default.Star, contentDescription = "AI Brainstorm", tint = Color(0xFFF59E0B)) },
            title = { Text("AI Co-Writer Brainstorm", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.heightIn(max = 320.dp)) {
                    LazyColumn {
                        item {
                            Text(
                                text = aiFeedbackContent ?: "",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearAiFeedback() }) {
                    Text("Tutup")
                }
            }
        )
    } else if (aiFeedbackContent != null) {
        // Error Snackbar notification
        LaunchedEffect(aiFeedbackContent) {
            viewModel.clearAiFeedback()
        }
    }
}

@Composable
fun FastInsertionTypeButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
    }
}

// BasicTextField helper wrapper supporting dynamic soft-keyboard setups
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = false,
        maxLines = 8,
        placeholder = {
            Text(
                "Tulis dialog, karakter, atau scene di sini...",
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Monospace,
                color = textStyle.color.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = textStyle.textAlign
            )
        }
    )
}

/**
 * Handles exporting Fountain raw text AND utilizing Android System Print Services
 * to instantly prompt native PDF Conversion!
 */
fun exportAndPrintScreenplay(context: Context, scriptTitle: String, rawContent: String) {
    try {
        // 1. Save Fountain raw text locally so user can export & share .fountain
        val cacheDir = context.cacheDir
        val filename = scriptTitle.replace(" ", "_").lowercase(Locale.getDefault()) + ".fountain"
        val tempFile = File(cacheDir, filename)
        val writer = FileWriter(tempFile)
        writer.write(rawContent)
        writer.close()

        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            tempFile
        )

        // 2. Share Intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "text/plain"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // 3. Native print command - HTML template styling
        val formattedHtml = buildString {
            append("<html><head><style>")
            append("@page { size: letter; margin: 1in; }")
            append("body { font-family: 'Courier New', Courier, monospace; font-size: 12pt; line-height: 1.5; background-color: white; color: black; padding: 1.25in; }")
            append(".scene-heading { font-weight: bold; text-transform: uppercase; margin-top: 24px; margin-bottom: 12px; }")
            append(".action { text-align: left; margin-bottom: 16px; margin-right: 1.5in; }")
            append(".character { text-align: center; text-transform: uppercase; margin-top: 20px; margin-bottom: 4px; padding-left: 2in; padding-right: 2in; }")
            append(".parenthetical { text-align: center; font-style: italic; margin-bottom: 4px; padding-left: 2.2in; padding-right: 2.2in; }")
            append(".dialogue { text-align: left; margin-bottom: 16px; padding-left: 2.5in; padding-right: 2.5in; }")
            append(".transition { text-align: right; text-transform: uppercase; font-weight: bold; margin-top: 18px; margin-bottom: 18px; }")
            append("</style></head><body>")
            
            // Format elements to HTML
            val elements = ScriptElement.listFromJsonString(rawContent)
            val list = if (elements.isNotEmpty()) elements else {
                rawContent.split("\n\n").map { block ->
                    val lines = block.split("\n")
                    when {
                        block.startsWith("INT.") || block.startsWith("EXT.") -> ScriptElement(ElementType.SCENE_HEADING, block)
                        lines.size == 1 && block == block.uppercase() -> ScriptElement(ElementType.CHARACTER, block)
                        block.startsWith("(") -> ScriptElement(ElementType.PARENTHETICAL, block)
                        else -> ScriptElement(ElementType.ACTION, block)
                    }
                }
            }

            for (element in list) {
                when (element.type) {
                    ElementType.SCENE_HEADING -> append("<div class='scene-heading'>${element.text}</div>")
                    ElementType.ACTION -> append("<div class='action'>${element.text}</div>")
                    ElementType.CHARACTER -> append("<div class='character'>${element.text}</div>")
                    ElementType.DIALOGUE -> append("<div class='dialogue'>${element.text}</div>")
                    ElementType.PARENTHETICAL -> append("<div class='parenthetical'>${element.text}</div>")
                    ElementType.TRANSITION -> append("<div class='transition'>${element.text}</div>")
                }
            }
            append("</body></html>")
        }

        // Custom WebView Print adapter setup
        val webView = android.webkit.WebView(context)
        webView.loadDataWithBaseURL(null, formattedHtml, "text/html", "UTF-8", null)
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(scriptTitle)
                val jobName = "${scriptTitle}_Naskah"
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }

        // Offer File Sharing directly as fallback
        context.startActivity(Intent.createChooser(shareIntent, "Buka atau Bagikan Naskah"))

    } catch (e: Exception) {
        e.printStackTrace()
    }
}
