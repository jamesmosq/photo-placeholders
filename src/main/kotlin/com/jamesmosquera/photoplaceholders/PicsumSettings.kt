package com.jamesmosquera.photoplaceholders

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/**
 * Remembers the last used options (application level).
 * https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html
 */
@Service(Service.Level.APP)
@State(name = "PhotoPlaceholdersSettings", storages = [Storage("photoPlaceholders.xml")])
class PicsumSettings : SimplePersistentStateComponent<PicsumSettings.SettingsState>(SettingsState()) {

    class SettingsState : BaseState() {
        var width by property(600)
        var height by property(400)
        var square by property(false)
        var mode by string(PicsumMode.RANDOM.name)
        var imageId by string("237")
        var seed by string("picsum")
        var grayscale by property(false)
        var blur by property(0)
        var format by string(PicsumFormat.DEFAULT.name)
        var uniquePerInsert by property(true)
        var output by string(OutputFormat.URL.name)
        var alt by string("Placeholder image")
    }

    var options: PicsumOptions
        get() = with(state) {
            PicsumOptions(
                width = width,
                height = height,
                square = square,
                mode = enumOr(mode, PicsumMode.RANDOM),
                imageId = imageId ?: "237",
                seed = seed ?: "picsum",
                grayscale = grayscale,
                blur = blur.coerceIn(0, 10),
                format = enumOr(format, PicsumFormat.DEFAULT),
                uniquePerInsert = uniquePerInsert,
                output = enumOr(output, OutputFormat.URL),
                alt = alt ?: "",
            )
        }
        set(o) = with(state) {
            width = o.width
            height = o.height
            square = o.square
            mode = o.mode.name
            imageId = o.imageId
            seed = o.seed
            grayscale = o.grayscale
            blur = o.blur
            format = o.format.name
            uniquePerInsert = o.uniquePerInsert
            output = o.output.name
            alt = o.alt
        }

    companion object {
        fun getInstance(): PicsumSettings = service()

        private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
