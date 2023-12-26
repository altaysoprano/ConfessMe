package com.example.confessme.data.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.confessme.R
import com.example.confessme.data.model.Answer
import com.example.confessme.data.model.Confession
import com.example.confessme.data.model.Notification
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.Constants
import com.example.confessme.util.MyUtils
import com.example.confessme.util.NotificationType
import com.example.confessme.util.UiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.rpc.context.AttributeContext
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ConfessionRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ConfessionRepo {

    override fun addConfession(
        userUid: String,
        confessionText: String,
        isAnonymous: Boolean,
        result: (UiState<String>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            database.collection("users").document(currentUserUid)
                .get()
                .addOnSuccessListener { currentUserDocument ->
                    if (currentUserDocument.exists()) {
                        val fromUserImageUrl =
                            if (isAnonymous) "" else currentUserDocument.getString("imageUrl")
                        val fromUserUsername =
                            if (isAnonymous) "Anonymous" else currentUserDocument.getString("userName")
                        val fromUserEmail = currentUserDocument.getString("email")
                        val anonymousId =
                            if (isAnonymous) currentUserDocument.getString("uid") else ""
                        val fromUserUid =
                            if (isAnonymous) "" else currentUserDocument.getString("uid")

                        database.collection("users").document(userUid)
                            .get()
                            .addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {
                                    val userDocument = documentSnapshot

                                    val confessionCollection = database.collection("confessions")
                                    val newConfessionDocument = confessionCollection.document()
                                    val confessionId = newConfessionDocument.id
                                    val toFcmToken = userDocument.getString("token")
                                    val fromFcmToken = currentUserDocument.getString("token") ?: ""
                                    val language = userDocument.getString("language") ?: ""

                                    val confessionData = hashMapOf(
                                        "id" to confessionId,
                                        "userId" to userUid,
                                        "fromUserId" to fromUserUid,
                                        "userToken" to toFcmToken,
                                        "fromUserToken" to fromFcmToken,
                                        "text" to confessionText,
                                        "anonymousId" to anonymousId,
                                        "username" to userDocument.getString("userName"),
                                        "email" to userDocument.getString("email"),
                                        "imageUrl" to userDocument.getString("imageUrl"),
                                        "fromUserEmail" to fromUserEmail,
                                        "fromUserUsername" to fromUserUsername,
                                        "fromUserImageUrl" to fromUserImageUrl,
                                        "timestamp" to FieldValue.serverTimestamp()
                                    )

                                    newConfessionDocument.set(confessionData)
                                        .addOnSuccessListener {
                                            result.invoke(UiState.Success(context.getString(R.string.confessed_successfully)))
                                            val title = MyUtils.getNotificationText(language, NotificationType.Confessed)
                                            if (toFcmToken != "" && toFcmToken != null) {
                                                sendNotification(
                                                    "$fromUserUsername ",
                                                    title,
                                                    confessionText,
                                                    userUid,
                                                    toFcmToken
                                                )
                                            }
                                            addNotificationToUser(
                                                userId = userUid,
                                                fromUserId = fromUserUid ?: "",
                                                fromUserToken = fromFcmToken,
                                                confessionText = confessionText,
                                                fromUserUsername = fromUserUsername ?: "",
                                                fromUserImageUrl = fromUserImageUrl ?: "",
                                                confessionId = confessionId,
                                                notificationType = NotificationType.Confessed
                                            )
                                        }
                                        .addOnFailureListener { exception ->
                                            result.invoke(UiState.Failure(context.getString(R.string.could_not_confess)))
                                        }
                                } else {
                                    result.invoke(UiState.Failure(context.getString(R.string.user_not_found)))
                                }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure(exception.localizedMessage))
                            }
                    } else {
                        result.invoke(UiState.Failure(context.getString(R.string.user_not_found)))
                    }
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    override fun fetchConfessions(
        userUid: String,
        limit: Long,
        confessionCategory: ConfessionCategory,
        result: (UiState<List<Confession>>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            val confessionCollection = database.collection("confessions")
            var confessionQuery: Query = confessionCollection

            when (confessionCategory) {
                ConfessionCategory.MY_CONFESSIONS -> {
                    confessionQuery = confessionQuery.whereEqualTo("fromUserId", currentUserUid)
                }

                ConfessionCategory.CONFESSIONS_TO_ME -> {
                    confessionQuery = confessionQuery.whereEqualTo("userId", currentUserUid)
                }

                ConfessionCategory.OTHER_USER_CONFESSIONS -> {
                    confessionQuery = confessionQuery.whereEqualTo("userId", userUid)
                }

                ConfessionCategory.CONFESSIONS_TO_OTHERS -> {
                    confessionQuery = confessionQuery.whereEqualTo("fromUserId", userUid)
                }
            }

            confessionQuery
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener { documents ->
                    val confessionList = mutableListOf<Confession>()

                    for (document in documents) {
                        val confession = document.toObject(Confession::class.java)
                        confessionList.add(confession)
                    }

                    result.invoke(UiState.Success(confessionList))
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    override fun getConfession(confessionId: String, result: (UiState<Confession?>) -> Unit) {
        val confessionDocument = database.collection("confessions").document(confessionId)

        confessionDocument.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val confession = documentSnapshot.toObject(Confession::class.java)
                    result.invoke(UiState.Success(confession))
                } else {
                    result.invoke(UiState.Failure(context.getString(R.string.confession_not_found)))
                }
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(context.getString(R.string.an_error_occurred_while_loading_the_confession)))
            }
    }

    override fun fetchFollowedUsersConfessions(
        limit: Long,
        result: (UiState<List<Confession>>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            val followingCollection =
                database.collection("users").document(currentUserUid).collection("following")

            followingCollection.get()
                .addOnSuccessListener { querySnapshot ->
                    val followedUserIds = querySnapshot.documents.map { it.id }

                    val confessionsList = mutableListOf<Confession>()

                    if (followedUserIds.isNotEmpty()) {
                        val confessionsCollection = database.collection("confessions")

                        confessionsCollection
                            .whereIn("userId", followedUserIds)
                            .limit(limit)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                val confessions = querySnapshot.toObjects(Confession::class.java)
                                confessionsList.addAll(confessions)

                                result.invoke(UiState.Success(confessionsList))
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure(context.getString(R.string.an_error_occurred_while_loading_confessions)))
                            }
                    } else {
                        result.invoke(UiState.Success(confessionsList))
                    }
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(context.getString(R.string.an_error_occurred_while_fetching_followed_users)))
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    override fun addAnswer(
        confessionId: String,
        answerText: String,
        result: (UiState<Confession?>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {

            val confessionDocRef = database.collection("confessions")
                .whereEqualTo("id", confessionId)

            confessionDocRef.get()
                .addOnSuccessListener { confessionQuerySnapshot ->
                    if (!confessionQuerySnapshot.isEmpty) {
                        val confessionDoc = confessionQuerySnapshot.documents[0]

                        val fromUserUsername = confessionDoc.getString("username") ?: ""
                        val fromUserUserId = confessionDoc.getString("fromUserId") ?: ""
                        val anonymousId = confessionDoc.getString("anonymousId") ?: ""
                        val userIdToNotification =
                            if (fromUserUserId.isNotBlank()) fromUserUserId else anonymousId
                        val userId = confessionDoc.getString("userId") ?: ""
                        val fromUserImageUrl = confessionDoc.getString("imageUrl") ?: ""
                        val fromUserEmail = confessionDoc.getString("email") ?: ""
                        val fromUserToken = confessionDoc.getString("fromUserToken") ?: ""
                        val userToken = confessionDoc.getString("userToken") ?: ""
                        val fcmToken = fromUserToken
                        val username = confessionDoc.getString("fromUserUsername") ?: ""
                        val imageUrl = confessionDoc.getString("fromUserImageUrl") ?: ""
                        val email = confessionDoc.getString("fromUserEmail") ?: ""
                        val confessionText = confessionDoc.getString("text") ?: ""

                        val answerData = Answer(
                            text = answerText,
                            username = username,
                            email = email,
                            fromUserUsername = fromUserUsername,
                            fromUserImageUrl = fromUserImageUrl,
                            fromUserEmail = fromUserEmail,
                            imageUrl = imageUrl,
                            timestamp = FieldValue.serverTimestamp(),
                            isExpanded = false
                        )

                        val batch = database.batch()

                        val updatedData = mapOf("answered" to true)
                        batch.update(confessionDoc.reference, updatedData)

                        val answerField = mapOf("answer" to answerData)
                        batch.set(confessionDoc.reference, answerField, SetOptions.merge())

                        batch.commit()
                            .addOnSuccessListener {
                                confessionDocRef.get()
                                    .addOnSuccessListener { updatedConfessionDocumentSnapshot ->
                                        result.invoke(
                                            UiState.Success(
                                                updatedConfessionDocumentSnapshot.documents[0].toObject(
                                                    Confession::class.java
                                                )
                                            )
                                        )
                                        if (userIdToNotification.isNotBlank()) {
                                            sendNotification(
                                                "$fromUserUsername ",
                                                context.getString(R.string.replied_to_this_confession),
                                                confessionText,
                                                userIdToNotification,
                                                fcmToken
                                            )
                                            addNotificationToUser(
                                                userId = userIdToNotification,
                                                fromUserId = userId,
                                                fromUserToken = userToken,
                                                confessionText = confessionText,
                                                fromUserUsername = fromUserUsername,
                                                fromUserImageUrl = fromUserImageUrl,
                                                confessionId = confessionId,
                                                notificationType = NotificationType.ConfessionReply
                                            )
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        result.invoke(UiState.Failure(exception.localizedMessage))
                                    }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure(context.getString(R.string.unable_to_send_response)))
                            }
                    } else {
                        result.invoke(UiState.Failure(context.getString(R.string.confession_not_found)))
                    }
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    override fun addFavorite(
        favorited: Boolean,
        confessionId: String,
        result: (UiState<Confession?>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {

            val confessionDocRef = database.collection("confessions")
                .whereEqualTo("id", confessionId)

            confessionDocRef.get()
                .addOnSuccessListener { confessionQuerySnapshot ->
                    if (!confessionQuerySnapshot.isEmpty) {
                        val confessionDoc = confessionQuerySnapshot.documents[0]
                        val fcmToken = confessionDoc.getString("fromUserToken") ?: ""
                        val fromFcmToken = confessionDoc.getString("userToken") ?: ""
                        val fromUserId = confessionDoc.getString("fromUserId") ?: ""
                        val anonymousId = confessionDoc.getString("anonymousId") ?: ""
                        val userIdToNotification =
                            if (fromUserId.isNotBlank()) fromUserId else anonymousId
                        val userId = confessionDoc.getString("userId") ?: ""
                        val username = confessionDoc.getString("username") ?: ""
                        val userImageUrl = confessionDoc.getString("imageUrl") ?: ""
                        val confessionText = confessionDoc.getString("text") ?: ""

                        val updatedData = mapOf("favorited" to favorited)

                        val documentRef = database.collection("confessions")
                            .document(confessionQuerySnapshot.documents[0].id)

                        documentRef.update(updatedData)
                            .addOnSuccessListener {
                                confessionDocRef.get()
                                    .addOnSuccessListener { updatedConfessionDocumentSnapshot ->
                                        result.invoke(
                                            UiState.Success(
                                                updatedConfessionDocumentSnapshot.documents[0].toObject(
                                                    Confession::class.java
                                                )
                                            )
                                        )
                                        if (favorited && userIdToNotification.isNotBlank()) {
                                            sendNotification(
                                                "$username ",
                                                context.getString(R.string.liked_this_confession),
                                                confessionText,
                                                userIdToNotification,
                                                fcmToken
                                            )
                                            addNotificationToUser(
                                                userId = userIdToNotification,
                                                fromUserId = userId,
                                                fromUserToken = fromFcmToken,
                                                confessionText = confessionText,
                                                fromUserUsername = username,
                                                fromUserImageUrl = userImageUrl,
                                                confessionId = confessionId,
                                                notificationType = NotificationType.ConfessionLike
                                            )
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        result.invoke(UiState.Failure(exception.localizedMessage))
                                    }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure(exception.localizedMessage))
                            }
                    } else {
                        result.invoke(UiState.Failure(context.getString(R.string.confession_not_found)))
                    }
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    override suspend fun favoriteAnswer(
        isFavorited: Boolean,
        confessionId: String,
        result: (UiState<Confession?>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {

            val confessionDocRef = database.collection("confessions")
                .whereEqualTo("id", confessionId)

            confessionDocRef.get()
                .addOnSuccessListener { confessionQuerySnapshot ->
                    if (!confessionQuerySnapshot.isEmpty) {
                        val confessionDocumentSnapshot = confessionQuerySnapshot.documents[0]

                        val fromUserUsername =
                            confessionDocumentSnapshot.getString("fromUserUsername") ?: ""
                        val fromUserImageUrl =
                            confessionDocumentSnapshot.getString("fromUserImageUrl") ?: ""
                        val userId = confessionDocumentSnapshot.getString("userId") ?: ""
                        val fromUserId = confessionDocumentSnapshot.getString("fromUserId") ?: ""
                        val userToken = confessionDocumentSnapshot.getString("userToken") ?: ""
                        val fromUserToken =
                            confessionDocumentSnapshot.getString("fromUserToken") ?: ""
                        val fcmToken = userToken

                        val answerMap =
                            confessionDocumentSnapshot.get("answer") as MutableMap<String, Any>?
                        if (answerMap != null) {
                            answerMap["favorited"] = isFavorited

                            val answerText = answerMap.get("text") as? String ?: ""

                            confessionDocumentSnapshot.reference.update("answer", answerMap)
                                .addOnSuccessListener {
                                    confessionDocRef.get()
                                        .addOnSuccessListener { updatedConfessionDocumentSnapshot ->
                                            result.invoke(
                                                UiState.Success(
                                                    updatedConfessionDocumentSnapshot.documents[0].toObject(
                                                        Confession::class.java
                                                    )
                                                )
                                            )
                                            if (isFavorited) {
                                                sendNotification(
                                                    "$fromUserUsername ",
                                                    context.getString(R.string.liked_this_answer),
                                                    "$answerText",
                                                    userId,
                                                    fcmToken
                                                )
                                                addNotificationToUser(
                                                    userId = userId,
                                                    fromUserId = fromUserId,
                                                    fromUserToken = fromUserToken,
                                                    confessionText = answerText,
                                                    fromUserUsername = fromUserUsername,
                                                    fromUserImageUrl = fromUserImageUrl,
                                                    confessionId = confessionId,
                                                    notificationType = NotificationType.AnswerLike
                                                )
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            result.invoke(UiState.Failure(exception.localizedMessage))
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    result.invoke(UiState.Failure(context.getString(R.string.the_like_couldn_t_be_completed_please_try_again)))
                                }
                        } else {
                            result.invoke(UiState.Failure(context.getString(R.string.answer_not_found)))
                        }
                    } else {
                        result.invoke(UiState.Failure(context.getString(R.string.confession_not_found)))
                    }
                }.addOnFailureListener {
                    result.invoke(UiState.Failure(context.getString(R.string.confession_not_found)))
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    override fun deleteAnswer(confessionId: String, result: (UiState<Confession?>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user != null) {

            val batch = database.batch()

            val confessionDocRef = database.collection("confessions")
                .whereEqualTo("id", confessionId)

            confessionDocRef.get()
                .addOnSuccessListener { confessionQuerySnapshot ->
                    if (!confessionQuerySnapshot.isEmpty) {
                        val confessionDocumentSnapshot = confessionQuerySnapshot.documents[0]

                        val answerMap =
                            confessionDocumentSnapshot.get("answer") as MutableMap<String, Any>?
                        if (answerMap != null) {

                            val answerFieldUpdate = mapOf("answer" to FieldValue.delete())

                            val answeredFieldUpdate = mapOf("answered" to false)

                            val confessionRef = confessionDocumentSnapshot.reference
                            batch.update(confessionRef, answerFieldUpdate)
                            batch.update(confessionRef, answeredFieldUpdate)

                            batch.commit()
                                .addOnSuccessListener {
                                    confessionDocRef.get()
                                        .addOnSuccessListener { updatedConfessionDocumentSnapshot ->
                                            result.invoke(
                                                UiState.Success(
                                                    updatedConfessionDocumentSnapshot.documents[0].toObject(
                                                        Confession::class.java
                                                    )
                                                )
                                            )
                                        }
                                        .addOnFailureListener { exception ->
                                            result.invoke(
                                                UiState.Failure(
                                                    exception.localizedMessage
                                                )
                                            )
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    result.invoke(
                                        UiState.Failure(
                                            exception.localizedMessage
                                        )
                                    )
                                }
                        } else {
                            result.invoke(UiState.Failure(context.getString(R.string.answer_not_found)))
                        }
                    } else {
                        result.invoke(UiState.Failure(context.getString(R.string.confession_not_found)))
                    }
                }.addOnFailureListener {
                    result.invoke(UiState.Failure(context.getString(R.string.confession_not_found)))
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    override fun deleteConfession(confessionId: String, result: (UiState<Confession?>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val batch = database.batch()

            val confessionDocRef = database.collection("confessions")
                .whereEqualTo("id", confessionId)

            confessionDocRef.get()
                .addOnSuccessListener { confessionQuerySnapshot ->
                    if (!confessionQuerySnapshot.isEmpty) {
                        val confessionDocumentSnapshot = confessionQuerySnapshot.documents[0]

                        val confessionRef = confessionDocumentSnapshot.reference
                        batch.delete(confessionRef)

                        val deletedConfession =
                            confessionDocumentSnapshot.toObject(Confession::class.java)

                        batch.commit()
                            .addOnSuccessListener {
                                confessionDocRef.get()
                                    .addOnSuccessListener { updatedConfessionDocumentSnapshot ->
                                        result.invoke(
                                            UiState.Success(
                                                deletedConfession
                                            )
                                        )
                                    }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(
                                    UiState.Failure(
                                        context.getString(R.string.could_not_be_deleted)
                                    )
                                )
                            }
                    } else {
                        result.invoke(UiState.Failure(context.getString(R.string.confession_not_found)))
                    }
                }.addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(context.getString(R.string.confession_not_found)))
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    override fun addBookmark(
        confessionId: String,
        timestamp: String,
        userUid: String,
        result: (UiState<String>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            val bookmarksCollection =
                database.collection("users").document(currentUserUid).collection("bookmarks")
            val newBookmarkDocument = bookmarksCollection.document(confessionId)

            val data = mapOf(
                "userUid" to userUid,
                "timestamp" to FieldValue.serverTimestamp()
            )

            newBookmarkDocument.set(data)
                .addOnSuccessListener {
                    result.invoke(UiState.Success(context.getString(R.string.bookmark_added_successfully)))
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    override fun fetchBookmarks(limit: Long, result: (UiState<List<Confession?>>) -> Unit) {
        val userUid = firebaseAuth.currentUser?.uid

        if (userUid != null) {
            val bookmarksCollection =
                database.collection("users").document(userUid).collection("bookmarks")

            bookmarksCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener { documents ->
                    val bookmarkedConfessions = mutableListOf<Confession?>()

                    val bookmarkCount = documents.size()

                    if (bookmarkCount == 0) {
                        result.invoke(UiState.Success(bookmarkedConfessions))
                        return@addOnSuccessListener
                    }

                    val timestampMap = mutableMapOf<String, Timestamp>()

                    var fetchedConfessionCount = 0

                    for (document in documents) {
                        val confessionId = document.id

                        val myConfessionsCollection = database.collection("confessions")
                        val myConfessionDocument = myConfessionsCollection.document(confessionId)

                        myConfessionDocument.get()
                            .addOnSuccessListener { myConfessionSnapshot ->
                                if (myConfessionSnapshot.exists()) {
                                    val bookmarkedConfession =
                                        myConfessionSnapshot.toObject(Confession::class.java)
                                    bookmarkedConfessions.add(bookmarkedConfession)
                                    timestampMap[confessionId] =
                                        bookmarkedConfession?.timestamp as Timestamp
                                    bookmarkedConfession.timestamp =
                                        document.getTimestamp("timestamp") as Timestamp
                                } else {
                                    bookmarksCollection.document(confessionId).delete()
                                }

                                fetchedConfessionCount++

                                if (fetchedConfessionCount == bookmarkCount || fetchedConfessionCount == limit.toInt()) {
                                    val sortedBookmarkedConfessions =
                                        bookmarkedConfessions.sortedByDescending { it?.timestamp.toString() }

                                    sortedBookmarkedConfessions.forEach { confession ->
                                        val originalTimestamp = timestampMap[confession?.id]
                                        confession?.timestamp = originalTimestamp
                                    }

                                    result.invoke(UiState.Success(sortedBookmarkedConfessions))
                                }
                            }
                            .addOnFailureListener { exception ->
                                fetchedConfessionCount++

                                if (fetchedConfessionCount == bookmarkCount || fetchedConfessionCount == limit.toInt()) {
                                    val sortedBookmarkedConfessions =
                                        bookmarkedConfessions.sortedByDescending { it?.timestamp.toString() }

                                    sortedBookmarkedConfessions.forEach { confession ->
                                        val originalTimestamp = timestampMap[confession?.id]
                                        confession?.timestamp = originalTimestamp
                                    }

                                    result.invoke(UiState.Success(sortedBookmarkedConfessions))
                                }
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(context.getString(R.string.failed_to_retrieve_bookmarks)))
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    override fun removeBookmark(
        confessionId: String,
        result: (UiState<DocumentReference>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            val bookmarksCollection =
                database.collection("users").document(currentUserUid).collection("bookmarks")
            val bookmarkDocument = bookmarksCollection.document(confessionId)

            bookmarkDocument.delete()
                .addOnSuccessListener {
                    result.invoke(UiState.Success(bookmarkDocument))
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.user_not_authenticated)))
        }
    }

    private fun sendNotification(
        username: String,
        title: String,
        message: String,
        currentUserId: String,
        token: String
    ) {

        try {
            val jsonObject = JSONObject()

            val notificationObject = JSONObject()
            val notificationText = username + title

            notificationObject.put("title", notificationText)
            notificationObject.put("body", message)

            val dataObject = JSONObject()
            dataObject.put("userId", currentUserId)

            jsonObject.put("notification", notificationObject)
            jsonObject.put("data", dataObject)
            jsonObject.put("to", token)

            callApi(jsonObject)
        } catch (e: Exception) {

        }
    }

    private fun callApi(jsonObject: JSONObject) {
        val JSON: MediaType = "application/json; charset=UTF-8".toMediaType()
        val client = OkHttpClient()
        val url = "https://fcm.googleapis.com/fcm/send"
        val body: RequestBody = RequestBody.create(JSON, jsonObject.toString())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer ${Constants.FCM_TOKEN}")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {

                    } else {

                    }
                }
            }
        })
    }

    private fun addNotificationToUser(
        userId: String,
        fromUserId: String,
        fromUserToken: String,
        confessionText: String,
        fromUserUsername: String,
        fromUserImageUrl: String,
        confessionId: String,
        notificationType: NotificationType,
    ) {
        val notificationsCollection = database.collection("notifications")

        val notification = Notification(
            confessionId = confessionId,
            userId = userId,
            fromUserId = fromUserId,
            fromUserToken = fromUserToken,
            text = confessionText,
            fromUserUsername = fromUserUsername,
            fromUserImageUrl = fromUserImageUrl,
            type = notificationType.toString(),
            timestamp = FieldValue.serverTimestamp()
        )

        notificationsCollection.whereEqualTo("confessionId", confessionId)
            .whereEqualTo("userId", userId)
            .whereEqualTo("fromUserId", fromUserId)
            .whereEqualTo("type", notificationType.toString())
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    notificationsCollection.add(notification)
                        .addOnSuccessListener { documentReference ->
                            val notificationId = documentReference.id
                            val updatedNotification = notification.copy(id = notificationId)

                            notificationsCollection.document(notificationId)
                                .set(updatedNotification)
                        }
                } else {
                    val existingNotification = querySnapshot.documents[0]

                    val notificationId = existingNotification.id
                    val updatedNotification = notification.copy(id = notificationId)

                    notificationsCollection.document(notificationId)
                        .set(updatedNotification)
                }
            }
    }
}