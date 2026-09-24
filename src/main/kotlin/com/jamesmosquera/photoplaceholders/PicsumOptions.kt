package com.jamesmosquera.photoplaceholders

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** How the image is chosen (https://picsum.photos). */
enum class PicsumMode(private val key: String) {
    RANDOM("mode.random"),
    ID("mode.id"),
    SEED("mode.seed");

    override fun toString() = PhotoPlaceholdersBundle.message(key)
}

/** Optional extension: .jpg or .webp, as documented by Picsum. */
enum class PicsumFormat(val extension: String, private val key: String) {
    DEFAULT("", "format.default"),
    JPG(".jpg", "format.jpg"),
    WEBP(".webp", "format.webp");

    override fun toString() = PhotoPlaceholdersBundle.message(key)
}

/** What gets inserted into the editor. */
enum class OutputFormat(private val key: String) {
    URL("output.url"),
    HTML("output.html"),
    CSS("output.css"),
    MARKDOWN("output.markdown"),
    JSX("output.jsx"),
    SRC_ATTRIBUTE("output.src");

    override fun toString() = PhotoPlaceholdersBundle.message(key)
}

data class PicsumOptions(
    val width: Int = 600,
    val height: Int = 400,
    val square: Boolean = false,
    val mode: PicsumMode = PicsumMode.RANDOM,
    val imageId: String = "237",
    val seed: String = "picsum",
    val grayscale: Boolean = false,
    /** 0 = no blur, 1..10 = intensity. */
    val blur: Int = 0,
    val format: PicsumFormat = PicsumFormat.DEFAULT,
    /** Appends ?random=N so identical <img> tags are not cached (random mode only). */
    val uniquePerInsert: Boolean = true,
    val output: OutputFormat = OutputFormat.URL,
    val alt: String = "Placeholder image",
) {
    /**
     * Builds the URL following the patterns documented at https://picsum.photos:
     *  /{w}/{h}, /{size}, /id/{id}/{w}/{h}, /seed/{seed}/{w}/{h},
     *  extensión .jpg/.webp, ?grayscale, ?blur / ?blur=N, ?random=N
     */
    fun buildUrl(randomToken: Int? = null, overrideWidth: Int? = null, overrideHeight: Int? = null, forPreview: Boolean = false): String {
        val w = (overrideWidth ?: width).coerceAtLeast(1)
        val h = (overrideHeight ?: if (square) width else height).coerceAtLeast(1)

        val sb = StringBuilder(BASE_URL)
        when (mode) {
            PicsumMode.ID -> sb.append("/id/").append(imageId.trim().ifEmpty { "0" })
            PicsumMode.SEED -> sb.append("/seed/").append(encodePath(seed.trim().ifEmpty { "picsum" }))
            PicsumMode.RANDOM -> Unit
        }
        // Square => /{size}; a forced size (preview) always uses /{w}/{h}.
        val squarePath = square && overrideWidth == null && overrideHeight == null
        sb.append('/').append(w)
        if (!squarePath) sb.append('/').append(h)
        // Preview uses JPG (ImageIO cannot decode WebP).
        if (!forPreview) sb.append(format.extension)

        val query = mutableListOf<String>()
        if (grayscale) query += "grayscale"
        if (blur in 1..10) query += "blur=$blur"
        if (mode == PicsumMode.RANDOM && randomToken != null) query += "random=$randomToken"
        if (query.isNotEmpty()) sb.append('?').append(query.joinToString("&"))
        return sb.toString()
    }

    /** Final snippet for the selected output format. */
    fun snippet(randomToken: Int? = null): String {
        val url = buildUrl(randomToken)
        val w = width
        val h = if (square) width else height
        val safeAlt = alt.replace("\"", "&quot;")
        return when (output) {
            OutputFormat.URL -> url
            OutputFormat.HTML -> "<img src=\"$url\" width=\"$w\" height=\"$h\" alt=\"$safeAlt\">"
            OutputFormat.CSS -> "background-image: url(\"$url\");"
            OutputFormat.MARKDOWN -> "![${alt.replace("]", "\\]")}]($url)"
            OutputFormat.JSX -> "<img src=\"$url\" width={$w} height={$h} alt=\"$safeAlt\" />"
            OutputFormat.SRC_ATTRIBUTE -> "src=\"$url\""
        }
    }

    companion object {
        const val BASE_URL = "https://picsum.photos"

        private fun encodePath(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
    }
}
