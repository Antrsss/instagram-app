package com.example.instagramapp

import com.cloudinary.Cloudinary
import com.example.instagramapp.models.Post
import com.example.instagramapp.repos.PostRepository
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
    private lateinit var postsCollection: CollectionReference

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        firestore = mockk()
        collectionReference = mockk()
        postsCollection = mockk()
        documentReference = mockk()
        cloudinary = mockk()

        every { firestore.collection("posts") } returns collectionReference
        every { collectionReference.document(any()) } returns documentReference

        postRepository = PostRepository(firestore, cloudinary)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createPost should upload images to Cloudinary and save post to Firestore`() = runTest {
        val post = Post(
            authorUid = "user1",
            creationTime = Date(),
            description = "Test description",
            imageUrls = listOf("image1.jpg", "image2.jpg"),
            likes = 0
        )

        val uploadedImageUrl = "http://cloudinary.com/uploaded_image.jpg"

        every { cloudinary.uploader().upload(any(), any()) } returns mapOf("url" to uploadedImageUrl)

        val mockTask: Task<Void> = mockk {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { exception } returns null
            every { result } returns null
            every { getResult() } returns null
        }
        coEvery { documentReference.set(any()) } returns mockTask

        postRepository.createPost(post)

        verify(exactly = 2) { cloudinary.uploader().upload(any(), any()) }
        coVerify { collectionReference.document(post.postUuid.toString()) }
        coVerify { documentReference.set(match { savedPost ->
            (savedPost as Post).imageUrls.all { it == uploadedImageUrl }
        }) }
    }

    @Test
    fun `getUserPosts should return posts for specific user`() = runTest {
        // Given
        val userUid = "user123"
        val expectedPosts = listOf(
            Post(
                authorUid = userUid,
                creationTime = Date(),
                description = "Post 1",
                imageUrls = listOf("url1"),
                likes = 5
            ),
            Post(
                authorUid = userUid,
                creationTime = Date(),
                description = "Post 2",
                imageUrls = listOf("url2"),
                likes = 10
            )
        )

        val mockQuerySnapshot: QuerySnapshot = mockk {
            every { documents } returns expectedPosts.map { post ->
                mockk<DocumentSnapshot> {
                    every { toObject(Post::class.java) } returns post
                }
            }
        }

        val mockTask: Task<QuerySnapshot> = mockk {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { exception } returns null
            every { result } returns mockQuerySnapshot
            every { getResult() } returns mockQuerySnapshot
        }
        val mockQuery: Query = mockk {
            every { get() } returns mockTask
        }

        val mockPostsCollection: CollectionReference = mockk {
            every { whereEqualTo("authorUid", userUid) } returns mockQuery
        }
        val mockFirestore: FirebaseFirestore = mockk {
            every { collection("posts") } returns mockPostsCollection
        }

        postRepository = PostRepository(mockFirestore, cloudinary)

        val result = postRepository.getUserPosts(userUid)

        assertEquals(expectedPosts, result)
        verify {
            mockPostsCollection.whereEqualTo("authorUid", userUid)
            mockQuery.get()
        }
    }

    @Test
    fun `getUserPosts should return empty list when no posts found`() = runTest {
        // Given
        val userUid = "user123"
        val emptyQuerySnapshot: QuerySnapshot = mockk {
            every { documents } returns emptyList()
        }

        val mockTask: Task<QuerySnapshot> = mockk {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { exception } returns null
            every { result } returns emptyQuerySnapshot
            every { getResult() } returns emptyQuerySnapshot
        }
        val mockQuery: Query = mockk {
            every { get() } returns mockTask
        }

        val mockPostsCollection: CollectionReference = mockk {
            every { whereEqualTo("authorUid", userUid) } returns mockQuery
        }
        val mockFirestore: FirebaseFirestore = mockk {
            every { collection("posts") } returns mockPostsCollection
        }

        postRepository = PostRepository(mockFirestore, cloudinary)

        // When
        val result = postRepository.getUserPosts(userUid)

        // Then
        assertTrue(result.isEmpty())
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
    fun `deletePost should delete document with specified UUID`() = runTest {
        val postUuid = UUID.randomUUID()

        val mockTask: Task<Void> = mockk {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { exception } returns null
            every { result } returns null
            every { getResult() } returns null
        }

        val mockDocumentReference: DocumentReference = mockk {
            every { delete() } returns mockTask
        }

        val mockPostsCollection: CollectionReference = mockk {
            every { document(postUuid.toString()) } returns mockDocumentReference
        }

        val mockFirestore: FirebaseFirestore = mockk {
            every { collection("posts") } returns mockPostsCollection
        }

        postRepository = PostRepository(mockFirestore, cloudinary)
        postRepository.deletePost(postUuid)

        verify {
            mockFirestore.collection("posts")
            mockPostsCollection.document(postUuid.toString())
            mockDocumentReference.delete()
        }
    }

    @Test
    fun `likePost should increment likes count by 1`() = runTest {
        val postUuid = UUID.randomUUID()
        val originalPost = Post(
            authorUid = "user123",
            creationTime = Date(),
            description = "Test post",
            imageUrls = listOf("url1"),
            likes = 5
        )

        val mockDocumentSnapshot: DocumentSnapshot = mockk {
            every { toObject(Post::class.java) } returns originalPost
        }

        val mockGetTask: Task<DocumentSnapshot> = mockk {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { exception } returns null
            every { result } returns mockDocumentSnapshot
            every { getResult() } returns mockDocumentSnapshot
        }

        val mockSetTask: Task<Void> = mockk {
            every { isComplete } returns true
            every { isSuccessful } returns true
            every { isCanceled } returns false
            every { exception } returns null
            every { result } returns null
            every { getResult() } returns null
        }

        val mockDocumentReference: DocumentReference = mockk {
            every { get() } returns mockGetTask
            every { set(any()) } returns mockSetTask
        }

        val mockPostsCollection: CollectionReference = mockk {
            every { document(postUuid.toString()) } returns mockDocumentReference
        }

        val mockFirestore: FirebaseFirestore = mockk {
            every { collection("posts") } returns mockPostsCollection
        }

        postRepository = PostRepository(mockFirestore, cloudinary)
        postRepository.likePost(postUuid)

        verify {
            mockDocumentReference.get()
            mockDocumentReference.set(match {
                (it as Post).likes == originalPost.likes + 1
            })
        }
    }
}