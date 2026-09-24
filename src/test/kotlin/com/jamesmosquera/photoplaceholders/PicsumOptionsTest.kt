package com.jamesmosquera.photoplaceholders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** URL patterns taken from the official docs at https://picsum.photos */
class PicsumOptionsTest {

    @Test
    fun `random image with width and height`() =
        assertEquals("https://picsum.photos/200/300", PicsumOptions(width = 200, height = 300).buildUrl())

    @Test
    fun `square image uses a single size segment`() =
        assertEquals("https://picsum.photos/200", PicsumOptions(width = 200, square = true).buildUrl())

    @Test
    fun `specific image by id`() =
        assertEquals(
            "https://picsum.photos/id/237/200/300",
            PicsumOptions(width = 200, height = 300, mode = PicsumMode.ID, imageId = "237").buildUrl(),
        )

    @Test
    fun `static random image by seed`() =
        assertEquals(
            "https://picsum.photos/seed/picsum/200/300",
            PicsumOptions(width = 200, height = 300, mode = PicsumMode.SEED, seed = "picsum").buildUrl(),
        )

    @Test
    fun `seed is url encoded`() =
        assertEquals(
            "https://picsum.photos/seed/hola%20mundo/200/300",
            PicsumOptions(width = 200, height = 300, mode = PicsumMode.SEED, seed = "hola mundo").buildUrl(),
        )

    @Test
    fun `grayscale and blur combine`() =
        assertEquals(
            "https://picsum.photos/200/300?grayscale&blur=2",
            PicsumOptions(width = 200, height = 300, grayscale = true, blur = 2).buildUrl(),
        )

    @Test
    fun `extension goes before the query string`() =
        assertEquals(
            "https://picsum.photos/200/300.webp?grayscale",
            PicsumOptions(width = 200, height = 300, format = PicsumFormat.WEBP, grayscale = true).buildUrl(),
        )

    @Test
    fun `random token only applies in random mode`() {
        assertEquals("https://picsum.photos/200/300?random=7", PicsumOptions(width = 200, height = 300).buildUrl(7))
        assertEquals(
            "https://picsum.photos/id/1/200/300",
            PicsumOptions(width = 200, height = 300, mode = PicsumMode.ID, imageId = "1").buildUrl(7),
        )
    }

    @Test
    fun `preview forces explicit size and drops extension`() =
        assertEquals(
            "https://picsum.photos/300/300",
            PicsumOptions(width = 900, square = true, format = PicsumFormat.WEBP)
                .buildUrl(overrideWidth = 300, overrideHeight = 300, forPreview = true),
        )

    @Test
    fun `snippet formats`() {
        val o = PicsumOptions(width = 200, height = 300, alt = "Demo")
        assertEquals("""<img src="https://picsum.photos/200/300" width="200" height="300" alt="Demo">""",
            o.copy(output = OutputFormat.HTML).snippet())
        assertEquals("""background-image: url("https://picsum.photos/200/300");""",
            o.copy(output = OutputFormat.CSS).snippet())
        assertEquals("![Demo](https://picsum.photos/200/300)", o.copy(output = OutputFormat.MARKDOWN).snippet())
        assertEquals("""<img src="https://picsum.photos/200/300" width={200} height={300} alt="Demo" />""",
            o.copy(output = OutputFormat.JSX).snippet())
        assertEquals("""src="https://picsum.photos/200/300"""", o.copy(output = OutputFormat.SRC_ATTRIBUTE).snippet())
    }

    @Test
    fun `alt text is escaped in html`() =
        assertEquals(
            """<img src="https://picsum.photos/10/10" width="10" height="10" alt="a &quot;b&quot;">""",
            PicsumOptions(width = 10, height = 10, alt = "a \"b\"", output = OutputFormat.HTML).snippet(),
        )
}

class PicsumApiParsingTest {

    private val sample = """
        [{"id":"0","author":"Alejandro Escamilla","width":5000,"height":3333,
          "url":"https://unsplash.com/photos/yC-Yzbqy7PY","download_url":"https://picsum.photos/id/0/5000/3333"},
         {"id":"1","author":"Alejandro \"Ale\" Escamilla","width":5000,"height":3333,
          "url":"https://unsplash.com/photos/LNRyGwIJr5c","download_url":"https:\/\/picsum.photos\/id\/1\/5000\/3333"}]
    """.trimIndent()

    @Test
    fun `parses list`() {
        val items = PicsumApi.parseList(sample)
        assertEquals(2, items.size)
        assertEquals("0", items[0].id)
        assertEquals("Alejandro Escamilla", items[0].author)
        assertEquals(5000, items[0].width)
        assertEquals(3333, items[0].height)
    }

    @Test
    fun `unescapes quotes and slashes`() {
        val second = PicsumApi.parseList(sample)[1]
        assertEquals("Alejandro \"Ale\" Escamilla", second.author)
        assertEquals("https://picsum.photos/id/1/5000/3333", second.downloadUrl)
    }

    @Test
    fun `numeric id is accepted`() =
        assertEquals("5", PicsumApi.parseObject("""{"id":5,"author":"X","width":1,"height":1}""")?.id)

    @Test
    fun `object without id is ignored`() = assertNull(PicsumApi.parseObject("""{"author":"X"}"""))
}
