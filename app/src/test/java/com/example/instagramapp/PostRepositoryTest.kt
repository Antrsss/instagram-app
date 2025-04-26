package com.example.instagramapp

import android.graphics.Bitmap
import com.example.instagramapp.models.Post
import com.example.instagramapp.repos.PostRepository
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PostRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var postRepository: PostRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        firestore = mockk(relaxed = true)
        postRepository = spyk(PostRepository(firestore))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createPost should upload images and save post to Firestore`() = runTest {
        // Arrange
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val post = Post(
            authorUid = "user123",
            creationTime = Date(),
            images = listOf(mockBitmap),
            description = "Test post",
            likes = 0
        )

        // Мокаем collection -> document -> set
        val postsCollection = mockk<CollectionReference>()
        val mockDocumentRef = mockk<DocumentReference>(relaxed = true)

        every { firestore.collection("posts") } returns postsCollection
        every { postsCollection.document(post.postUuid.toString()) } returns mockDocumentRef
        coEvery { mockDocumentRef.set(any()) } returns Tasks.forResult(null)

        // Мокаем uploadToCloudinary
        coEvery { postRepository["uploadToCloudinary"](mockBitmap) } returns "https://mock.url/image.jpg"

        // Act
        val result = postRepository.createPost(post)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(emptyList<Bitmap>(), result.getOrNull()?.images)
        coVerify { mockDocumentRef.set(any()) }
        coVerify { postRepository["uploadToCloudinary"](mockBitmap) }
    }

    @Test
    fun `getUserPosts returns posts correctly`() = runTest {
        val collectionRef = mockk<CollectionReference>()
        val query = mockk<Query>()
        val querySnapshot = mockk<QuerySnapshot>()
        val task = mockk<Task<QuerySnapshot>>()

        val userUid = "123"
        val postUuid = UUID.randomUUID()

        // Мокаем вызов Firestore
        coEvery { firestore.collection("posts") } returns collectionRef
        every { collectionRef.whereEqualTo("authorUid", userUid) } returns query
        coEvery { query.get() } returns task
        coEvery { task.result } returns querySnapshot

        // Мокаем данные внутри querySnapshot
        val document = mockk<DocumentSnapshot>()
        every { querySnapshot.documents } returns listOf(document)
        every { document.get("images") } returns listOf("url1", "url2")
        every { document.getString("authorUid") } returns userUid
        every { document.getDate("creationTime") } returns Date()
        every { document.getString("description") } returns "Desc"
        every { document.getLong("likes") } returns 5L
        every { document.getString("postUuid") } returns postUuid.toString()

        val result = postRepository.getUserPosts(userUid)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `deletePost succeeds`() = runTest {
        val docRef = mockk<DocumentReference>(relaxed = true)
        val uuid = UUID.randomUUID()

        coEvery { firestore.collection("posts") } returns mockk {
            every { document(uuid.toString()) } returns docRef
        }
        coEvery { docRef.delete() } returns Tasks.forResult(null)

        val result = postRepository.deletePost(uuid)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `likePost increments like count`() = runTest {
        val docRef = mockk<DocumentReference>()
        val uuid = UUID.randomUUID()
        val snapshot = mockk<DocumentSnapshot>()
        val transaction = mockk<Transaction>(relaxed = true)

        coEvery { firestore.collection("posts") } returns mockk {
            every { document(uuid.toString()) } returns docRef
        }

        val slot = slot<Transaction.() -> Any>()
        coEvery {
            firestore.runTransaction(capture(slot))
        } returns Tasks.forResult(slot.captured.invoke(transaction))

        every { transaction.get(docRef) } returns snapshot
        every { snapshot.getLong("likes") } returns 3L
        every { transaction.update(docRef, "likes", 4L) } returns mockk()

        val result = postRepository.likePost(uuid)
        assertTrue(result.isSuccess)
    }
}