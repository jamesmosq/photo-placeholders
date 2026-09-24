package com.jamesmosquera.photoplaceholders.actions

import com.jamesmosquera.photoplaceholders.PhotoPlaceholdersBundle
import com.jamesmosquera.photoplaceholders.PicsumMode
import com.jamesmosquera.photoplaceholders.PicsumOptions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

/** Inserts the snippet at every caret (multi-caret aware), replacing any selection. */
object PicsumInserter {

    fun insert(project: Project, editor: Editor, options: PicsumOptions) {
        val document = editor.document
        // Bottom-up so earlier offsets are not shifted.
        val carets = editor.caretModel.allCarets.sortedByDescending { it.selectionStart }
        val baseToken = (1..900_000).random()

        WriteCommandAction.runWriteCommandAction(project, PhotoPlaceholdersBundle.message("command.insert"), null, Runnable {
            carets.forEachIndexed { index, caret ->
                val token = if (options.mode == PicsumMode.RANDOM && options.uniquePerInsert) baseToken + index else null
                val text = options.snippet(token)
                val start = caret.selectionStart
                document.replaceString(start, caret.selectionEnd, text)
                caret.removeSelection()
                caret.moveToOffset(start + text.length)
            }
        })
    }
}
