package com.example.instagramapp

import com.example.instagramapp.repos.UserRepository
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userRepository: UserRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        firebaseAuth = mockk(relaxed = true)
        userRepository = UserRepository(firebaseAuth)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signUp should return user on success`() = runTest {
        val user = mockk<FirebaseUser>(relaxed = true)
        val authResult = mockk<AuthResult> {
            every { this@mockk.user } returns user
        }

        val completedTask: Task<AuthResult> = Tasks.forResult(authResult)

        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns completedTask

        val result = userRepository.signUp("test@mail.com", "password")

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
    }


    @Test
    fun `signUp should return failure on exception`() = runTest {
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } throws RuntimeException("Signup error")

        val result = userRepository.signUp("test@mail.com", "password")

        assertTrue(result.isFailure)
        assertEquals("Signup error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn should return user on success`() = runTest {
        val user = mockk<FirebaseUser>(relaxed = true)
        val authResult = mockk<AuthResult> {
            every { this@mockk.user } returns user
        }

        val completedTask: Task<AuthResult> = Tasks.forResult(authResult)

        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns completedTask

        val result = userRepository.signIn("test@mail.com", "password")

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
    }

    @Test
    fun `signIn should return failure on exception`() = runTest {
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } throws RuntimeException("Signin error")

        val result = userRepository.signIn("test@mail.com", "password")

        assertTrue(result.isFailure)
        assertEquals("Signin error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `sendEmailVerification should succeed when user is available`() = runTest {
        val user = mockk<FirebaseUser>()
        val completedTask: Task<Void> = Tasks.forResult(null)

        every { user.sendEmailVerification() } returns completedTask
        every { firebaseAuth.currentUser } returns user

        val result = userRepository.sendEmailVerification()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `sendEmailVerification should fail when user is null`() = runTest {
        every { firebaseAuth.currentUser } returns null

        val result = userRepository.sendEmailVerification()

        assertTrue(result.isFailure)
        assertEquals("User not logged in", result.exceptionOrNull()?.message)
    }

    @Test
    fun `reloadUser should succeed when user is valid and reload is successful`() = runTest {
        val mockFirebaseAuth = mockk<FirebaseAuth>()

        val mockUser = mockk<FirebaseUser>()

        every { mockFirebaseAuth.currentUser } returns mockUser

        every { mockFirebaseAuth.addAuthStateListener(any()) } just runs

        val userRepository = UserRepository(mockFirebaseAuth)

        userRepository.reloadUser()
    }

    @Test
    fun `reloadUser should fail when user is null`() = runTest {
        every { firebaseAuth.currentUser } returns null

        val result = userRepository.reloadUser()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `reloadUser should fail when reload fails`() = runTest {
        val user = mockk<FirebaseUser>()
        val task = mockk<Task<Void>>()

        every { task.isSuccessful } returns false
        every { task.exception } returns RuntimeException("Reload error")
        every { task.isComplete } returns true
        every { user.reload() } returns task
        every { firebaseAuth.currentUser } returns user

        val result = userRepository.reloadUser()

        assertTrue(result.isFailure)
        assertEquals("Reload error", result.exceptionOrNull()?.message)
    }
}