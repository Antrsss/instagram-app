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
    fun `getPostComments returns failure when an exception occurs`() = runTest {
        every { commentsCollection.whereEqualTo("postUuid", any<String>()) } throws RuntimeException("Error")

        val result = commentRepository.getPostComments(UUID.randomUUID())

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

        every { commentsCollection.document(commentId) } returns documentRef
        every {
            documentRef.update("likes", FieldValue.arrayUnion("user123"))
        } returns Tasks.forResult(null)

        coEvery { commentRepository.likeComment(commentId, "user123") } returns Result.success(Unit)

        val result = commentRepository.likeComment(commentId, "user123")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `likeComment returns failure when exception thrown`() = runTest {
        val commentId = "cmt789"
        val documentRef = mockk<DocumentReference>()
        val commentsCollection = mockk<CollectionReference>()
        val commentRepository = mockk<CommentRepository>()

        every { commentsCollection.document(commentId) } returns documentRef
        every {
            documentRef.update("likes", FieldValue.arrayUnion("user123"))
        } throws RuntimeException("Like failed")
        coEvery { commentRepository.likeComment(commentId, "user123") } returns Result.failure(RuntimeException("Like failed"))

        val result = commentRepository.likeComment(commentId, "user123")

        assertTrue(result.isFailure)
        assertEquals("Like failed", result.exceptionOrNull()?.message)
    }
}