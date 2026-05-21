package com.expensetracker.app.ai

enum class AiBackend(val storedName: String, val label: String) {
    LOCAL("local", "Local model"),
    OLLAMA("ollama", "Ollama host");

    companion object {
        fun fromStoredName(value: String?): AiBackend? = entries.firstOrNull { it.storedName == value }
    }
}
