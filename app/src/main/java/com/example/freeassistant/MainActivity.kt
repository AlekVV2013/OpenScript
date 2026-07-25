package com.example.freeassistant

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.freeassistant.tasks.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var inputEditText: EditText
    private lateinit var runButton: Button
    private lateinit var commandsButton: Button
    private lateinit var micButton: ImageButton
    private lateinit var statusText: TextView
    private lateinit var resultsRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private var isListening = false

    private lateinit var resultAdapter: ResultAdapter
    private lateinit var handlers: List<TaskHandler>
    private val app: App by lazy { application as App }

    private val notesFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                CoroutineScope(Dispatchers.Main).launch {
                    handleImportNotes(uri)
                }
            }
        }
    }

    private fun pickNotesFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "text/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        notesFolderLauncher.launch(intent)
    }

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            matches?.firstOrNull()?.let { text ->
                inputEditText.setText(text)
                processInput(text)
            }
        }
        isListening = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        inputEditText = findViewById(R.id.inputEditText)
        runButton = findViewById(R.id.runButton)
        commandsButton = findViewById(R.id.commandsButton)
        micButton = findViewById(R.id.micButton)
        statusText = findViewById(R.id.statusText)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        toolbar = findViewById(R.id.toolbar)

        // Setup toolbar
        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        // Setup navigation view
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navSettings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Setup RecyclerView
        resultAdapter = ResultAdapter { resultItem ->
            handleResultItemClick(resultItem)
        }
        resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        resultsRecyclerView.adapter = resultAdapter

        // Build task handlers
        handlers = buildHandlers()

        // Setup buttons
        runButton.setOnClickListener {
            processInput(inputEditText.text.toString())
        }

        commandsButton.setOnClickListener {
            showCommands()
        }

        micButton.setOnClickListener {
            startListening()
        }

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                updateTtsLanguage()
            }
        }

        // Initialize speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) {
                    isListening = false
                    showStatus("Recognition error")
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { text ->
                        inputEditText.setText(text)
                        processInput(text)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }

        // Handle intent
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE, Intent.ACTION_VIEW -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                    inputEditText.setText(text)
                }
            }
        }
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
        handlers += WeatherTask(app.weather)
        handlers += OpenAppTask(applicationContext)
        CatalogValidator.validate(handlers)
        return handlers
    }

    private fun processInput(input: String) {
        if (input.isBlank()) return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                showStatus("Processing...")
                
                val handler = handlers.firstOrNull { it.canHandle(input) }
                
                if (handler != null) {
                    val result = withContext(Dispatchers.IO) {
                        handler.handle(input, this@MainActivity)
                    }
                    handleResult(result)
                } else {
                    val language = LanguageManager.getLanguage(this@MainActivity)
                    val message = if (language == "ru") {
                        "Команда не распознана. Попробуйте: ${handlers.first().exampleRu}"
                    } else {
                        "Command not recognized. Try: ${handlers.first().example}"
                    }
                    showStatus(message)
                    speak(message)
                }
            } catch (e: Exception) {
                showStatus("Error: ${e.message}")
            }
        }
    }

    private fun handleResult(result: TaskResult) {
        if (result.message.isNotEmpty()) {
            showStatus(result.message)
            speak(result.message)
        }

        if (result.items.isNotEmpty()) {
            resultAdapter.submitList(result.items)
        }

        CoroutineScope(Dispatchers.Main).launch {
            handleAction(result.action)
        }
    }

    private suspend fun handleAction(action: TaskAction) {
        when (action) {
            TaskAction.None -> Unit
            is TaskAction.OpenUri -> openUri(action.uri)
            is TaskAction.LaunchApp -> launchApp(action.packageName)
            TaskAction.PickNotesFolder -> pickNotesFolder()
            TaskAction.IndexPhotos -> indexPhotos()
            is TaskAction.SetAlarm -> setSystemAlarm(action.hour, action.minute, action.label)
            is TaskAction.SetTimer -> setSystemTimer(action.seconds, action.label)
        }
    }

    private fun handleResultItemClick(item: ResultItem) {
        when {
            item.uri != null -> openUri(item.uri)
            item.packageName != null -> launchApp(item.packageName)
            item.command != null -> inputEditText.setText(item.command)
        }
    }

    private fun showCommands() {
        val language = LanguageManager.getLanguage(this)
        val commands = handlers.map { 
            if (language == "ru") it.exampleRu else it.example 
        }
        val message = if (language == "ru") {
            "Достupные команды:\n" + commands.joinToString("\n")
        } else {
            "Available commands:\n" + commands.joinToString("\n")
        }
        showStatus(message)
        speak(message)
    }

    private fun startListening() {
        if (isListening) return

        isListening = true
        showStatus("Listening...")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, when (LanguageManager.getLanguage(this@MainActivity)) {
                "ru" -> "ru-RU"
                else -> "en-US"
            })
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command")
        }

        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            showStatus("Recognition not available")
        }
    }

    private fun speak(text: String) {
        if (!SettingsManager.isSpeechOutputEnabled(this)) return
        if (tts.isSpeaking) {
            tts.stop()
        }
        
        val gender = SettingsManager.getVoiceGender(this)
        val params = Bundle().apply {
            when (gender) {
                "male" -> putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, -1.0f)
                "female" -> putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 1.0f)
            }
        }
        
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, null)
    }

    private fun updateTtsLanguage() {
        val language = LanguageManager.getLanguage(this)
        tts.language = when (language) {
            "ru" -> Locale("ru")
            else -> Locale("en")
        }
    }

    private fun showStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }

    private fun openUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            toast("Cannot open: ${uri}")
        }
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            toast("App not found: $packageName")
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
            toast("Cannot open system clock app")
        }
    }

    private suspend fun indexPhotos() {
        withContext(Dispatchers.IO) {
            app.photos.indexAllPhotos()
        }
        val language = LanguageManager.getLanguage(this)
        val message = if (language == "ru") {
            "Фото проиндексированы"
        } else {
            "Photos indexed"
        }
        showStatus(message)
        speak(message)
    }

    private suspend fun handleImportNotes(uri: Uri) {
        val notes = withContext(Dispatchers.IO) {
            app.notes.importFromUri(uri)
        }
        val language = LanguageManager.getLanguage(this)
        val message = if (language == "ru") {
            "Импортировано ${notes.size} заметок"
        } else {
            "Imported ${notes.size} notes"
        }
        showStatus(message)
        speak(message)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        speechRecognizer.destroy()
    }
}
