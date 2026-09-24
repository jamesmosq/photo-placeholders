package com.jamesmosquera.photoplaceholders

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.PhotoPlaceholdersBundle"

/**
 * Delegates to a DynamicBundle instance (subclassing is deprecated).
 * https://plugins.jetbrains.com/docs/intellij/internationalization.html
 */
object PhotoPlaceholdersBundle {
    private val INSTANCE = DynamicBundle(PhotoPlaceholdersBundle::class.java, BUNDLE)

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): @Nls String =
        INSTANCE.getMessage(key, *params)
}
