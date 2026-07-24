package com.example.freeassistant.tasks

import android.content.Context
import android.content.pm.PackageManager
import com.example.freeassistant.LanguageManager

class OpenAppTask(private val context: Context) : TaskHandler {
    override val name = "Open app"
    override val example = "open calculator"
    override val exampleRu = "открыть калькулятор"

    private val openPatterns = listOf(
        Regex("(?i)^(open|launch|start|run)\\s+(\\w+)"),
        Regex("(?i)^(открой|открыть|запусти|запустить)\\s+(\\w+)")
    )

    override fun canHandle(input: String): Boolean {
        val normalized = InputNormalizer.normalize(input)
        return openPatterns.any { it.containsMatchIn(normalized) }
    }

    override suspend fun handle(input: String, context: Context): TaskResult {
        val appName = extractAppName(input)
        val packageName = findPackageName(appName, context)
        
        val language = LanguageManager.getLanguage(context)

        if (packageName == null) {
            val message = if (language == "ru") {
                "Приложение '$appName' не найдено"
            } else {
                "App '$appName' not found"
            }
            return TaskResult(message)
        }

        val message = if (language == "ru") {
            "Открываю приложение '$appName'"
        } else {
            "Opening app '$appName'"
        }

        return TaskResult(
            message = message,
            action = TaskAction.LaunchApp(packageName)
        )
    }

    private fun extractAppName(input: String): String {
        val lower = input.lowercase()
        val prefixes = listOf("open ", "launch ", "start ", "run ", "открой ", "открыть ", "запусти ", "запустить ")
        
        var appName = input
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                appName = input.substring(prefix.length)
                break
            }
        }
        return appName.trim()
    }

    private fun findPackageName(appName: String, context: Context): String? {
        val pm = context.packageManager
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        
        val packages = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        
        val lowerAppName = appName.lowercase()
        for (resolveInfo in packages) {
            val label = resolveInfo.loadLabel(pm).toString().lowercase()
            if (label.contains(lowerAppName)) {
                return resolveInfo.activityInfo.packageName
            }
        }
        return null
    }
}
