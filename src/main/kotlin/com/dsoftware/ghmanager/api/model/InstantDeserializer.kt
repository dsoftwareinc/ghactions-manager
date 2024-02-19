package com.dsoftware.ghmanager.api.model

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import kotlinx.datetime.Instant

class InstantDeserializer: JsonDeserializer<Instant>(){
    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext?): Instant {
        return Instant.parse(jsonParser.text)
    }
}