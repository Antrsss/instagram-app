package com.example.serialization_library

import com.example.serialization_library.serializers.JsonSerializer
import com.example.serialization_library.serializers.XmlSerializer

enum class SerializationFormat {
    JSON, XML
}

class SerializationManager {
    val jsonSerializer = JsonSerializer()
    val xmlSerializer = XmlSerializer()

    fun serialize(data: Any, filePath: String, format: SerializationFormat) {
        when (format) {
            SerializationFormat.JSON -> jsonSerializer.serialize(data, filePath)
            SerializationFormat.XML -> xmlSerializer.serialize(data, filePath)
        }
    }

    inline fun <reified T : Any> deserialize(filePath: String, format: SerializationFormat): T {
        return when (format) {
            SerializationFormat.JSON -> jsonSerializer.deserialize(filePath)
            SerializationFormat.XML -> xmlSerializer.deserialize(filePath)
        }
    }
}