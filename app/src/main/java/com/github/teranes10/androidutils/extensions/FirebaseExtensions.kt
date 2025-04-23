package com.github.teranes10.androidutils.extensions

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseExtensions {

    suspend fun <T> FirebaseFirestore.getAllDocs(collectionId: String, clazz: Class<T>): List<T>? {
        val snapshot = this.collection(collectionId).get().await()
        return if (snapshot.isEmpty && !snapshot.metadata.isFromCache) null
        else snapshot.toObjects(clazz)
    }

    suspend fun <T> FirebaseFirestore.getDoc(collectionId: String, docId: String, classType: Class<T>): T? {
        val doc = this.collection(collectionId).document(docId).get().await()
        return if (doc.exists()) {
            doc.toObject(classType)
        } else {
            null
        }
    }
}