package com.github.teranes10.androidutils.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseUtil {

    suspend fun <T : Any> getAllDocs(collectionName: String, clazz: Class<T>): List<T> {
        return try {
            val snapshot = FirebaseFirestore.getInstance().collection(collectionName).get().await()
            snapshot.toObjects(clazz)
        } catch (e: Exception) {
            Log.e("FirebaseUtil", "Error getting documents: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun <T : Any> getDocById(collectionName: String, docId: String, clazz: Class<T>): T? {
        return try {
            val document = FirebaseFirestore.getInstance().collection(collectionName).document(docId).get().await()
            document.toObject(clazz)
        } catch (e: Exception) {
            Log.e("FirebaseUtil", "Error getting document: ${e.localizedMessage}")
            null
        }
    }
}