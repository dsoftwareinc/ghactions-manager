package com.dsoftware.ghmanager.api.model

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import kotlinx.datetime.Instant

class InstantDeserializer : JsonDeserializer<Instant>() {
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext?): Instant {
        // https://fasterxml.github.io/jackson-databind/javadoc/2.8/com/fasterxml/jackson/databind/JsonDeserializer.html#deserialize(com.fasterxml.jackson.core.JsonParser,%20com.fasterxml.jackson.databind.DeserializationContext)
        // Note that this method is never called for JSON null literal, and thus deserializers need (and should) not check for it
        return Instant.parse(jsonParser.text)
    }
}