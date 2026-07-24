package com.example.freeassistant.notes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class NotesRepository(private val context: Context) {
    private var notesFolderUri: Uri? = null
    private val notes = mutableListOf<Note>()

    data class Note(
        val title: String,
        val content: String,
        val uri: Uri? = null
    )

    suspend fun importFromUri(uri: Uri): List<Note> = withContext(Dispatchers.IO) {
        val newNotes = mutableListOf<Note>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    val builder = StringBuilder()
                    while (reader.readLine().also { line = it } != null) {
                        builder.append(line).append("\n")
                    }
                    val content = builder.toString().trim()
                    val title = context.contentResolver.query(uri, null, null, null, null)?.use {
                        if (it.moveToFirst()) {
                            it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                        } else {
                            "Untitled"
                        }
                    } ?: "Untitled"
                    newNotes.add(Note(title, content, uri))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        notes.addAll(newNotes)
        newNotes
    }

    fun setFolderUri(uri: Uri) {
        notesFolderUri = uri
    }

    fun getAllNotes(): List<Note> = notes.toList()

    fun search(query: String): List<Note> {
        val lowerQuery = query.lowercase()
        return notes.filter {
            it.title.lowercase().contains(lowerQuery) ||
                    it.content.lowercase().contains(lowerQuery)
        }
    }

    fun addNote(note: Note) {
        notes.add(note)
    }

    fun clear() {
        notes.clear()
    }
}
