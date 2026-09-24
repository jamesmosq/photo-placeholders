package com.jamesmosquera.photoplaceholders.actions

import com.jamesmosquera.photoplaceholders.PicsumSettings
import com.jamesmosquera.photoplaceholders.ui.PicsumDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/** Opens the generator (preview + gallery) and inserts the result. */
class InsertPicsumAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = e.project != null && editor != null && editor.document.isWritable
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val settings = PicsumSettings.getInstance()

        val dialog = PicsumDialog(project, settings.options)
        if (!dialog.showAndGet()) return

        val options = dialog.options
        settings.options = options
        PicsumInserter.insert(project, editor, options)
    }
}

/** Inserts right away with the last used options, without opening the dialog. */
class QuickInsertPicsumAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = e.project != null && editor != null && editor.document.isWritable
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        PicsumInserter.insert(project, editor, PicsumSettings.getInstance().options)
    }
}
