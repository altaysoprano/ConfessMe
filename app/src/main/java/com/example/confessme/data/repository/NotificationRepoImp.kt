package com.example.confessme.data.repository

import android.util.Log
import com.example.confessme.data.model.Confession
import com.example.confessme.data.model.Notification
import com.example.confessme.util.UiState
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch

class NotificationRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore
) : NotificationRepo {

    override fun fetchNotificationsForUser(
        limit: Long,
        forNotifications: Boolean,
        result: (UiState<List<Notification>>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid
            val notificationsCollection = database.collection("notifications")
            val followersCollection =
                database.collection("users").document(currentUserUid).collection("followers")

            notificationsCollection
                .limit(limit)
                .whereEqualTo("userId", currentUserUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    val notificationList = mutableListOf<Notification>()
                    val batchDelete = database.batch()
                    val batchSeen = database.batch()

                    val tasks = mutableListOf<Task<*>>()

                    for (document in documents) {
                        val notification = document.toObject(Notification::class.java)

                        if (notification.confessionId.isNotEmpty()) {
                            val confessionRef = database.collection("confessions")
                                .document(notification.confessionId)

                            tasks.add(confessionRef.get().addOnSuccessListener { confessionDoc ->
                                verifyNotificationValidity(
                                    notification,
                                    confessionDoc,
                                    notificationsCollection,
                                    batchDelete,
                                    notificationList
                                )
                            })
                        } else {
                            val fromUserId = notification.fromUserId
                            val followerDocument = followersCollection.document(fromUserId)

                            tasks.add(followerDocument.get().addOnSuccessListener { followerDoc ->
                                if (!followerDoc.exists()) {
                                    batchDelete.delete(notificationsCollection.document(notification.id))
                                } else {
                                    notificationList.add(notification)
                                }
                            })
                        }

                        if (forNotifications) {
                            batchSeen.update(
                                notificationsCollection.document(document.id),
                                "seen",
                                true
                            )
                        }
                    }

                    Tasks.whenAllComplete(tasks)
                        .addOnCompleteListener {
                            batchDelete.commit().addOnCompleteListener {
                                val sortedNotificationsList =
                                    notificationList.sortedByDescending { it.timestamp.toString() }

                                if (forNotifications) {
                                    batchSeen.commit().addOnCompleteListener {
                                        result.invoke(UiState.Success(sortedNotificationsList))
                                    }
                                } else {
                                    result.invoke(UiState.Success(sortedNotificationsList))
                                }
                            }
                        }
                }
                .addOnFailureListener {
                    result.invoke(UiState.Failure("An error occurred. Please try again."))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }

    private fun verifyNotificationValidity(
        notification: Notification,
        confessionDoc: DocumentSnapshot,
        notificationsCollection: CollectionReference,
        batchDelete: WriteBatch,
        notificationList: MutableList<Notification>
    ) {
        when (notification.description) {
            " confessed:" -> {
                if (confessionDoc.exists()) {
                    notificationList.add(notification)
                } else {
                    batchDelete.delete(notificationsCollection.document(notification.id))
                }
            }

            " liked this confession:" -> {
                val favorited = confessionDoc.getBoolean("favorited") ?: false
                if (!favorited) {
                    batchDelete.delete(notificationsCollection.document(notification.id))
                } else {
                    notificationList.add(notification)
                }
            }

            " replied to this confession:" -> {
                val answered = confessionDoc.getBoolean("answered") ?: false
                if (!answered) {
                    batchDelete.delete(notificationsCollection.document(notification.id))
                } else {
                    notificationList.add(notification)
                }
            }

            " liked this answer:" -> {
                val answerMap = confessionDoc.get("answer") as? Map<String, Any>
                val favorited = answerMap?.get("favorited") as? Boolean ?: false
                if (!favorited) {
                    batchDelete.delete(notificationsCollection.document(notification.id))
                } else {
                    notificationList.add(notification)
                }
            }
        }
    }
}