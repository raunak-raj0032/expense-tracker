package com.expensetracker.app.ai

import android.content.Context
import com.expensetracker.app.core.prefs.AiModelState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalModelStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val modelDir: File get() = File(context.filesDir, "ai-models")
    val modelFile: File get() = File(modelDir, AiModelSpec.FILE_NAME)
    val tempFile: File get() = File(modelDir, "${AiModelSpec.FILE_NAME}.download")

    fun isInstalled(state: AiModelState): Boolean =
        state.localModelVersion == AiModelSpec.VERSION &&
            state.localModelPath.isNotBlank() &&
            File(state.localModelPath).let { it.exists() && it.length() > 0L }

    fun prepareTempFile(): File {
        modelDir.mkdirs()
        if (tempFile.exists()) tempFile.delete()
        return tempFile
    }

    fun installVerifiedTemp(): File {
        modelDir.mkdirs()
        if (modelFile.exists()) modelFile.delete()
        check(tempFile.renameTo(modelFile)) { "Could not install downloaded model." }
        return modelFile
    }

    fun deleteAll() {
        if (tempFile.exists()) tempFile.delete()
        if (modelFile.exists()) modelFile.delete()
    }
}
