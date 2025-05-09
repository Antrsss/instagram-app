package com.example.serialization_library.serializers

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class JsonSerializer {
    fun serialize(data: Any, filePath: String) {
        val jsonString = convertToJson(data)
        File(filePath).writeText(jsonString)
    }

    inline fun <reified T : Any> deserialize(filePath: String): T {
        val jsonString = File(filePath).readText()
        return parseJson(jsonString, T::class)
    }

    @PublishedApi
    internal fun <T : Any> parseJson(json: String, targetClass: KClass<T>): T {
        val cleanJson = json.trim()
        return when {
            cleanJson.startsWith('{') && cleanJson.endsWith('}') -> {
                parseJsonObject(cleanJson.removeSurrounding("{", "}"), targetClass)
            }
            cleanJson.startsWith('[') && cleanJson.endsWith(']') -> {
                when {
                    targetClass == List::class -> parseJsonArray(cleanJson.removeSurrounding("[", "]")) as T
                    else -> throw IllegalArgumentException("Expected list type but got $targetClass")
                }
            }
            else -> parsePrimitive(cleanJson, targetClass)
        }
    }

    @PublishedApi
    internal fun parseJsonArray(jsonArray: String): List<Any?> {
        if (jsonArray.isEmpty()) return emptyList()

        return jsonArray.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
            .map { parseJsonValue(it.trim(), "", null) } // Добавлен null для targetType
    }

    @PublishedApi
    internal fun <T : Any> parseJsonObject(jsonObj: String, targetClass: KClass<T>): T {
        val constructor = targetClass.primaryConstructor
            ?: throw IllegalArgumentException("No primary constructor found for $targetClass")
        val args = mutableMapOf<KParameter, Any?>()

        // Улучшенный парсинг JSON объекта с обработкой вложенных структур
        val pairs = mutableListOf<Pair<String, String>>()
        var current = jsonObj
        while (current.isNotEmpty()) {
            val colonIndex = current.indexOf(':')
            if (colonIndex == -1) break

            val key = current.substring(0, colonIndex)
                .trim()
                .removeSurrounding("\"")

            val valuePart = current.substring(colonIndex + 1).trim()
            val (value, remaining) = extractJsonValue(valuePart)

            pairs.add(key to value)
            current = remaining
        }

        pairs.forEach { (propName, value) ->
            val param = constructor.parameters.find { it.name == propName }
                ?: throw IllegalArgumentException("Unknown property '$propName' in ${targetClass.simpleName}")

            val propValue = parseJsonValue(
                value = value.trim(),
                propName = propName,
                targetType = param.type.classifier as? KClass<*>
            )
            args[param] = propValue
        }

        return constructor.callBy(args)
    }

    private fun extractJsonValue(json: String): Pair<String, String> {
        var depth = 0
        var inString = false
        var escape = false

        for (i in json.indices) {
            val c = json[i]

            when {
                escape -> escape = false
                c == '\\' -> escape = true
                c == '"' -> inString = !inString
                !inString -> when (c) {
                    '{', '[' -> depth++
                    '}', ']' -> depth--
                    ',' -> if (depth == 0) {
                        return json.substring(0, i) to json.substring(i + 1)
                    }
                }
            }
        }

        return json to ""
    }

    @PublishedApi
    internal fun parseJsonValue(value: String, propName: String, targetType: KClass<*>?): Any? {
        return when {
            value == "null" -> null
            value.startsWith('"') && value.endsWith('"') ->
                value.removeSurrounding("\"").unescapeJsonString()
            value.startsWith('{') -> {
                if (targetType == null) {
                    throw IllegalArgumentException("Target type must be specified for object property $propName")
                }
                parseJsonObject(value.removeSurrounding("{", "}"), targetType)
            }
            value.startsWith('[') -> parseJsonArray(value.removeSurrounding("[", "]"))
            value.toIntOrNull() != null -> value.toInt()
            value.toDoubleOrNull() != null -> value.toDouble()
            value.toBooleanStrictOrNull() != null -> value.toBooleanStrict()
            else -> throw IllegalArgumentException("Cannot parse value: $value")
        }
    }

    @PublishedApi
    internal fun <T : Any> parsePrimitive(json: String, targetClass: KClass<T>): T {
        return when (targetClass) {
            String::class -> json.removeSurrounding("\"") as T
            Int::class -> json.toInt() as T
            Double::class -> json.toDouble() as T
            Boolean::class -> json.toBoolean() as T
            else -> throw IllegalArgumentException("Unsupported primitive type $targetClass")
        }
    }

    private fun convertToJson(obj: Any?): String {
        return when (obj) {
            null -> "null"
            is String -> "\"${escapeJsonString(obj)}\""
            is Number, is Boolean -> obj.toString()
            is List<*> -> obj.joinToString(",", "[", "]") { convertToJson(it) }
            is Map<*, *> -> {
                val entries = obj.entries.joinToString(",") { (k, v) ->
                    "\"${escapeJsonString(k.toString())}\":${convertToJson(v)}"
                }
                "{$entries}"
            }
            else -> {
                val properties = obj::class.memberProperties
                val fields = properties.joinToString(",") { prop ->
                    "\"${prop.name}\":${convertToJson(prop.call(obj))}"
                }
                "{$fields}"
            }
        }
    }

    @PublishedApi
    internal fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    @PublishedApi
    internal fun String.unescapeJsonString(): String {
        return replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}