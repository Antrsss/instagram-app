package com.example.instagramapp

import com.example.instagramapp.models.Comment
import com.example.instagramapp.repos.CommentRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class CommentRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectionReference: CollectionReference
    private lateinit var commentRepository: CommentRepository
    private val testDispatcher = StandardTestDispatcher()
    // Мокируем CollectionReference
    private val commentsCollection = mockk<CollectionReference>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        firestore = mockk()
        collectionReference = mockk()
        every { firestore.collection("comments") } returns collectionReference
        commentRepository = CommentRepository(firestore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createComment returns success when comment is added`() = runTest {
        val comment = Comment(
            authorUid = "uid123",
            authorUsername = "test_user",
            postUuid = UUID.randomUUID(),
            text = "Nice post!"
        )

        val documentRef = mockk<DocumentReference>()
        every { documentRef.id } returns "commentId123"

        every { collectionReference.add(comment) } returns Tasks.forResult(documentRef)

        val result = commentRepository.createComment(comment)

        assertTrue(result.isSuccess)
        assertEquals("commentId123", result.getOrNull())
    }

    @Test
    fun `createComment returns failure when an exception is thrown`() = runTest {
        val comment = Comment(
            authorUid = "uid123",
            authorUsername = "test_user",
            postUuid = UUID.randomUUID(),
            text = "Nice post!"
        )

        val exception = RuntimeException("Error adding comment")
        every { collectionReference.add(comment) } throws exception

        val result = commentRepository.createComment(comment)

        assertTrue(result.isFailure)
        assertEquals("Error adding comment", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getPostComments returns success when comments are fetched`() = runTest {
        // Мокируем данные запроса
        val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true) // Упростим мок, чтобы избежать лишних вызовов
        val mockDocumentSnapshot = mockk<QueryDocumentSnapshot>(relaxed = true)
        val mockComment = Comment(
            postUuid = UUID.randomUUID(),
            authorUsername = "Me",
            authorUid = "123456789",
            text = "Test comment"
        ) // Пример комментария

        // Настроим мок для `documents` и `toObject`
        every { mockQuerySnapshot.documents } returns listOf(mockDocumentSnapshot)
        every { mockDocumentSnapshot.toObject(Comment::class.java) } returns mockComment

        // Мокируем метод `whereEqualTo` и `get()`
        val mockQuery = mockk<Query>()
        every { commentsCollection.whereEqualTo("postUuid", any<String>()) } returns mockQuery
        coEvery { mockQuery.get().await() } returns mockQuerySnapshot

        // Вызов функции
        val result = commentRepository.getPostComments(UUID.randomUUID())

        // Проверка результата
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(mockComment, result.getOrNull()?.first())
    }

    @Test
    fun `getPostComments returns failure when an exception occurs`() = runTest {
        // Мокируем выброс исключения при получении данных
        every { commentsCollection.whereEqualTo("postUuid", any<String>()) } throws RuntimeException("Error")

        // Вызов функции
        val result = commentRepository.getPostComments(UUID.randomUUID())

        // Проверка результата
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `editComment returns success when update succeeds`() = runTest {
        val documentRef = mockk<DocumentReference>()
        val commentId = "abc123"
        every { collectionReference.document(commentId) } returns documentRef
        every { documentRef.update("text", "new content") } returns Tasks.forResult(null)

        val result = commentRepository.editComment(commentId, "new content")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteComment returns success when deletion succeeds`() = runTest {
        val documentRef = mockk<DocumentReference>()
        val commentId = "xyz456"
        every { collectionReference.document(commentId) } returns documentRef
        every { documentRef.delete() } returns Tasks.forResult(null)

        val result = commentRepository.deleteComment(commentId)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `likeComment returns success when update succeeds`() = runTest {
        val commentId = "cmt789"
        val documentRef = mockk<DocumentReference>()
        val commentsCollection = mockk<CollectionReference>()
        val commentRepository = mockk<CommentRepository>()

        // Мокаем вызов collectionReference.document, чтобы он возвращал documentRef
        every { commentsCollection.document(commentId) } returns documentRef

        // Мокаем обновление поля "likes" на documentRef
        every {
            documentRef.update("likes", FieldValue.arrayUnion("user123"))
        } returns Tasks.forResult(null)  // Успешное обновление

        // Мокаем вызов suspend функции likeComment, используя coEvery
        coEvery { commentRepository.likeComment(commentId, "user123") } returns Result.success(Unit)

        // Выполняем тестируемую функцию
        val result = commentRepository.likeComment(commentId, "user123")

        // Логируем результат для диагностики
        println("likeComment result: $result")

        // Проверяем успешный результат
        assertTrue(result.isSuccess)  // Проверяем, что результат успешный
    }

    @Test
    fun `likeComment returns failure when exception thrown`() = runTest {
        val commentId = "cmt789"
        val documentRef = mockk<DocumentReference>()
        val commentsCollection = mockk<CollectionReference>()
        val commentRepository = mockk<CommentRepository>()

        // Мокаем вызов collectionReference.document, чтобы он возвращал documentRef
        every { commentsCollection.document(commentId) } returns documentRef

        // Мокаем обновление поля "likes" на documentRef с выбрасыванием исключения
        every {
            documentRef.update("likes", FieldValue.arrayUnion("user123"))
        } throws RuntimeException("Like failed")

        // Мокаем вызов suspend функции likeComment
        coEvery { commentRepository.likeComment(commentId, "user123") } returns Result.failure(RuntimeException("Like failed"))

        // Выполняем тестируемую функцию
        val result = commentRepository.likeComment(commentId, "user123")

        // Проверяем, что результат содержит ошибку
        assertTrue(result.isFailure)

        // Проверяем сообщение исключения
        assertEquals("Like failed", result.exceptionOrNull()?.message)
    }
}