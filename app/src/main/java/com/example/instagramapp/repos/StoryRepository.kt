package com.example.instagramapp.repos

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.cloudinary.Cloudinary
import com.example.instagramapp.models.Story
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.tasks.await // Убедись, что этот импорт есть в файле

class StoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinary: Cloudinary // Инстанс Cloudinary
) {

    // Коллекция историй в Firestore
    private val storiesCollection: CollectionReference = firestore.collection("stories")

    // Создание новой истории
    /*suspend fun createStory(story: Story) {
        // Загружаем изображение на Cloudinary
        val photoUrl = uploadToCloudinary(story.photoUrl)

        // Создаем объект истории с обновленным URL
        val newStory = story.copy(photoUrl = photoUrl)

        // Добавляем историю в Firestore
        storiesCollection.document(newStory.storyUuid.toString()).set(newStory).await()
    }*/

    // Получение активных историй пользователя
    suspend fun getActiveStories(userUid: String): List<Story> {
        return storiesCollection
            .whereEqualTo("authorUid", userUid)
            .whereEqualTo("isVisible", true)
            .whereGreaterThan("expirationTime", Date()) // Получаем истории, которые еще не истекли
            .get()
            .await()
            .toObjects(Story::class.java)
    }

    // Получение архивных историй пользователя
    suspend fun getArchivedStories(userUid: String): List<Story> {
        return storiesCollection
            .whereEqualTo("authorUid", userUid)
            .whereEqualTo("isVisible", false)
            .get()
            .await()
            .toObjects(Story::class.java)
    }

    // Удаление истории
    suspend fun deleteStory(storyUuid: UUID) {
        storiesCollection.document(storyUuid.toString()).delete().await()
    }

    // Лайк истории
    suspend fun likeStory(storyUuid: UUID) {
        val docRef = storiesCollection.document(storyUuid.toString())
        val story = docRef.get().await().toObject(Story::class.java)
        if (story != null) {
            val updatedStory = story.copy(likes = story.likes + 1) // Увеличиваем лайки
            docRef.set(updatedStory).await()
        }
    }

    // Метод загрузки фото на Cloudinary
    /*private suspend fun uploadToCloudinary(photoUrl: String): String {
        val response = cloudinary.uploader().upload(photoUrl, ObjectUtils.emptyMap()).await()
        return response["url"] as String
    }*/
}
