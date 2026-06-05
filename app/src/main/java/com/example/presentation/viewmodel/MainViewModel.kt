package com.example.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SceneEntity
import com.example.data.remote.AiRepository
import com.example.data.repository.ScriptRepository
import com.example.domain.model.ElementType
import com.example.domain.model.ScriptElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScriptRepository
    private val aiRepository: AiRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ScriptRepository(db.projectDao(), db.sceneDao(), db.characterDao(), db.locationDao())
        aiRepository = AiRepository(application)
    }

    // --- State & Streams ---
    val projects: StateFlow<List<ProjectEntity>> = repository.getAllProjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentProject = MutableStateFlow<ProjectEntity?>(null)
    val currentProject: StateFlow<ProjectEntity?> = _currentProject.asStateFlow()

    private val _scenes = MutableStateFlow<List<SceneEntity>>(emptyList())
    val scenes: StateFlow<List<SceneEntity>> = _scenes.asStateFlow()

    private val _characters = MutableStateFlow<List<CharacterEntity>>(emptyList())
    val characters: StateFlow<List<CharacterEntity>> = _characters.asStateFlow()

    private val _locations = MutableStateFlow<List<LocationEntity>>(emptyList())
    val locations: StateFlow<List<LocationEntity>> = _locations.asStateFlow()

    // --- Active Editor State ---
    private val _activeScene = MutableStateFlow<SceneEntity?>(null)
    val activeScene: StateFlow<SceneEntity?> = _activeScene.asStateFlow()

    private val _editorElements = MutableStateFlow<List<ScriptElement>>(emptyList())
    val editorElements: StateFlow<List<ScriptElement>> = _editorElements.asStateFlow()

    // Undo / Redo Stacks
    private val undoStack = Stack<List<ScriptElement>>()
    private val redoStack = Stack<List<ScriptElement>>()

    // Autocomplete / Shortcut support
    private val _isAutoSaving = MutableStateFlow(false)
    val isAutoSaving: StateFlow<Boolean> = _isAutoSaving.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // Jobs
    private var autoSaveJob: Job? = null
    private var streamsJob: Job? = null

    // For Theme management (default light, supports light, dark, sepia)
    private val _editorTheme = MutableStateFlow("sepia") // "dark", "light", "sepia"
    val editorTheme: StateFlow<String> = _editorTheme.asStateFlow()

    fun setEditorTheme(theme: String) {
        _editorTheme.value = theme
    }

    // --- Selection and Loading of Project ---
    fun selectProject(project: ProjectEntity) {
        _currentProject.value = project
        
        // Cancel previous flows and observe new project details
        streamsJob?.cancel()
        streamsJob = viewModelScope.launch {
            // Scenes list
            launch {
                repository.getScenesForProjectFlow(project.id).collect {
                    _scenes.value = it
                    // Auto select first scene if none is selected
                    if (_activeScene.value == null && it.isNotEmpty()) {
                        selectScene(it.first())
                    }
                }
            }
            // Characters list
            launch {
                repository.getCharactersForProjectFlow(project.id).collect {
                    _characters.value = it
                }
            }
            // Locations list
            launch {
                repository.getLocationsForProjectFlow(project.id).collect {
                    _locations.value = it
                }
            }
        }
    }

    fun selectScene(scene: SceneEntity) {
        // Save previous active scene if any
        viewModelScope.launch {
            saveCurrentSceneImmediate()
            _activeScene.value = scene
            _editorElements.value = ScriptElement.listFromJsonString(scene.contentJson)
            
            // Clear undo/redo stacks for new scene context
            undoStack.clear()
            redoStack.clear()

            // Trigger autosaver
            startAutoSaveTimer()
        }
    }

    // --- Create Project with Structure Templates ---
    fun createProject(
        title: String,
        logline: String,
        genre: String,
        targetDuration: Int,
        structureType: String
    ) {
        viewModelScope.launch {
            val project = ProjectEntity(
                title = title.ifEmpty { "Naskah Baru" },
                logline = logline.ifEmpty { "Logline belum dibuat." },
                genre = genre.ifEmpty { "Drama" },
                targetDuration = if (targetDuration <= 0) 90 else targetDuration,
                structureType = structureType
            )
            val projectId = repository.insertProject(project).toInt()
            val createdProject = project.copy(id = projectId)
            
            // Insert standard template scenes based on selection
            val templateScenes = getTemplateScenesForStructure(projectId, structureType)
            repository.insertScenes(templateScenes)

            // Insert 3 mock/sample characters if first project as demo samples!
            val sampleChars = listOf(
                CharacterEntity(projectId = projectId, name = "REZA (30)", age = "30", bio = "Penulis ambisius yang terjebak utang.", motivation = "Ingin membuktikan naskahnya bernilai milyaran."),
                CharacterEntity(projectId = projectId, name = "ALETHA (28)", age = "28", bio = "Produser film idealis tapi sinis.", motivation = "Mencari skrip jenius berikutnya untuk menyelamatkan studionya."),
                CharacterEntity(projectId = projectId, name = "BENTO (45)", age = "45", bio = "Pemberi dana bayaran dengan selera humor gelap.", motivation = "Mendapatkan uangnya kembali dengan segala cara.")
            )
            sampleChars.forEach { repository.insertCharacter(it) }

            // Insert sample Locations
            val sampleLocs = listOf(
                LocationEntity(projectId = projectId, name = "KAFE SUNNY", type = "INT", timeOfDay = "SORE", description = "Kafe retro dengan aroma kopi pekat dan piringan hitam klasik berputar."),
                LocationEntity(projectId = projectId, name = "JALANAN KOTA MALANG", type = "EXT", timeOfDay = "MALAM", description = "Lampu kota temaram, aspal basah karena hujan rintik-rintik.")
            )
            sampleLocs.forEach { repository.insertLocation(it) }

            // Auto-select the newly created project
            selectProject(createdProject)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = null
                _scenes.value = emptyList()
                _activeScene.value = null
                _editorElements.value = emptyList()
            }
        }
    }

    private fun getTemplateScenesForStructure(projectId: Int, structure: String): List<SceneEntity> {
        val list = mutableListOf<SceneEntity>()
        when (structure) {
            "3_act" -> {
                list.add(SceneEntity(projectId = projectId, title = "INT. LOKASI AWAL - PAGI", sequenceNumber = 1, summary = "Babak I: Pengenalan Karakter & Status Quo", contentJson = getSampleScriptElementsJson(1)))
                list.add(SceneEntity(projectId = projectId, title = "EXT. JALAN RAYA - SIANG", sequenceNumber = 2, summary = "Babak I: Kejadian Pemicu (Inciting Incident)", contentJson = getSampleScriptElementsJson(2)))
                list.add(SceneEntity(projectId = projectId, title = "INT. KANTOR PRODUSER - SORE", sequenceNumber = 3, summary = "Babak II: Menyeberang Ambang Pintu Pertama", contentJson = getSampleScriptElementsJson(3)))
                list.add(SceneEntity(projectId = projectId, title = "INT. RUANG EMAS - MALAM", sequenceNumber = 4, summary = "Babak II: Titik Tengah Cerita (Midpoint) & Konsekuensi", contentJson = "[]"))
                list.add(SceneEntity(projectId = projectId, title = "EXT. TEBING CURAM - SORE", sequenceNumber = 5, summary = "Babak III: Klimaks & Resolusi Terakhir", contentJson = "[]"))
            }
            "heros_journey" -> {
                list.add(SceneEntity(projectId = projectId, title = "INT. DESA BIASA - AMBIENT", sequenceNumber = 1, summary = "Dunia Biasa (The Ordinary World)", contentJson = "[]"))
                list.add(SceneEntity(projectId = projectId, title = "EXT. GERBANG DESA - FAJAR", sequenceNumber = 2, summary = "Panggilan Berpetualang & Penolakan", contentJson = "[]"))
                list.add(SceneEntity(projectId = projectId, title = "INT. GOA TUA - MALAM", sequenceNumber = 3, summary = "Pertemuan dengan Mentor (Meeting the Mentor)", contentJson = "[]"))
                list.add(SceneEntity(projectId = projectId, title = "EXT. GURUN PASIR - SIANG", sequenceNumber = 4, summary = "Ujian, Teman, dan Musuh Pertama", contentJson = "[]"))
                list.add(SceneEntity(projectId = projectId, title = "INT. ISTANA MERAH - MALAM", sequenceNumber = 5, summary = "Pertempuran Besar & Kebangkitan Baru", contentJson = "[]"))
            }
            "save_the_cat" -> {
                list.add(SceneEntity(projectId = projectId, title = "INT. KAMAR UTAMA - PAGI", sequenceNumber = 1, summary = "Opening Image & Pernyataan Tema", contentJson = "[]"))
                list.add(SceneEntity(projectId = projectId, title = "EXT. KANTOR POS - SIANG", sequenceNumber = 2, summary = "Setup & Katalis Utama", contentJson = "[]"))
                list.add(SceneEntity(projectId = projectId, title = "INT. KAMPUS - SORE", sequenceNumber = 3, summary = "Debat & Masuk Babak Baru", contentJson = "[]"))
                list.add(SceneEntity(projectId = projectId, title = "INT. KAFE - MALAM", sequenceNumber = 4, summary = "Fun and Games & Kisah Sampingan (B Story)", contentJson = "[]"))
                list.add(SceneEntity(projectId = projectId, title = "EXT. ATAP GEDUNG - HUJAN", sequenceNumber = 5, summary = "Semua Hilang (All Is Lost) & Finale", contentJson = "[]"))
            }
            else -> {
                // Blank / Free
                list.add(SceneEntity(projectId = projectId, title = "INT. LOKASI BARU - SIANG", sequenceNumber = 1, summary = "Scene pertama Anda", contentJson = getSampleScriptElementsJson(1)))
            }
        }
        return list
    }

    private fun getSampleScriptElementsJson(index: Int): String {
        val elements = when (index) {
            1 -> listOf(
                ScriptElement(ElementType.SCENE_HEADING, "INT. KAFE SUNNY - SORE"),
                ScriptElement(ElementType.ACTION, "REZA mengaduk kopi hitamnya dengan lesu. Ia menatap layar laptop tua berstiker lusuh di hadapannya. Halaman naskahnya masih kosong melongpong."),
                ScriptElement(ElementType.CHARACTER, "REZA"),
                ScriptElement(ElementType.DIALOGUE, "Satu jam lagi Aletha dateng, dan gue belum nulis satu baris pun. Mati gue malam ini."),
                ScriptElement(ElementType.ACTION, "Dari belakang, barista ramah meletakkan secangkir cappuccino hangat di hadapan Reza dengan senyuman ceria.")
            )
            2 -> listOf(
                ScriptElement(ElementType.SCENE_HEADING, "EXT. JALAN RAYA MALANG - SORE"),
                ScriptElement(ElementType.ACTION, "Hujan deras mengguyur aspal hitam kota. Reza berlari kencang menerobos rintik air, memeluk jaket denimnya erat-erat melindungi tas punggung berisi skrip."),
                ScriptElement(ElementType.CHARACTER, "REZA"),
                ScriptElement(ElementType.PARENTHETICAL, "(berteriak parau)"),
                ScriptElement(ElementType.DIALOGUE, "Woi! Tunggu! Tas saya jangan dibawa kabur!"),
                ScriptElement(ElementType.TRANSITION, "CUT TO:")
            )
            3 -> listOf(
                ScriptElement(ElementType.SCENE_HEADING, "INT. KANTOR PRODUSER - SORE"),
                ScriptElement(ElementType.ACTION, "ALETHA melipat tangannya di dada, duduk di balik meja kayu jati berukuran besar penuh piala penghargaan. Matanya terpaku menembus kacamata bulatnya."),
                ScriptElement(ElementType.CHARACTER, "ALETHA"),
                ScriptElement(ElementType.DIALOGUE, "Ceritamu ini sudah kuno, Rez. Penonton sekarang butuh plot twist yang bikin jantungan, bukan kisah romantis cengeng."),
                ScriptElement(ElementType.CHARACTER, "REZA"),
                ScriptElement(ElementType.DIALOGUE, "Tapi Tha, idealisme cinta di film ini punya basis emosional kuat.")
            )
            else -> emptyList()
        }
        return ScriptElement.listToJsonString(elements)
    }

    // --- Editor Operations ---
    fun updateElementsList(newElements: List<ScriptElement>) {
        // Save current to undo stack before editing
        if (_editorElements.value != newElements) {
            saveToUndoStack(_editorElements.value)
            _editorElements.value = newElements
            redoStack.clear()
        }
    }

    fun addElement(type: ElementType, text: String = "") {
        val current = _editorElements.value.toMutableList()
        current.add(ScriptElement(type, text))
        updateElementsList(current)
    }

    fun updateElementText(index: Int, newText: String) {
        val current = _editorElements.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(text = newText)
            
            // Bypass undo push for continuous letter-typing to avoid bloating undo stack
            _editorElements.value = current
        }
    }

    fun changeElementType(index: Int, newType: ElementType) {
        val current = _editorElements.value.toMutableList()
        if (index in current.indices) {
            val oldElement = current[index]
            var formattedText = oldElement.text
            if (newType == ElementType.CHARACTER) {
                formattedText = formattedText.uppercase(Locale.getDefault())
            } else if (newType == ElementType.SCENE_HEADING) {
                formattedText = formattedText.uppercase(Locale.getDefault())
                if (!formattedText.startsWith("INT.") && !formattedText.startsWith("EXT.")) {
                    formattedText = "INT. " + formattedText
                }
            }
            current[index] = ScriptElement(newType, formattedText)
            updateElementsList(current)
        }
    }

    fun removeElement(index: Int) {
        val current = _editorElements.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            updateElementsList(current)
        }
    }

    fun moveElementUp(index: Int) {
        val current = _editorElements.value.toMutableList()
        if (index > 0 && index in current.indices) {
            Collections.swap(current, index, index - 1)
            updateElementsList(current)
        }
    }

    fun moveElementDown(index: Int) {
        val current = _editorElements.value.toMutableList()
        if (index in current.indices && index < current.size - 1) {
            Collections.swap(current, index, index + 1)
            updateElementsList(current)
        }
    }

    // Undo / Redo
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.pop()
            redoStack.push(_editorElements.value)
            _editorElements.value = prev
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.pop()
            undoStack.push(_editorElements.value)
            _editorElements.value = next
        }
    }

    private fun saveToUndoStack(state: List<ScriptElement>) {
        if (undoStack.isEmpty() || undoStack.peek() != state) {
            undoStack.push(state.toList())
            if (undoStack.size > 50) {
                undoStack.removeAt(0)
            }
        }
    }

    // --- Auto-Save Mechanism (Runs background coroutine every 2 seconds) ---
    private fun startAutoSaveTimer() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                saveCurrentSceneImmediate()
            }
        }
    }

    suspend fun saveCurrentSceneImmediate() {
        val scene = _activeScene.value ?: return
        val currentElements = _editorElements.value
        val currentJson = ScriptElement.listToJsonString(currentElements)
        
        if (scene.contentJson != currentJson) {
            _isAutoSaving.value = true
            val updatedScene = scene.copy(contentJson = currentJson)
            repository.updateScene(updatedScene)
            _activeScene.value = updatedScene
            delay(300) // visual feedback padding
            _isAutoSaving.value = false
        }
    }

    // --- Scene Management inside Project ---
    fun createScene(title: String, summary: String = "") {
        val prj = _currentProject.value ?: return
        viewModelScope.launch {
            val scs = _scenes.value
            val nextSeq = if (scs.isEmpty()) 1 else (scs.maxOf { it.sequenceNumber } + 1)
            val newScene = SceneEntity(
                projectId = prj.id,
                title = title.ifEmpty { "INT. SCENE BARU - SIANG" }.uppercase(Locale.getDefault()),
                sequenceNumber = nextSeq,
                summary = summary.ifEmpty { "Aksi scene di lokasi baru." },
                contentJson = "[]"
            )
            val sceneId = repository.insertScene(newScene).toInt()
            val createdScene = newScene.copy(id = sceneId)
            
            // Auto select newly created scene
            selectScene(createdScene)
        }
    }

    fun deleteScene(scene: SceneEntity) {
        viewModelScope.launch {
            repository.deleteScene(scene)
            if (_activeScene.value?.id == scene.id) {
                _activeScene.value = null
                _editorElements.value = emptyList()
            }
            reorderSceneSequence()
        }
    }

    fun moveSceneUp(scene: SceneEntity) {
        viewModelScope.launch {
            val list = _scenes.value.toMutableList()
            val index = list.indexOfFirst { it.id == scene.id }
            if (index > 0) {
                Collections.swap(list, index, index - 1)
                // Persist new sequence order
                for (i in list.indices) {
                    repository.updateScene(list[i].copy(sequenceNumber = i + 1))
                }
            }
        }
    }

    fun moveSceneDown(scene: SceneEntity) {
        viewModelScope.launch {
            val list = _scenes.value.toMutableList()
            val index = list.indexOfFirst { it.id == scene.id }
            if (index in 0 until list.size - 1) {
                Collections.swap(list, index, index + 1)
                // Persist new sequence order
                for (i in list.indices) {
                    repository.updateScene(list[i].copy(sequenceNumber = i + 1))
                }
            }
        }
    }

    private suspend fun reorderSceneSequence() {
        val list = repository.getScenesForProject(_currentProject.value?.id ?: return)
        for (i in list.indices) {
            repository.updateScene(list[i].copy(sequenceNumber = i + 1))
        }
    }

    // --- Characters Management ---
    fun addCharacter(name: String, age: String, bio: String, motivation: String) {
        val prj = _currentProject.value ?: return
        viewModelScope.launch {
            val char = CharacterEntity(
                projectId = prj.id,
                name = name.uppercase(Locale.getDefault()),
                age = age,
                bio = bio,
                motivation = motivation
            )
            repository.insertCharacter(char)
        }
    }

    fun deleteCharacter(character: CharacterEntity) {
        viewModelScope.launch {
            repository.deleteCharacter(character)
        }
    }

    // --- Locations Management ---
    fun addLocation(name: String, type: String, timeOfDay: String, description: String) {
        val prj = _currentProject.value ?: return
        viewModelScope.launch {
            val loc = LocationEntity(
                projectId = prj.id,
                name = name.uppercase(Locale.getDefault()),
                type = type,
                timeOfDay = timeOfDay,
                description = description
            )
            repository.insertLocation(loc)
        }
    }

    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }

    // --- AI Co-Writer Execution ---
    fun executeAiCoWriter(mode: String, targetElementIndex: Int? = null) {
        val activePrj = _currentProject.value ?: return
        _aiLoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            try {
                // Compile context from editor naskah
                val rawNaskah = getSceneFountainText()
                val contextText = "Genre: ${activePrj.genre}. Logline: ${activePrj.logline}"
                val charNames = _characters.value.map { it.name }

                val response = aiRepository.generateAiContent(mode, rawNaskah, contextText, charNames)

                if (mode == "CONTINUE") {
                    // Inject AI result to end or selection
                    val generatedElements = parsePlainFountainToElements(response)
                    val current = _editorElements.value.toMutableList()
                    current.addAll(generatedElements)
                    updateElementsList(current)
                } else if (mode == "IMPROVE" && targetElementIndex != null) {
                    // Overwrite targeted dialogue with improved dialog
                    val current = _editorElements.value.toMutableList()
                    if (targetElementIndex in current.indices) {
                        current[targetElementIndex] = current[targetElementIndex].copy(text = response)
                        updateElementsList(current)
                    }
                } else {
                    // Brainstorming - output as a nice action notification or Dialog box content
                    _aiError.value = response // Use error flow temporarily to hold AI output dialog payload!
                }
            } catch (e: Exception) {
                _aiError.value = "Koneksi Bermasalah: ${e.message ?: "Gagal memanggil AI. Silakan coba lagi."}"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun clearAiFeedback() {
        _aiError.value = null
    }

    // Parse plain screenplay format text back into ScriptElements for standard editor loading
    private fun parsePlainFountainToElements(text: String): List<ScriptElement> {
        val lines = text.split("\n")
        val result = mutableListOf<ScriptElement>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            
            when {
                trimmed.startsWith("INT.") || trimmed.startsWith("EXT.") -> {
                    result.add(ScriptElement(ElementType.SCENE_HEADING, trimmed.uppercase(Locale.getDefault())))
                }
                trimmed == trimmed.uppercase(Locale.getDefault()) && trimmed.length < 30 && !trimmed.endsWith(":") -> {
                    result.add(ScriptElement(ElementType.CHARACTER, trimmed))
                }
                trimmed.startsWith("(") && trimmed.endsWith(")") -> {
                    result.add(ScriptElement(ElementType.PARENTHETICAL, trimmed))
                }
                trimmed.endsWith("TO:") || trimmed.startsWith("FADE ") -> {
                    result.add(ScriptElement(ElementType.TRANSITION, trimmed.uppercase(Locale.getDefault())))
                }
                // If previous was character or parenthetical, then it's a dialogue!
                result.isNotEmpty() && (result.last().type == ElementType.CHARACTER || result.last().type == ElementType.PARENTHETICAL) -> {
                    result.add(ScriptElement(ElementType.DIALOGUE, trimmed))
                }
                else -> {
                    result.add(ScriptElement(ElementType.ACTION, trimmed))
                }
            }
        }
        return result.ifEmpty { listOf(ScriptElement(ElementType.ACTION, text)) }
    }

    // Format elements as a standard Fountain Plain-Text
    fun getSceneFountainText(): String {
        return _editorElements.value.joinToString("\n\n") { element ->
            when (element.type) {
                ElementType.SCENE_HEADING -> element.text.uppercase(Locale.getDefault())
                ElementType.ACTION -> element.text
                ElementType.CHARACTER -> element.text.uppercase(Locale.getDefault())
                ElementType.DIALOGUE -> element.text
                ElementType.PARENTHETICAL -> if (element.text.startsWith("(")) element.text else "(${element.text})"
                ElementType.TRANSITION -> element.text.uppercase(Locale.getDefault())
            }
        }
    }

    // Overrides
    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        streamsJob?.cancel()
    }
}
