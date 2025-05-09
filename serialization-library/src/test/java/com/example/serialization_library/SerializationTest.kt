package com.example.serialization_library

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Person(val name: String, val age: Int, val isStudent: Boolean)
data class Book(val title: String, val author: Person, val pages: Int)


class SerializationTest {
    private val testPerson = Person("Me", 19, false)
    private val testBook = Book("Kotlin Programming", Person("Phillip Lanker", 32, true), 350)
    private val jsonFile = "test.json"
    private val xmlFile = "test.xml"
    private val serializer = SerializationManager()

    @Test
    fun testJsonPersonSerialization() {
        serializer.serialize(testPerson, jsonFile, SerializationFormat.JSON)
        val deserialized = serializer.deserialize<Person>(jsonFile, SerializationFormat.JSON)
        //assertEquals(testPerson, deserialized)
        assertTrue(testPerson.name == deserialized.name
                && testPerson.age == deserialized.age
                && testPerson.isStudent == deserialized.isStudent
        )
        File(jsonFile).delete()
    }

    @Test
    fun testJsonBookSerialization() {
        serializer.serialize(testBook, jsonFile, SerializationFormat.JSON)
        val deserialized = serializer.deserialize<Book>(jsonFile, SerializationFormat.JSON)
        //assertEquals(testBook, deserialized)
        assertTrue(testBook.title == deserialized.title
                && testBook.author.name == deserialized.author.name
                && testBook.author.age == deserialized.author.age
                && testBook.author.isStudent == deserialized.author.isStudent
                && testBook.pages == deserialized.pages)
        File(jsonFile).delete()
    }

    @Test
    fun testXmlPersonSerialization() {
        serializer.serialize(testPerson, xmlFile, SerializationFormat.XML)
        val deserialized = serializer.deserialize<Person>(xmlFile, SerializationFormat.XML)
        //assertEquals(testPerson, deserialized)
        assertTrue(testPerson.name == deserialized.name
                && testPerson.age == deserialized.age
                && testPerson.isStudent == deserialized.isStudent
        )
        File(xmlFile).delete()
    }

    @Test
    fun testXmlBookSerialization() {
        serializer.serialize(testBook, xmlFile, SerializationFormat.XML)
        val deserialized = serializer.deserialize<Book>(xmlFile, SerializationFormat.XML)
        //assertEquals(testBook, deserialized)
        assertTrue(testBook.title == deserialized.title
                && testBook.author.name == deserialized.author.name
                && testBook.author.age == deserialized.author.age
                && testBook.author.isStudent == deserialized.author.isStudent
                && testBook.pages == deserialized.pages)
        File(xmlFile).delete()
    }
}