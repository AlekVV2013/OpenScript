package com.hlebushek.openscript.tasks

object TaskRegistry {
    private val handlers = mutableListOf<TaskHandler>()

    fun register(handler: TaskHandler) {
        handlers.add(handler)
    }

    fun getHandlers(): List<TaskHandler> = handlers.toList()

    fun findHandler(input: String): TaskHandler? {
        return handlers.firstOrNull { it.canHandle(input) }
    }

    fun clear() {
        handlers.clear()
    }
}
