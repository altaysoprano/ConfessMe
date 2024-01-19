package com.example.confessme.data.repository

import android.content.Context
import com.example.confessme.R
import com.example.confessme.data.model.Notification
import com.example.confessme.presentation.home.notifications.NotificationType
import com.example.confessme.presentation.utils.UiState
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import dagger.hilt.android.qualifiers.ApplicationContext

class NotificationRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore,
    @ApplicationContext private val context: Context
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
                    result.invoke(UiState.Failure(context.getString(R.string.an_error_occurred_please_try_again)))
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    private fun verifyNotificationValidity(
        notification: Notification,
        confessionDoc: DocumentSnapshot,
        notificationsCollection: CollectionReference,
        batchDelete: WriteBatch,
        notificationList: MutableList<Notification>
    ) {
        when (notification.type) {
            NotificationType.Confessed.toString() -> {
                if (confessionDoc.exists()) {
                    notificationList.add(notification)
                } else {
                    batchDelete.delete(notificationsCollection.document(notification.id))
                }
            }

            NotificationType.ConfessionLike.toString() -> {
                val favorited = confessionDoc.getBoolean("favorited") ?: false
                if (!favorited) {
                    batchDelete.delete(notificationsCollection.document(notification.id))
                } else {
                    notificationList.add(notification)
                }
            }

            NotificationType.ConfessionReply.toString() -> {
                val answered = confessionDoc.getBoolean("answered") ?: false
                if (!answered) {
                    batchDelete.delete(notificationsCollection.document(notification.id))
                } else {
                    notificationList.add(notification)
                }
            }

            NotificationType.AnswerLike.toString() -> {
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