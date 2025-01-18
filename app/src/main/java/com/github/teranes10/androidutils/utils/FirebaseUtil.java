package com.github.teranes10.androidutils.utils;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FirebaseUtil {

    public static <T> CompletableFuture<List<T>> getAllDocs(String collectionName, Class<T> clazz) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();

        FirebaseFirestore.getInstance().collection(collectionName).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            future.complete(querySnapshot.toObjects(clazz));
                        } else {
                            future.complete(new ArrayList<>());
                        }
                    } else {
                        future.completeExceptionally(task.getException());
                    }
                });

        return future;
    }

    public static <T> CompletableFuture<T> getDocById(String collectionName, String docId, Class<T> clazz) {
        CompletableFuture<T> future = new CompletableFuture<>();

        FirebaseFirestore.getInstance().collection(collectionName).document(docId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            future.complete(document.toObject(clazz));
                        } else {
                            future.complete(null);
                        }
                    } else {
                        future.completeExceptionally(task.getException());
                    }
                });

        return future;
    }
}
