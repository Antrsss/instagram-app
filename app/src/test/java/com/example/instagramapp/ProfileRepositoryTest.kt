package com.example.instagramapp

import com.example.instagramapp.models.Profile
import com.example.instagramapp.repos.ProfileRepository
import com.example.instagramapp.services.CloudinaryService
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.Exception

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var profilesCollection: com.google.firebase.firestore.CollectionReference
    private lateinit var documentReference: DocumentReference
    private lateinit var cloudinaryService: CloudinaryService
    private lateinit var profileRepository: ProfileRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        firestore = mockk()
        cloudinaryService = mockk()
        profilesCollection = mockk()
        documentReference = mockk()

        every { firestore.collection("profiles") } returns profilesCollection
        every { profilesCollection.document(any()) } returns documentReference

        profileRepository = ProfileRepository(firestore, cloudinaryService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createProfile should save profile successfully`() = runTest {
        val profile = Profile(
            userUid = "user123",
            name = "Test User",
            username = "testuser"
        )

        val task = mockk<Task<Void>>().apply {
            coEvery { isComplete } returns true
            coEvery { isCanceled } returns false
            coEvery { isSuccessful } returns true
            coEvery { exception } returns null
            coEvery { result } returns null
        }

        coEvery { documentReference.set(profile) } returns task

        val result = profileRepository.createProfile(profile)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { documentReference.set(profile) }
    }

    @Test
    fun `createProfile should return failure when Firestore fails`() = runTest {
        val profile = Profile(
            userUid = "user123",
            name = "Test User",
            username = "testuser"
        )

        val task = mockk<Task<Void>>().apply {
            coEvery { isComplete } returns true
            coEvery { isSuccessful } returns false
            coEvery { exception } returns Exception("Firestore error")
        }

        coEvery { documentReference.set(profile) } returns task

        val result = profileRepository.createProfile(profile)

        assertTrue(result.isFailure)
        assertEquals("Firestore error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getProfile should return profile when exists`() = runTest {
        val userUid = "user123"
        val expectedProfile = Profile(
            userUid = userUid,
            name = "Test User",
            username = "testuser"
        )

        val mockSnapshot = mockk<DocumentSnapshot> {
            coEvery { toObject(Profile::class.java) } returns expectedProfile
        }

        coEvery { documentReference.get() } returns Tasks.forResult(mockSnapshot)

        val result = profileRepository.getProfile(userUid)

        assertTrue(result.isSuccess)
        assertEquals(expectedProfile, result.getOrNull())
    }

    @Test
    fun `getProfile should return failure when profile not found`() = runTest {
        val userUid = "user123"

        val mockSnapshot = mockk<DocumentSnapshot> {
            coEvery { toObject(Profile::class.java) } returns null
        }

        coEvery { documentReference.get() } returns Tasks.forResult(mockSnapshot)

        val result = profileRepository.getProfile(userUid)

        assertTrue(result.isFailure)
        assertEquals("Profile not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getProfile should return failure when Firestore fails`() = runTest {
        val userUid = "user123"
        val expectedException = Exception("Firestore error")

        coEvery { documentReference.get() } throws expectedException

        val result = profileRepository.getProfile(userUid)

        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `editProfile should update profile fields successfully`() = runTest {
        val profile = Profile(
            userUid = "user123",
            name = "Updated Name",
            username = "updateduser",
            bio = "New bio",
            website = "example.com",
            isPrivate = true
        )

        val successfulTask = Tasks.forResult(null) as Task<Void>

        coEvery {
            documentReference.update(
                "name", profile.name,
                "username", profile.username,
                "bio", profile.bio,
                "website", profile.website,
                "isPrivate", profile.isPrivate
            )
        } returns successfulTask

        val result = profileRepository.editProfile(profile)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            documentReference.update(
                "name", profile.name,
                "username", profile.username,
                "bio", profile.bio,
                "website", profile.website,
                "isPrivate", profile.isPrivate
            )
        }
    }

    @Test
    fun `editProfile should return failure when update fails`() = runTest {
        val profile = Profile(
            userUid = "user123",
            name = "Test User",
            username = "testuser"
        )

        val failedTask: Task<Void> = Tasks.forException(Exception("Update failed"))

        coEvery {
            documentReference.update(
                "name", profile.name,
                "username", profile.username,
                "bio", profile.bio,
                "website", profile.website,
                "isPrivate", profile.isPrivate
            )
        } returns failedTask

        val result = profileRepository.editProfile(profile)

        assertTrue(result.isFailure)
        assertEquals("Update failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `deleteProfilePhoto should delete photo and update profile`() = runTest {
        val userUid = "user123"
        val photoUrl = "cloud_url"
        val profile = Profile(
            userUid = userUid,
            name = "Test User",
            username = "testuser",
            photoUrl = photoUrl
        )

        val getTask = Tasks.forResult(mockk<DocumentSnapshot> {
            every { toObject(Profile::class.java) } returns profile
        })

        val updateTask = Tasks.forResult(null) as Task<Void>

        coEvery { documentReference.get() } returns getTask
        coEvery { cloudinaryService.deleteImage(photoUrl) } returns true
        coEvery { documentReference.update("photoUrl", null) } returns updateTask

        val result = profileRepository.deleteProfilePhoto(userUid)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { cloudinaryService.deleteImage(photoUrl) }
        coVerify(exactly = 1) { documentReference.update("photoUrl", null) }
    }

    @Test
    fun `deleteProfilePhoto should succeed when no photo exists`() = runTest {
        val userUid = "user123"
        val profile = Profile(
            userUid = userUid,
            name = "Test User",
            username = "testuser",
            photoUrl = null
        )

        val getTask = Tasks.forResult(mockk<DocumentSnapshot> {
            every { toObject(Profile::class.java) } returns profile
        })

        val updateTask = Tasks.forResult(null) as Task<Void>

        coEvery { documentReference.get() } returns getTask
        coEvery { documentReference.update("photoUrl", null) } returns updateTask

        val result = profileRepository.deleteProfilePhoto(userUid)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { cloudinaryService.deleteImage(any()) }
        coVerify(exactly = 1) { documentReference.update("photoUrl", null) }
    }

    @Test
    fun `deleteProfile should delete profile and photo`() = runTest {
        val userUid = "user123"
        val photoUrl = "cloud_url"
        val profile = Profile(
            userUid = userUid,
            name = "Test User",
            username = "testuser",
            photoUrl = photoUrl
        )

        val getTask = Tasks.forResult(mockk<DocumentSnapshot> {
            every { toObject(Profile::class.java) } returns profile
        })

        val deleteTask = Tasks.forResult(null) as Task<Void>

        coEvery { documentReference.get() } returns getTask
        coEvery { cloudinaryService.deleteImage(photoUrl) } returns true
        coEvery { documentReference.delete() } returns deleteTask

        val result = profileRepository.deleteProfile(userUid)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { cloudinaryService.deleteImage(photoUrl) }
        coVerify(exactly = 1) { documentReference.delete() }
    }
}