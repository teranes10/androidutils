package com.github.teranes10.androidutils.extensions

import android.content.Context
import com.github.teranes10.androidutils.extensions.ContextExtensions.getUriForFile
import com.github.teranes10.androidutils.extensions.ExceptionExtensions.displayMessage
import com.github.teranes10.androidutils.models.Outcome
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.io.File

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

    suspend fun FirebaseStorage.uploadFile(
        context: Context,
        file: File,
        directory: String? = null,
        fileName: String? = null,
        removeFile: Boolean = true,
        metadata: StorageMetadata? = null,
        onProgress: ((percentage: Double) -> Unit)? = null
    ): Outcome<String> {
        if (!file.exists()) {
            return Outcome.fail("File does not exist: ${file.absolutePath}")
        }

        val uri = context.getUriForFile(file)
        val serverFileName = fileName ?: uri.lastPathSegment ?: file.name
        val resolvePath = directory?.takeIf { it.isNotBlank() }?.let { "$directory/$serverFileName" } ?: serverFileName
        val fileRef = this.reference.child(resolvePath)

        val inputStream = context.contentResolver.openInputStream(uri) ?: return Outcome.fail("Unable to open input stream for URI: $uri")

        val uploadTask = if (metadata != null) fileRef.putStream(inputStream, metadata) else fileRef.putStream(inputStream)

        return try {
            uploadTask.addOnProgressListener {
                val progress = (100.0 * it.bytesTransferred) / it.totalByteCount
                onProgress?.invoke(progress)
            }

            uploadTask.await()

            if (removeFile) {
                file.delete()
            }

            val downloadUrl = fileRef.downloadUrl.await()
            Outcome.ok(downloadUrl.toString(), "File uploaded successfully.")
        } catch (e: Exception) {
            Outcome.fail("File upload failed. ${e.displayMessage}")
        }
    }
}