package com.example.confessme.data.repository

import com.example.confessme.data.model.Notification
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore
): NotificationRepo {

    override fun fetchNotificationsForUser(
        limit: Long,
        forNotifications: Boolean,
        result: (UiState<List<Notification>>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            val notificationsCollection = database.collection("notifications")

            notificationsCollection
                .limit(limit)
                .whereEqualTo("userId", currentUserUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val notificationList = mutableListOf<Notification>()

                    for (document in documents) {
                        val notification = document.toObject(Notification::class.java)
                        notificationList.add(notification)
                    }

                    if (forNotifications) {
                        val batch = database.batch()
                        for (document in documents) {
                            val notificationRef = notificationsCollection.document(document.id)
                            batch.update(notificationRef, "seen", true)
                        }

                        batch.commit().addOnSuccessListener {
                            result.invoke(UiState.Success(notificationList))
                        }.addOnFailureListener { exception ->
                            result.invoke(UiState.Failure(exception.localizedMessage))
                        }
                    } else {
                        result.invoke(UiState.Success(notificationList))
                    }
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }

}