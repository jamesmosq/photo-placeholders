package com.jamesmosquera.photoplaceholders.ui

import com.jamesmosquera.photoplaceholders.OutputFormat
import com.jamesmosquera.photoplaceholders.PhotoPlaceholdersBundle.message
import com.jamesmosquera.photoplaceholders.PicsumApi
import com.jamesmosquera.photoplaceholders.PicsumFormat
import com.jamesmosquera.photoplaceholders.PicsumMode
import com.jamesmosquera.photoplaceholders.PicsumOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Image
import java.awt.datatransfer.StringSelection
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Placeholder photo generator with live preview and a gallery backed by picsum.photos /v2/list.
 */
class PicsumDialog(project: Project?, initial: PicsumOptions) : DialogWrapper(project, true) {

    // ---------- Generator ----------
    private val widthSpinner = JSpinner(SpinnerNumberModel(initial.width.coerceIn(1, MAX_SIZE), 1, MAX_SIZE, 10))
    private val heightSpinner = JSpinner(SpinnerNumberModel(initial.height.coerceIn(1, MAX_SIZE), 1, MAX_SIZE, 10))
    private val squareCheck = JBCheckBox(message("check.square"), initial.square)
    private val modeCombo = ComboBox(PicsumMode.entries.toTypedArray()).apply { selectedItem = initial.mode }
    private val idField = JBTextField(initial.imageId, 8)
    private val seedField = JBTextField(initial.seed, 14)
    private val randomSeedButton = JButton(message("button.newSeed"))
    private val grayscaleCheck = JBCheckBox(message("check.grayscale"), initial.grayscale)
    private val blurSlider = JSlider(0, 10, initial.blur).apply {
        majorTickSpacing = 1; paintTicks = true; paintLabels = true; snapToTicks = true
    }
    private val formatCombo = ComboBox(PicsumFormat.entries.toTypedArray()).apply { selectedItem = initial.format }
    private val uniqueCheck = JBCheckBox(message("check.unique"), initial.uniquePerInsert)
    private val outputCombo = ComboBox(OutputFormat.entries.toTypedArray()).apply { selectedItem = initial.output }
    private val altField = JBTextField(initial.alt)
    private val resultArea = JBTextArea(3, 50).apply { isEditable = false; lineWrap = true }
    private val copyButton = JButton(message("button.copy"))
    private val previewLabel = JBLabel("", SwingConstants.CENTER).apply {
        preferredSize = Dimension(PREVIEW_MAX, PREVIEW_MAX * 2 / 3)
        border = JBUI.Borders.customLine(JBColor.border())
    }
    private val infoLabel = JBLabel(" ")
    private val previewTimer = Timer(600) { loadPreview() }.apply { isRepeats = false }
    private val previewGeneration = AtomicInteger()

    // ---------- Gallery ----------
    private val tabs = JBTabbedPane()
    private val galleryGrid = JPanel(GridLayout(0, 3, 8, 8))
    private val pageSpinner = JSpinner(SpinnerNumberModel(1, 1, 10_000, 1))
    private val galleryStatus = JBLabel(" ")
    private val galleryGeneration = AtomicInteger()
    private var galleryLoaded = false

    /** At most 4 concurrent downloads, to be gentle with a free service. */
    private val executor: ExecutorService =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("PhotoPlaceholders.Downloads", 4)

    init {
        title = message("dialog.title")
        setOKButtonText(message("dialog.ok"))
        init()
        wireListeners()
        updateEnabledState()
        refreshResult()
        previewTimer.restart()
    }

    // ============================================================ UI

    override fun createCenterPanel(): JComponent {
        tabs.addTab(message("tab.generator"), buildGeneratorPanel())
        tabs.addTab(message("tab.gallery"), buildGalleryPanel())
        tabs.addChangeListener {
            if (tabs.selectedIndex == 1 && !galleryLoaded) loadGallery()
        }
        tabs.preferredSize = JBUI.size(820, 560)
        return tabs
    }

