package com.jamesmosquera.photoplaceholders

import com.intellij.util.io.HttpRequests
import java.awt.Image
import java.io.ByteArrayInputStream
import java.util.Collections
import javax.imageio.ImageIO

/** Metadata returned by /v2/list and /id/{id}/info. */
data class PicsumImageInfo(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    val url: String,
    val downloadUrl: String,
)

/**
 * Minimal client for the https://picsum.photos API.
 * Uses the IDE's HttpRequests (honours proxy settings, follows redirects).
 * All network functions block: never call them on the EDT.
 */
object PicsumApi {

    private const val THUMB_CACHE_SIZE = 150

    /** Small LRU cache so browsing gallery pages back and forth does not re-download thumbnails. */
    private val thumbnails: MutableMap<String, Image> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Image>(THUMB_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Image>) = size > THUMB_CACHE_SIZE
        }
    )

    private fun request(url: String) = HttpRequests.request(url)
        .productNameAsUserAgent()
        .connectTimeout(10_000)
        .readTimeout(15_000)

    fun thumbnailUrl(id: String, width: Int, height: Int) = "${PicsumOptions.BASE_URL}/id/$id/$width/$height"

    /** GET /v2/list?page={page}&limit={limit} */
    fun list(page: Int, limit: Int = 30): List<PicsumImageInfo> =
        parseList(request("${PicsumOptions.BASE_URL}/v2/list?page=$page&limit=$limit").readString())

    /** GET /id/{id}/info */
    fun info(id: String): PicsumImageInfo? =
        parseObject(request("${PicsumOptions.BASE_URL}/id/${id.trim()}/info").readString())

    /** Downloads and decodes a JPG image. Only deterministic URLs (by id) should be cached. */
    fun image(url: String, cache: Boolean): Image? {
        if (cache) thumbnails[url]?.let { return it }
        val image = ImageIO.read(ByteArrayInputStream(request(url).readBytes(null))) ?: return null
        if (cache) thumbnails[url] = image
        return image
    }

    // --- minimal JSON parsing (Picsum objects are flat, no nesting) ---
    private val OBJECT = Regex("""\{[^{}]*\}""")

    internal fun parseList(json: String): List<PicsumImageInfo> =
        OBJECT.findAll(json).mapNotNull { parseObject(it.value) }.toList()

    internal fun parseObject(obj: String): PicsumImageInfo? {
        val id = str(obj, "id") ?: num(obj, "id")?.toString() ?: return null
        return PicsumImageInfo(
            id = id,
            author = str(obj, "author") ?: "",
            width = num(obj, "width") ?: 0,
            height = num(obj, "height") ?: 0,
            url = str(obj, "url") ?: "",
            downloadUrl = str(obj, "download_url") ?: "",
        )
    }

    private fun str(obj: String, key: String): String? =
        Regex(""""$key"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(obj)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")?.replace("\\/", "/")

    private fun num(obj: String, key: String): Int? =
        Regex(""""$key"\s*:\s*"?(\d+)"?""").find(obj)?.groupValues?.get(1)?.toIntOrNull()
}
