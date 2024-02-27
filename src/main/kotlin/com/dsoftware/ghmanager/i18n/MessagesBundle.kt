package com.dsoftware.ghmanager.i18n

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

object MessagesBundle {
    private const val BUNDLE: String = "messages.messages"

    private val INSTANCE = DynamicBundle(
        MessagesBundle::class.java,
        BUNDLE
    )

    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any): String {
        return INSTANCE.getMessage(key, *params)
    }

    fun messagePointer(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any): Supplier<String> {
        return INSTANCE.getLazyMessage(key, *params)
    }
}