    private fun buildGeneratorPanel(): JComponent {
        val sizeRow = row(widthSpinner, JBLabel("×"), heightSpinner, squareCheck)
        val seedRow = row(seedField, randomSeedButton)
        val idRow = row(idField, JButton(message("button.pickFromGallery")).apply { addActionListener { tabs.selectedIndex = 1 } })
        val resultRow = JPanel(BorderLayout(4, 0)).apply {
            add(JBScrollPane(resultArea), BorderLayout.CENTER)
            add(copyButton, BorderLayout.EAST)
        }

        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent(message("label.size"), sizeRow)
            .addLabeledComponent(message("label.mode"), modeCombo)
            .addLabeledComponent(message("label.id"), idRow)
            .addLabeledComponent(message("label.seed"), seedRow)
            .addSeparator()
            .addComponent(grayscaleCheck)
            .addLabeledComponent(message("label.blur"), blurSlider)
            .addLabeledComponent(message("label.format"), formatCombo)
            .addComponent(uniqueCheck)
            .addSeparator()
            .addLabeledComponent(message("label.output"), outputCombo)
            .addLabeledComponent(message("label.alt"), altField)
            .addLabeledComponent(message("label.result"), resultRow)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        val credit = JBLabel(message("credit")).apply {
            foreground = UIUtil.getContextHelpForeground()
        }
        val right = JPanel(BorderLayout(0, 6)).apply {
            add(JBLabel(message("label.preview")), BorderLayout.NORTH)
            add(previewLabel, BorderLayout.CENTER)
            add(JPanel(BorderLayout(0, 4)).apply {
                add(infoLabel, BorderLayout.CENTER)
                add(JButton(message("button.another")).apply { addActionListener { loadPreview() } }, BorderLayout.EAST)
                add(credit, BorderLayout.SOUTH)
            }, BorderLayout.SOUTH)
            border = JBUI.Borders.emptyLeft(12)
        }

        return JPanel(BorderLayout()).apply {
            add(form, BorderLayout.CENTER)
            add(right, BorderLayout.EAST)
            border = JBUI.Borders.empty(8)
        }
    }

    private fun buildGalleryPanel(): JComponent {
        val nav = row(
            JButton("◀").apply { addActionListener { changePage(-1) } },
            JBLabel(message("label.page")), pageSpinner,
            JButton("▶").apply { addActionListener { changePage(+1) } },
            JButton(message("button.load")).apply { addActionListener { loadGallery() } },
            galleryStatus,
        )
        return JPanel(BorderLayout(0, 8)).apply {
            add(nav, BorderLayout.NORTH)
            add(JBScrollPane(galleryGrid).apply { verticalScrollBar.unitIncrement = 16 }, BorderLayout.CENTER)
            border = JBUI.Borders.empty(8)
        }
    }

    private fun row(vararg components: JComponent) =
        JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply { components.forEach { add(it) } }

    // ============================================================ State

    val options: PicsumOptions
        get() = PicsumOptions(
            width = widthSpinner.value as Int,
            height = heightSpinner.value as Int,
            square = squareCheck.isSelected,
            mode = modeCombo.selectedItem as PicsumMode,
            imageId = idField.text.trim(),
            seed = seedField.text.trim(),
            grayscale = grayscaleCheck.isSelected,
            blur = blurSlider.value,
            format = formatCombo.selectedItem as PicsumFormat,
            uniquePerInsert = uniqueCheck.isSelected,
            output = outputCombo.selectedItem as OutputFormat,
            alt = altField.text,
        )

    private fun wireListeners() {
        val onChange = { refreshResult(); previewTimer.restart() }
        widthSpinner.addChangeListener { onChange() }
        heightSpinner.addChangeListener { onChange() }
        squareCheck.addActionListener { updateEnabledState(); onChange() }
        modeCombo.addActionListener { updateEnabledState(); onChange() }
        grayscaleCheck.addActionListener { onChange() }
        blurSlider.addChangeListener { if (!blurSlider.valueIsAdjusting) onChange() }
        formatCombo.addActionListener { refreshResult() }
        uniqueCheck.addActionListener { refreshResult() }
        outputCombo.addActionListener { refreshResult() }
        listOf(idField, seedField).forEach { it.document.addDocumentListener(docListener(onChange)) }
        altField.document.addDocumentListener(docListener { refreshResult() })
        randomSeedButton.addActionListener { seedField.text = UUID.randomUUID().toString().take(8) }
        copyButton.addActionListener { CopyPasteManager.getInstance().setContents(StringSelection(resultArea.text)) }
    }

    private fun updateEnabledState() {
        val mode = modeCombo.selectedItem as PicsumMode
        idField.isEnabled = mode == PicsumMode.ID
        seedField.isEnabled = mode == PicsumMode.SEED
        randomSeedButton.isEnabled = mode == PicsumMode.SEED
        uniqueCheck.isEnabled = mode == PicsumMode.RANDOM
        heightSpinner.isEnabled = !squareCheck.isSelected
    }

    private fun refreshResult() {
        val o = options
        resultArea.text = o.snippet(if (o.mode == PicsumMode.RANDOM && o.uniquePerInsert) 1 else null)
    }

    override fun doValidate(): ValidationInfo? {
        val o = options
        return when {
            o.mode == PicsumMode.ID && o.imageId.toIntOrNull() == null -> ValidationInfo(message("validation.id"), idField)
            o.mode == PicsumMode.SEED && o.seed.isBlank() -> ValidationInfo(message("validation.seed"), seedField)
            else -> null
        }
    }

    // ============================================================ Preview

