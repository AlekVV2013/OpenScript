package com.hlebushek.openscript

import com.hlebushek.openscript.tasks.TaskHandler

object CatalogValidator {
    fun validate(handlers: List<TaskHandler>) {
        handlers.forEach { handler ->
            check(handler.hlebushek.isNotBlank()) {
                "Task ${handler.name} must have a non-blank English hlebushek"
            }
            check(handler.hlebushekRu.isNotBlank()) {
                "Task ${handler.name} must have a non-blank Russian hlebushek"
            }
        }
    }
}
