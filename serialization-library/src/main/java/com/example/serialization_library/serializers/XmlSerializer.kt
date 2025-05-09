package com.example.serialization_library.serializers

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class XmlSerializer {
    fun serialize(data: Any, filePath: String) {
        val xmlString = convertToXml(data, data::class.simpleName ?: "root")
        File(filePath).writeText(xmlString)
    }

    inline fun <reified T : Any> deserialize(filePath: String): T {
        val xmlString = File(filePath).readText()
        return parseXml(xmlString)
    }

    @PublishedApi
    internal inline fun <reified T : Any> parseXml(xml: String): T {
        val cleanXml = xml.trim()
        if (!cleanXml.startsWith("<") || !cleanXml.endsWith(">")) {
            throw IllegalArgumentException("Invalid XML format")
        }

        val rootTag = cleanXml.substringAfter("<").substringBefore(">")
        val closingTag = "</$rootTag>"

        if (!cleanXml.endsWith(closingTag)) {
            throw IllegalArgumentException("Mismatched tags for $rootTag")
        }

        val content = cleanXml
            .substringAfter("<$rootTag>")
            .substringBefore(closingTag)
            .trim()

        return parseXmlContent(content, T::class)
    }

    @PublishedApi
    internal fun <T : Any> parseXmlContent(xmlContent: String, targetClass: KClass<T>): T {
        val constructor = targetClass.primaryConstructor
            ?: throw IllegalArgumentException("No primary constructor found for $targetClass")
        val args = mutableMapOf<KParameter, Any?>()

        var remaining = xmlContent
        while (remaining.isNotEmpty()) {
            if (!remaining.startsWith("<")) {
                throw IllegalArgumentException("Expected XML tag but got: ${remaining.take(20)}...")
            }

            val tagName = remaining.substringAfter("<").substringBefore(">")
            val isSelfClosing = remaining.contains("<$tagName/>")

            if (isSelfClosing) {
                constructor.parameters.find { it.name == tagName }?.let { param ->
                    args[param] = null
                }
                remaining = remaining.substringAfter("<$tagName/>").trim()
                continue
            }

            val fullTag = "<$tagName>"
            val closingTag = "</$tagName>"

            if (!remaining.contains(closingTag)) {
                throw IllegalArgumentException("Missing closing tag for $tagName")
            }

            val value = remaining
                .substringAfter(fullTag)
                .substringBefore(closingTag)
                .trim()

            val param = constructor.parameters.find { it.name == tagName }
                ?: throw IllegalArgumentException("Unknown property $tagName in ${targetClass.simpleName}")

            args[param] = when {
                value.isEmpty() -> null
                param.type.classifier == String::class -> unescapeXml(value)
                param.type.classifier == Int::class -> value.toInt()
                param.type.classifier == Double::class -> value.toDouble()
                param.type.classifier == Boolean::class -> value.toBoolean()
                else -> {
                    // Handle both data classes and regular classes
                    val nestedClass = param.type.classifier as? KClass<*>
                        ?: throw IllegalArgumentException("Unsupported type for $tagName")
                    parseXmlContent(value, nestedClass)
                }
            }

            remaining = remaining.substringAfter(closingTag).trim()
        }

        return constructor.callBy(args)
    }

    private fun convertToXml(obj: Any?, tagName: String): String {
        return when (obj) {
            null -> "<$tagName/>"
            is String -> "<$tagName>${escapeXml(obj)}</$tagName>"
            is Number, is Boolean -> "<$tagName>$obj</$tagName>"
            is List<*> -> {
                val items = obj.joinToString("") { item ->
                    convertToXml(item, "item")
                }
                "<$tagName>$items</$tagName>"
            }
            else -> {
                val properties = obj::class.memberProperties
                val fields = properties.joinToString("") { prop ->
                    convertToXml(prop.call(obj), prop.name)
                }
                "<$tagName>$fields</$tagName>"
            }
        }
    }

    @PublishedApi
    internal fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    @PublishedApi
    internal fun unescapeXml(str: String): String {
        return str.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}