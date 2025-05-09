package com.example.instagramapp

import com.cloudinary.Cloudinary
import com.example.instagramapp.models.Post
import com.example.instagramapp.repos.PostRepository
import com.example.instagramapp.services.CloudinaryService
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID


@OptIn(ExperimentalCoroutinesApi::class)
class PostRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectionReference: CollectionReference
    private lateinit var documentReference: DocumentReference
    private lateinit var cloudinary: Cloudinary
    private lateinit var postRepository: PostRepository
    private lateinit var cloudinaryService: CloudinaryService

    private lateinit var postsCollection: CollectionReference
    private lateinit var likesCollection: CollectionReference
    private lateinit var commentsCollection: CollectionReference
    private lateinit var bookmarksCollection: CollectionReference

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        firestore = mockk()
        cloudinaryService = mockk()
        collectionReference = mockk()
        postsCollection = mockk()
        documentReference = mockk()
        cloudinary = mockk()


        likesCollection = mockk()
        commentsCollection = mockk()
        bookmarksCollection = mockk()

        every { firestore.collection("posts") } returns collectionReference
        every { firestore.collection("comments") } returns commentsCollection // Здесь меняем на commentsCollection
        every { firestore.collection("bookmarks") } returns bookmarksCollection // Здесь меняем на bookmarksCollection
        every { firestore.collection("likes") } returns likesCollection // Здесь меняем на likesCollection
        every { collectionReference.document(any()) } returns documentReference
        postRepository = PostRepository(firestore, cloudinaryService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createPost should upload images, generate UUID and save post`() = runTest {
        val originalPost = Post(
            authorUid = "user123",
            creationTime = Date(0),
            description = "Test post",
            imageUrls = listOf("local1", "local2"),
            likes = 0,
            postUuid = UUID(0, 0)
        )

        val task = mockk<Task<Void>>().apply {
            coEvery { isComplete } returns true
            coEvery { isCanceled } returns false
            coEvery { isSuccessful } returns true
            coEvery { exception } returns null
            coEvery { result } returns null
        }

        coEvery { documentReference.set(any()) } returns task
        coEvery { cloudinaryService.uploadImage("local1") } returns "cloud1"
        coEvery { cloudinaryService.uploadImage("local2") } returns "cloud2"

        val result = postRepository.createPost(originalPost)

        assertTrue(result.isSuccess)

        val createdPost = result.getOrNull()!!
        assertNotEquals(UUID(0, 0), createdPost.postUuid)
        assertEquals(listOf("cloud1", "cloud2"), createdPost.imageUrls)

        coVerify(exactly = 1) {
            documentReference.set(match { it as Post == createdPost })
        }
    }

    @Test
    fun `createPost should return failure when Firestore save fails`() = runTest {
        val originalPost = Post(
            authorUid = "user123",
            creationTime = Date(0),
            description = "Test post",
            imageUrls = emptyList(),
            likes = 0,
            postUuid = UUID(0, 0)
        )

        val task = mockk<Task<Void>>().apply {
            coEvery { isComplete } returns true
            coEvery { isSuccessful } returns false
            coEvery { exception } returns Exception("Firestore error")
        }

        coEvery { documentReference.set(any()) } returns task
        coEvery { cloudinaryService.uploadImage(any()) } returns "cloud1"

        val result = postRepository.createPost(originalPost)

        assertTrue(result.isFailure)
        assertEquals("Firestore error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createPost should return failure when image upload fails`() = runTest {
        val originalPost = Post(
            authorUid = "user123",
            creationTime = Date(),
            description = "Test post",
            imageUrls = listOf("local1"),
            likes = 0
        )

        val expectedException = Exception("Upload failed")
        coEvery { cloudinaryService.uploadImage(any()) } throws expectedException

        val result = postRepository.createPost(originalPost)

        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())

        coVerify(exactly = 0) { documentReference.set(any()) }
    }

    @Test
    fun `getPost should return success with post when document exists`() = runTest {
        val postUuid = UUID.randomUUID()
        val expectedPost = Post(
            authorUid = "user123",
            creationTime = Date(),
            description = "Test post",
            imageUrls = listOf("url1", "url2"),
            likes = 5,
            postUuid = postUuid
        )

        val mockSnapshot = mockk<DocumentSnapshot> {
            coEvery { toObject<Post>() } returns expectedPost
        }

        coEvery { documentReference.get() } returns Tasks.forResult(mockSnapshot)

        val result = postRepository.getPost(postUuid)

        assertTrue(result.isSuccess)
        assertEquals(expectedPost, result.getOrNull())
    }

    @Test
    fun `getPost should return failure when document does not exist`() = runTest {
        val postUuid = UUID.randomUUID()

        val mockSnapshot = mockk<DocumentSnapshot> {
            coEvery { toObject<Post>() } returns null
        }

        coEvery { documentReference.get() } returns Tasks.forResult(mockSnapshot)

        val result = postRepository.getPost(postUuid)

        assertTrue(result.isFailure)
        assertEquals("Post not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getPost should return failure when Firestore throws exception`() = runTest {
        val postUuid = UUID.randomUUID()
        val expectedException = Exception("Firestore error")

        coEvery { documentReference.get() } throws expectedException

        val result = postRepository.getPost(postUuid)

        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `getUserPosts should throw exception when post cannot be converted`() = runTest {
        val userUid = "user123"
        val mockQuerySnapshot: QuerySnapshot = mockk {
            every { documents } returns listOf(
                mockk<DocumentSnapshot> {
                    every { toObject(Post::class.java) } returns null
                }
            )
        }

        coEvery {
            postsCollection.whereEqualTo("authorUid", userUid).get()
        } returns Tasks.forResult(mockQuerySnapshot)

        assertThrows(Exception::class.java) {
            runTest {
                postRepository.getUserPosts(userUid)
            }
        }
    }

    @Test
    fun `deletePost should delete post and related data successfully`() = runTest {
        val postUuid = UUID.randomUUID()
        val post = Post(
            postUuid = postUuid,
            authorUid = "user123",
            imageUrls = listOf("url1", "url2"),
            creationTime = Date(),
            description = "Test post",
            likes = 5
        )

        every { firestore.collection("posts") } returns postsCollection
        every { postsCollection.document(postUuid.toString()) } returns documentReference

        val mockSnapshot = mockk<DocumentSnapshot> {
            coEvery { toObject<Post>() } returns post
        }
        coEvery { documentReference.get() } returns Tasks.forResult(mockSnapshot)
        coEvery { cloudinaryService.deleteImage(any()) } returns true
        coEvery { documentReference.delete() } returns Tasks.forResult(null)

        val emptyQuery = mockk<Query>()
        val emptySnapshot = mockk<QuerySnapshot> {
            coEvery { documents } returns emptyList()
        }

        coEvery { likesCollection.whereEqualTo("postUuid", postUuid.toString()) } returns emptyQuery
        coEvery { commentsCollection.whereEqualTo("postUuid", postUuid.toString()) } returns emptyQuery
        coEvery { bookmarksCollection.whereEqualTo("postUuid", postUuid.toString()) } returns emptyQuery

        coEvery { emptyQuery.get() } returns Tasks.forResult(emptySnapshot)

        val result = postRepository.deletePost(postUuid)

        assertTrue(result.isSuccess)
        coVerify(exactly = 2) { cloudinaryService.deleteImage(any()) }
        coVerify(exactly = 1) { documentReference.delete() }
    }

    @Test
    fun `deletePost should return failure when image deletion fails`() = runTest {
        val postUuid = UUID.randomUUID()
        val post = Post(
            postUuid = postUuid,
            authorUid = "user123",
            imageUrls = listOf("url1"),
            creationTime = Date(),
            description = "Test post",
            likes = 5
        )
        val expectedException = Exception("Image deletion failed")

        every { firestore.collection("posts") } returns postsCollection
        every { postsCollection.document(postUuid.toString()) } returns documentReference

        val mockSnapshot = mockk<DocumentSnapshot> {
            coEvery { toObject<Post>() } returns post
        }
        coEvery { documentReference.get() } returns Tasks.forResult(mockSnapshot)
        coEvery { cloudinaryService.deleteImage(any()) } throws expectedException

        val result = postRepository.deletePost(postUuid)

        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
        coVerify(exactly = 0) { documentReference.delete() }
    }

    @Test
    fun `deletePost should return failure when post deletion fails`() = runTest {
        val postUuid = UUID.randomUUID()
        val post = Post(
            postUuid = postUuid,
            authorUid = "user123",
            imageUrls = listOf("url1"),
            creationTime = Date(),
            description = "Test post",
            likes = 5
        )
        val expectedException = Exception("Post deletion failed")

        every { firestore.collection("posts") } returns postsCollection
        every { postsCollection.document(postUuid.toString()) } returns documentReference

        val mockSnapshot = mockk<DocumentSnapshot> {
            coEvery { toObject<Post>() } returns post
        }
        coEvery { documentReference.get() } returns Tasks.forResult(mockSnapshot)
        coEvery { cloudinaryService.deleteImage(any()) } returns true

        val failedTask = Tasks.forException<Void>(expectedException)
        coEvery { documentReference.delete() } returns failedTask

        val emptyQuery = mockk<Query>()
        val emptySnapshot = mockk<QuerySnapshot> { coEvery { documents } returns emptyList() }
        coEvery { likesCollection.whereEqualTo("postUuid", postUuid.toString()) } returns emptyQuery
        coEvery { commentsCollection.whereEqualTo("postUuid", postUuid.toString()) } returns emptyQuery
        coEvery { bookmarksCollection.whereEqualTo("postUuid", postUuid.toString()) } returns emptyQuery
        coEvery { emptyQuery.get() } returns Tasks.forResult(emptySnapshot)

        val result = postRepository.deletePost(postUuid)

        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
        coVerify(exactly = 1) { cloudinaryService.deleteImage(any()) }
        coVerify(exactly = 1) { documentReference.delete() }
    }

    @Test
    fun `deletePostRelatedData should delete all related data`() = runTest {
        val postUuid = UUID.randomUUID()

        val likeDocRef = mockk<DocumentReference>()
        val commentDocRef = mockk<DocumentReference>()
        val bookmarkDocRef = mockk<DocumentReference>()

        val likeDoc = mockk<QueryDocumentSnapshot> { every { reference } returns likeDocRef }
        val commentDoc = mockk<QueryDocumentSnapshot> { every { reference } returns commentDocRef }
        val bookmarkDoc = mockk<QueryDocumentSnapshot> { every { reference } returns bookmarkDocRef }

        val likesQuerySnapshot = mockk<QuerySnapshot> { coEvery { documents } returns listOf(likeDoc) }
        val commentsQuerySnapshot = mockk<QuerySnapshot> { coEvery { documents } returns listOf(commentDoc) }
        val bookmarksQuerySnapshot = mockk<QuerySnapshot> { coEvery { documents } returns listOf(bookmarkDoc) }

        coEvery { likesCollection.whereEqualTo("postUuid", postUuid.toString()).get() } returns Tasks.forResult(likesQuerySnapshot)
        coEvery { commentsCollection.whereEqualTo("postUuid", postUuid.toString()).get() } returns Tasks.forResult(commentsQuerySnapshot)
        coEvery { bookmarksCollection.whereEqualTo("postUuid", postUuid.toString()).get() } returns Tasks.forResult(bookmarksQuerySnapshot)

        coEvery { likeDocRef.delete() } returns Tasks.forResult(null)
        coEvery { commentDocRef.delete() } returns Tasks.forResult(null)
        coEvery { bookmarkDocRef.delete() } returns Tasks.forResult(null)

        postRepository.deletePostRelatedData(postUuid)

        coVerify(exactly = 1) { likeDocRef.delete() }
        coVerify(exactly = 1) { commentDocRef.delete() }
        coVerify(exactly = 1) { bookmarkDocRef.delete() }
    }
}