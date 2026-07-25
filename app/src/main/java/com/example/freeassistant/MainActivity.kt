package com.example.freeassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.speech.RecognizerIntent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.freeassistant.tasks.CurrentTimeTask
import com.example.freeassistant.tasks.HelpTask
import com.example.freeassistant.tasks.ImportNotesTask
import com.example.freeassistant.tasks.IndexPhotosTask
import com.example.freeassistant.tasks.IntentTranslator
import com.example.freeassistant.tasks.ListPhotoTagsTask
import com.example.freeassistant.tasks.ListTagsTask
import com.example.freeassistant.tasks.OpenAppTask
import com.example.freeassistant.tasks.ResultItem
import com.example.freeassistant.tasks.SearchNotesTask
import com.example.freeassistant.tasks.SearchPhotosByDescriptionTask
import com.example.freeassistant.tasks.SearchPhotosByNameTask
import com.example.freeassistant.tasks.SetAlarmTask
import com.example.freeassistant.tasks.SetTimerTask
import com.example.freeassistant.tasks.TaskAction
import com.example.freeassistant.tasks.TaskHandler
import com.example.freeassistant.tasks.TaskResult
import com.example.freeassistant.tasks.WeatherTask
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : BaseActivity() {

    private lateinit var app: App
    private lateinit var handlers: List<TaskHandler>
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var navigationView: NavigationView
    private lateinit var inputEditText: EditText
    private lateinit var micButton: ImageButton
    private lateinit var sendButton: FloatingActionButton
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var voiceManager: VoiceManager

    private var lastInput: String? = null

    @Volatile
    private var isIndexing = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val granted = grants.entries.any { entry ->
                val relevantPermission =
                    entry.key == Manifest.permission.READ_MEDIA_IMAGES ||
                            entry.key == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED ||
                            entry.key == Manifest.permission.READ_EXTERNAL_STORAGE
                entry.value && relevantPermission
            }
            if (granted) {
                lastInput?.let { runTask(it) }
            } else {
                addAssistantMessage("Permission denied. Photo tasks will not work.")
            }
        }

    private val notesFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@registerForActivityResult
            val progressId = addAssistantMessage("Importing notes…")
            lifecycleScope.launch {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Ignore. Import may still work for this session.
                }
                val imported = withContext(Dispatchers.IO) {
                    app.notes.importFromTree(uri)
                }
                chatAdapter.updateMessageText(
                    progressId,
                    "Imported $imported note(s). Try: notes search <text>"
                )
            }
        }

    // Fallback single-file picker for import notes (keeps backward compatibility)
    private val notesFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                val imported = withContext(Dispatchers.IO) {
                    app.notes.importFromUri(uri)
                }
                addAssistantMessage("Imported ${imported.size} note(s). Try: notes search <text>")
            }
        }

    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startSpeechRecognition()
            } else {
                addAssistantMessage("Microphone permission denied.")
            }
        }

    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val recognizedText = matches?.firstOrNull()
                if (!recognizedText.isNullOrBlank()) {
                    ensurePermissionsThenRun(recognizedText)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        app = application as App

        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        navigationView = findViewById(R.id.navigationView)
        inputEditText = findViewById(R.id.inputEditText)
        micButton = findViewById(R.id.micButton)
        sendButton = findViewById(R.id.sendButton)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)

        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navCommands -> {
                    showCommandsDialog()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.navSettings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }

        chatAdapter = ChatAdapter { item ->
            onResultClicked(item)
        }

        chatRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatRecyclerView.adapter = chatAdapter

        handlers = buildHandlers()
        voiceManager = VoiceManager(this)

        sendButton.setOnClickListener {
            val input = inputEditText.text.toString().trim()
            if (input.isNotBlank()) {
                inputEditText.setText("")
                ensurePermissionsThenRun(input)
            }
        }

        micButton.setOnClickListener {
            checkAudioPermissionAndStartSpeech()
        }

        handleIncomingIntent(intent)
        if (savedInstanceState == null) {
            addAssistantMessage(getString(R.string.greeting))
        }
    }

    override fun onResume() {
        super.onResume()
        voiceManager.refresh()
    }

    override fun onDestroy() {
        voiceManager.shutdown()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun buildHandlers(): List<TaskHandler> {
        val handlers = mutableListOf<TaskHandler>()
        handlers += HelpTask { handlers.toList() }
        handlers += CurrentTimeTask()
        handlers += SetAlarmTask()
        handlers += SetTimerTask()
        handlers += ImportNotesTask()
        handlers += IndexPhotosTask()
        handlers += ListTagsTask(app.descriptions)
        handlers += ListPhotoTagsTask(app.photos)
        handlers += SearchNotesTask(app.notes)
        handlers += SearchPhotosByNameTask(app.photos)
        handlers += SearchPhotosByDescriptionTask(app.descriptions, app.photos)
        handlers += WeatherTask()
        handlers += OpenAppTask(applicationContext)
        CatalogValidator.validate(handlers)
        return handlers
    }

    private fun ensurePermissionsThenRun(input: String) {
        if (needsPhotoPermission(input) && !app.photos.hasImagePermission()) {
            lastInput = input
            requestPhotoPermissions()
        } else {
            runTask(input)
        }
    }

    private fun runTask(input: String) {
        addUserMessage(input)
        val typingId = addTypingMessage()
        lifecycleScope.launch {
            val result = executeTask(input)
            removeMessage(typingId)
            addAssistantMessage(result.message, result.items)
            handleAction(result.action)
            speakResult(result)
        }
    }

    private fun runCommand(display: String, command: String) {
        addUserMessage(display)
        if (needsPhotoPermission(command) && !app.photos.hasImagePermission()) {
            lastInput = command
            requestPhotoPermissions()
            return
        }
        val typingId = addTypingMessage()
        lifecycleScope.launch {
            val result = executeTask(command)
            removeMessage(typingId)
            addAssistantMessage(result.message, result.items)
            handleAction(result.action)
            speakResult(result)
        }
    }

    private suspend fun executeTask(input: String): TaskResult {
        return withContext(Dispatchers.IO) {
            val handler = handlers.firstOrNull { it.canHandle(input) }
            if (handler != null) {
                handler.handle(input, this@MainActivity)
            } else {
                val language = LanguageManager.getLanguage(this@MainActivity)
                val message = if (language == "ru") {
                    "Команда не распознана. Попробуйте: ${handlers.first().exampleRu}"
                } else {
                    "Command not recognized. Try: ${handlers.first().example}"
                }
                TaskResult(message)
            }
        }
    }

    private fun needsPhotoPermission(input: String): Boolean {
        val language = LanguageManager.getLanguage(this)
        val canonical = try {
            IntentTranslator.toCanonical(input, language)
        } catch (_: Exception) {
            null
        } ?: ""
        val combined = "$input $canonical".lowercase()
        return combined.contains("photo") ||
                combined.contains("picture") ||
                combined.contains("image") ||
                combined.contains("pic") ||
                combined.contains("фото") ||
                combined.contains("фотограф") ||
                combined.contains("изображени") ||
                combined.contains("картинк") ||
                combined.contains("снимк")
    }

    private fun requestPhotoPermissions() {
        val permissions = when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        permissionLauncher.launch(permissions)
    }

    private fun showCommandsDialog() {
        val language = LanguageManager.getLanguage(this)
        val displayItems = handlers.map { handler ->
            val example = if (language == "ru") {
                handler.exampleRu
            } else {
                handler.example
            }
            "${handler.name}: $example"
        }.toTypedArray()

        runCatching {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.commands)
                .setItems(displayItems) { _, which ->
                    val handler = handlers[which]
                    val display = if (language == "ru") {
                        handler.exampleRu
                    } else {
                        handler.example
                    }
                    runCommand(display, handler.example)
                }
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private suspend fun handleAction(action: TaskAction) {
        when (action) {
            TaskAction.None -> Unit
            is TaskAction.OpenUri -> openUri(action.uri)
            is TaskAction.LaunchApp -> launchApp(action.packageName)
            TaskAction.PickNotesFolder -> {
                // Use tree picker to align with new UI, but also support file picker fallback
                try {
                    notesFolderLauncher.launch(null)
                } catch (_: Exception) {
                    notesFileLauncher.launch(arrayOf("text/*"))
                }
            }
            TaskAction.IndexPhotos -> indexPhotos()
            is TaskAction.SetAlarm -> setSystemAlarm(action.hour, action.minute, action.label)
            is TaskAction.SetTimer -> setSystemTimer(action.seconds, action.label)
        }
    }

    private fun openUri(uri: Uri) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }.onFailure {
            toast("Cannot open item")
        }
    }

    private fun launchApp(packageName: String) {
        runCatching {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                toast("Cannot launch app")
            }
        }.onFailure {
            toast("Cannot launch app")
        }
    }

    private fun indexPhotos() {
        if (isIndexing) {
            addAssistantMessage("Photo indexing is already running.")
            return
        }
        if (!app.photos.hasImagePermission()) {
            lastInput = "index photos"
            requestPhotoPermissions()
            return
        }
        isIndexing = true
        val messageId = addAssistantMessage("Indexing photos…")
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    app.photos.indexAllPhotos()
                }
                chatAdapter.updateMessageText(
                    messageId,
                    "Indexed photos. Try: list tags"
                )
            } finally {
                isIndexing = false
            }
        }
    }

    private fun onResultClicked(item: ResultItem) {
        when {
            item.command != null -> runCommand(item.command, item.command)
            item.uri != null -> openUri(item.uri)
            item.packageName != null -> launchApp(item.packageName)
            item.text != null -> showTextDialog(item.title, item.text)
            else -> toast(item.title)
        }
    }

    private fun showTextDialog(title: String, text: String) {
        runCatching {
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND_MULTIPLE -> handleSendMultiple(intent)
            Intent.ACTION_SEND, Intent.ACTION_VIEW -> handleSendSingle(intent)
        }
    }

    private fun handleSendMultiple(intent: Intent) {
        val type = intent.type ?: return
        if (!type.startsWith("text/")) return
        val uris = IntentCompat.getParcelableArrayListExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java
        ) ?: return
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                var count = 0
                uris.forEachIndexed { index, uri ->
                    if (uri != null) {
                        val text = app.notes.readText(uri)
                        if (!text.isNullOrBlank()) {
                            if (app.notes.addNote("Shared note ${index + 1}", text, uri.toString())) {
                                count++
                            }
                        }
                    }
                }
                count
            }
            addAssistantMessage(
                if (saved > 0) {
                    "Saved $saved note(s). Try: notes search <text>"
                } else {
                    "No new notes saved."
                }
            )
        }
    }

    private fun handleSendSingle(intent: Intent) {
        val type = intent.type
        // Allow if no type but has text extra
        val directText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (type != null && !type.startsWith("text/") && directText.isNullOrBlank() && intent.data == null) {
            return
        }

        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        lifecycleScope.launch {
            val sharedText = directText?.takeIf { it.isNotBlank() }
                ?: withContext(Dispatchers.IO) {
                    readSharedStream(intent)
                }

            if (sharedText.isNullOrBlank()) {
                // If intent data itself contains text via URI? Already handled
                // If no text, ignore
                return@launch
            }

            val saved = withContext(Dispatchers.IO) {
                app.notes.addNote(
                    subject ?: "Shared note",
                    sharedText,
                    "share"
                )
            }
            addAssistantMessage(
                if (saved) {
                    "Note saved. Try: notes search <text>"
                } else {
                    "Duplicate note not saved."
                }
            )
        }
    }

    private fun readSharedStream(intent: Intent): String? {
        val streamUri = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java
        )
        val dataUri = intent.data
        val streamText = streamUri?.let { app.notes.readText(it) }
        if (!streamText.isNullOrBlank()) return streamText
        return dataUri?.let { app.notes.readText(it) }
    }

    private fun checkAudioPermissionAndStartSpeech() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startSpeechRecognition()
        } else {
            audioPermissionLauncher.launch(permission)
        }
    }

    private fun startSpeechRecognition() {
        val language = LanguageManager.getLanguage(this)
        val recognizerLanguage = if (language == "ru") {
            "ru-RU"
        } else {
            "en-US"
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognizerLanguage)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_command))
        }
        runCatching {
            speechLauncher.launch(intent)
        }.onFailure {
            toast("Speech recognition is not available.")
        }
    }

    private fun speakResult(result: TaskResult) {
        voiceManager.speak(result.message)
    }

    private fun now(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun nextId(): Long = System.nanoTime()

    private fun addUserMessage(text: String) {
        chatAdapter.addMessage(
            ChatMessage(
                id = nextId(),
                text = text,
                isFromUser = true,
                time = now()
            )
        )
        scrollToEnd()
    }

    private fun addAssistantMessage(
        text: String,
        items: List<ResultItem> = emptyList()
    ): Long {
        val id = nextId()
        chatAdapter.addMessage(
            ChatMessage(
                id = id,
                text = text,
                isFromUser = false,
                time = now(),
                items = items
            )
        )
        scrollToEnd()
        return id
    }

    private fun addTypingMessage(): Long {
        val id = nextId()
        chatAdapter.addMessage(
            ChatMessage(
                id = id,
                text = "",
                isFromUser = false,
                time = now(),
                isTyping = true
            )
        )
        scrollToEnd()
        return id
    }

    private fun removeMessage(id: Long) {
        chatAdapter.removeMessage(id)
    }

    private fun scrollToEnd() {
        chatRecyclerView.post {
            if (chatAdapter.itemCount > 0) {
                chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private fun setSystemAlarm(hour: Int, minute: Int, label: String) {
        runCatching {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }.onFailure {
            toast("Cannot open system clock app")
        }
    }

    private fun setSystemTimer(seconds: Int, label: String) {
        runCatching {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }.onFailure {
            // Fallback to EXTRA_SECONDS for some OEMs
            runCatching {
                val fallback = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_SECONDS, seconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(fallback)
            }.onFailure {
                toast("Cannot open system clock app")
            }
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}