    private fun loadPreview() {
        val o = options
        val w = o.width
        val h = if (o.square) o.width else o.height
        val pw = minOf(w, PREVIEW_MAX)
        val ph = maxOf(1, (h.toLong() * pw / w).toInt())
        val url = o.buildUrl(randomToken = (1..1_000_000).random(), overrideWidth = pw, overrideHeight = ph, forPreview = true)
        val generation = previewGeneration.incrementAndGet()
        previewLabel.icon = null
        previewLabel.text = message("preview.loading")
        infoLabel.text = " "

        runInBackground(
            task = {
                val image = PicsumApi.image(url, cache = false)
                val info = if (o.mode == PicsumMode.ID) runCatching { PicsumApi.info(o.imageId) }.getOrNull() else null
                image to info
            },
            onSuccess = { (image, info) ->
                if (generation == previewGeneration.get()) {
                    previewLabel.text = if (image == null) message("preview.none") else ""
                    previewLabel.icon = image?.let { ImageIcon(fit(it, PREVIEW_MAX, PREVIEW_MAX)) }
                    infoLabel.text = info?.let { "#${it.id} · ${it.author} (${it.width}×${it.height})" } ?: "$w×$h"
                }
            },
            onError = {
                if (generation == previewGeneration.get()) previewLabel.text = errorText(it)
            },
        )
    }

    // ============================================================ Gallery

    private fun changePage(delta: Int) {
        pageSpinner.value = maxOf(1, (pageSpinner.value as Int) + delta)
        loadGallery()
    }

    private fun loadGallery() {
        galleryLoaded = true
        val page = pageSpinner.value as Int
        val generation = galleryGeneration.incrementAndGet()
        galleryStatus.text = message("gallery.loading", page)
        galleryGrid.removeAll(); galleryGrid.revalidate(); galleryGrid.repaint()

        runInBackground(
            task = { PicsumApi.list(page, PAGE_SIZE) },
            onSuccess = { items ->
                if (generation != galleryGeneration.get()) return@runInBackground
                galleryStatus.text = if (items.isEmpty()) message("gallery.empty") else message("gallery.count", items.size)
                items.forEach { info ->
                    val button = JButton("<html><center>#${info.id}<br>${escape(info.author)}</center></html>").apply {
                        verticalTextPosition = SwingConstants.BOTTOM
                        horizontalTextPosition = SwingConstants.CENTER
                        preferredSize = JBUI.size(THUMB_W + 20, THUMB_H + 50)
                        toolTipText = "${info.author} · ${info.width}×${info.height}"
                        addActionListener { pickFromGallery(info.id) }
                    }
                    galleryGrid.add(button)
                    runInBackground(
                        task = { PicsumApi.image(PicsumApi.thumbnailUrl(info.id, THUMB_W, THUMB_H), cache = true) },
                        onSuccess = { img ->
                            if (img != null && generation == galleryGeneration.get()) button.icon = ImageIcon(img)
                        },
                        onError = {},
                    )
                }
                galleryGrid.revalidate(); galleryGrid.repaint()
            },
            onError = {
                if (generation == galleryGeneration.get()) galleryStatus.text = errorText(it)
            },
        )
    }

    private fun pickFromGallery(id: String) {
        modeCombo.selectedItem = PicsumMode.ID
        idField.text = id
        tabs.selectedIndex = 0
    }

    // ============================================================ Helpers

    private fun <T> runInBackground(task: () -> T, onSuccess: (T) -> Unit, onError: (Throwable) -> Unit) {
        executor.execute {
            if (isDisposed) return@execute
            val result = runCatching(task)
            // ModalityState.any(): the dialog is modal; otherwise the callback would wait until it closes.
            ApplicationManager.getApplication().invokeLater({
                if (!isDisposed) result.fold(onSuccess, onError)
            }, ModalityState.any())
        }
    }

    private fun errorText(t: Throwable) = message("error.generic", t.message ?: message("error.offline"))

    override fun dispose() {
        previewTimer.stop()
        previewGeneration.incrementAndGet()
        galleryGeneration.incrementAndGet()
        executor.shutdownNow()
        super.dispose()
    }

    private fun docListener(block: () -> Unit) = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent) = block()
        override fun removeUpdate(e: DocumentEvent) = block()
        override fun changedUpdate(e: DocumentEvent) = block()
    }

    private fun fit(img: Image, maxW: Int, maxH: Int): Image {
        val w = img.getWidth(null).coerceAtLeast(1)
        val h = img.getHeight(null).coerceAtLeast(1)
        val scale = minOf(maxW.toDouble() / w, maxH.toDouble() / h, 1.0)
        return if (scale >= 1.0) img
        else img.getScaledInstance((w * scale).toInt(), (h * scale).toInt(), Image.SCALE_SMOOTH)
    }

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    override fun getPreferredFocusedComponent(): JComponent = widthSpinner

    companion object {
        private const val MAX_SIZE = 5000
        private const val PREVIEW_MAX = 300
        private const val THUMB_W = 200
        private const val THUMB_H = 133
        private const val PAGE_SIZE = 30
    }
}
