package com.example.freeassistant.tasks

import android.content.Context
import android.net.Uri

data class ResultItem(
    val title: String,
    val subtitle: String = "",
    val uri: Uri? = null,
    val packageName: String? = null,
    val text: String? = null,
    val command: String? = null
)

sealed class TaskAction {
    object None : TaskAction()
    data class OpenUri(val uri: Uri) : TaskAction()
    data class LaunchApp(val packageName: String) : TaskAction()
    object PickNotesFolder : TaskAction()
    object IndexPhotos : TaskAction()
    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val label: String = "Alarm"
    ) : TaskAction()
    data class SetTimer(
        val seconds: Int,
        val label: String = "Timer"
    ) : TaskAction()
}

data class TaskResult(
    val message: String,
    val items: List<ResultItem> = emptyList(),
    val action: TaskAction = TaskAction.None
)

interface TaskHandler {
    val name: String
    val example: String
    val exampleRu: String
    fun canHandle(input: String): Boolean
    suspend fun handle(input: String, context: Context): TaskResult
}
