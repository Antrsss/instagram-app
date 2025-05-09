package com.example.instagramapp

import com.example.instagramapp.models.Story
import com.example.instagramapp.repos.StoryRepository
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class StoryRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storiesCollection: CollectionReference
    private lateinit var documentReference: DocumentReference
    private lateinit var cloudinaryService: CloudinaryService
    private lateinit var storyRepository: StoryRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        firestore = mockk()
        cloudinaryService = mockk()
        storiesCollection = mockk()
        documentReference = mockk()

        every { firestore.collection("stories") } returns storiesCollection
        every { storiesCollection.document(any()) } returns documentReference

        storyRepository = StoryRepository(firestore, cloudinaryService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createStory should save story successfully`() = runTest {
        val story = Story(
            authorUid = "user123",
            creationTime = Date(),
            expirationTime = Date(System.currentTimeMillis() + 86400000), // +1 day
            isVisible = true,
            photoUrl = "photo_url"
        )

        val task = Tasks.forResult(null) as Task<Void>

        coEvery { documentReference.set(story) } returns task

        storyRepository.createStory(story)

        coVerify(exactly = 1) { documentReference.set(story) }
    }

    @Test(expected = Exception::class)
    fun `createStory should throw exception when Firestore fails`() = runTest {
        val story = Story(
            authorUid = "user123",
            creationTime = Date(),
            expirationTime = Date(System.currentTimeMillis() + 86400000),
            isVisible = true,
            photoUrl = "photo_url"
        )

        coEvery { documentReference.set(story) } throws Exception("Firestore error")

        storyRepository.createStory(story)
    }

    @Test
    fun `getActiveStories should return active stories`() = runTest {
        val userUid = "user123"
        val now = Date()
        val activeStory = Story(
            authorUid = userUid,
            creationTime = now,
            expirationTime = Date(now.time + 86400000),
            isVisible = true,
            photoUrl = "active_photo_url"
        )

        val initialQuery = mockk<Query>()
        val visibleQuery = mockk<Query>()
        val finalQuery = mockk<Query>()

        val querySnapshot = mockk<QuerySnapshot> {
            coEvery { toObjects(Story::class.java) } returns listOf(activeStory)
        }

        every { storiesCollection.whereEqualTo("authorUid", userUid) } returns initialQuery
        every { initialQuery.whereEqualTo("isVisible", true) } returns visibleQuery
        every { visibleQuery.whereGreaterThan("expirationTime", any<Date>()) } returns finalQuery
        coEvery { finalQuery.get() } returns Tasks.forResult(querySnapshot)

        val result = storyRepository.getActiveStories(userUid)

        assertEquals(1, result.size)
        assertEquals(activeStory, result[0])

        verify { storiesCollection.whereEqualTo("authorUid", userUid) }
        verify { initialQuery.whereEqualTo("isVisible", true) }
        verify { visibleQuery.whereGreaterThan("expirationTime", any<Date>()) }
        coVerify(exactly = 1) { finalQuery.get() }
    }

    @Test(expected = Exception::class)
    fun `getActiveStories should throw exception when query fails`() = runTest {
        val userUid = "user123"
        val query = mockk<Query>()

        every { storiesCollection.whereEqualTo("authorUid", userUid) } returns query
        every { query.whereEqualTo("isVisible", true) } returns query
        every { query.whereGreaterThan("expirationTime", any()) } returns query
        coEvery { query.get() } throws Exception("Query failed")

        storyRepository.getActiveStories(userUid)
    }

    @Test
    fun `getArchivedStories should return archived stories`() = runTest {
        val userUid = "user123"
        val now = Date()
        val archivedStory = Story(
            authorUid = userUid,
            creationTime = Date(now.time - 172800000), // -2 days
            expirationTime = Date(now.time - 86400000), // -1 day
            isVisible = true,
            photoUrl = "archived_photo_url"
        )

        val query = mockk<Query>()
        val querySnapshot = mockk<QuerySnapshot> {
            coEvery { toObjects(Story::class.java) } returns listOf(archivedStory)
        }

        every { storiesCollection.whereEqualTo("authorUid", userUid) } returns query
        every { query.whereLessThan("expirationTime", any<Date>()) } returns query
        coEvery { query.get() } returns Tasks.forResult(querySnapshot)

        val result = storyRepository.getArchivedStories(userUid)

        assertEquals(1, result.size)
        assertEquals(archivedStory, result[0])

        verify { storiesCollection.whereEqualTo("authorUid", userUid) }
        verify { query.whereLessThan("expirationTime", any<Date>()) }
        coVerify { query.get() }
    }

    @Test(expected = Exception::class)
    fun `getArchivedStories should throw exception when query fails`() = runTest {
        val userUid = "user123"
        val query = mockk<Query>()

        every { storiesCollection.whereEqualTo("authorUid", userUid) } returns query
        every { query.whereLessThan("expirationTime", any()) } returns query
        coEvery { query.get() } throws Exception("Query failed")

        storyRepository.getArchivedStories(userUid)
    }

    @Test
    fun `deleteStory should delete story and photo successfully`() = runTest {
        val storyUuid = UUID.randomUUID()
        val photoUrl = "photo_url"
        val story = Story(
            authorUid = "user123",
            creationTime = Date(),
            expirationTime = Date(System.currentTimeMillis() + 86400000),
            isVisible = true,
            photoUrl = photoUrl,
            storyUuid = storyUuid
        )

        val documentSnapshot = mockk<DocumentSnapshot> {
            coEvery { toObject(Story::class.java) } returns story
        }

        val getTask = Tasks.forResult(documentSnapshot)

        val deleteTask = Tasks.forResult(null as Void?)

        coEvery { documentReference.get() } returns getTask
        coEvery { cloudinaryService.deleteImage(photoUrl) } returns true
        coEvery { documentReference.delete() } returns deleteTask

        storyRepository.deleteStory(storyUuid)

        coVerify(exactly = 1) {
            documentReference.get()
        }
        coVerify(exactly = 1) {
            cloudinaryService.deleteImage(photoUrl)
        }
        coVerify(exactly = 1) {
            documentReference.delete()
        }
    }

    @Test(expected = Exception::class)
    fun `deleteStory should throw exception when story not found`() = runTest {
        val storyUuid = UUID.randomUUID()
        val getTask = Tasks.forResult(mockk<DocumentSnapshot> {
            coEvery { toObject(Story::class.java) } returns null
        })

        coEvery { documentReference.get() } returns getTask

        storyRepository.deleteStory(storyUuid)
    }
}