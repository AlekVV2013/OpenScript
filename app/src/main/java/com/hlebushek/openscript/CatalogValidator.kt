package com.hlebushek.openscript

import com.hlebushek.openscript.tasks.TaskHandler

object CatalogValidator {
    fun validate(handlers: List<TaskHandler>) {
        handlers.forEach { handler ->
            check(handler.example.isNotBlank()) {
                "Task ${handler.name} must have a non-blank English example"
            }
            check(handler.exampleRu.isNotBlank()) {
                "Task ${handler.name} must have a non-blank Russian example"
            }
        }
    }
}
