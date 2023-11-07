package com.example.confessme.data.repository

import com.example.confessme.data.model.Answer
import com.example.confessme.data.model.Confession
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class ConfessionRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore,
) : ConfessionRepo {

    override fun addConfession(
        userUid: String,
        confessionText: String,
        result: (UiState<String>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            database.collection("users").document(currentUserUid)
                .get()
                .addOnSuccessListener { currentUserDocument ->
                    if (currentUserDocument.exists()) {
                        val fromUserImageUrl = currentUserDocument.getString("imageUrl")
                        val fromUserUsername = currentUserDocument.getString("userName")
                        val fromUserEmail = currentUserDocument.getString("email")
                        val fromUserUid = currentUserDocument.getString("uid")

                        database.collection("users").document(userUid)
                            .get()
                            .addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {
                                    val userDocument = documentSnapshot

                                    val confessionCollection = database.collection("confessions")
                                    val newConfessionDocument = confessionCollection.document()
                                    val confessionId = newConfessionDocument.id

                                    val confessionData = hashMapOf(
                                        "id" to confessionId,
                                        "userId" to userUid,
                                        "fromUserId" to fromUserUid,
                                        "text" to confessionText,
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
                                            result.invoke(UiState.Success("Confession added successfully"))
                                        }
                                        .addOnFailureListener { exception ->
                                            result.invoke(UiState.Failure(exception.localizedMessage))
                                        }
                                } else {
                                    result.invoke(UiState.Failure("User not found"))
                                }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure(exception.localizedMessage))
                            }
                    } else {
                        result.invoke(UiState.Failure("User not found"))
                    }
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
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
            var query: Query = confessionCollection

            when (confessionCategory) {
                ConfessionCategory.MY_CONFESSIONS -> {
                    query = query.whereEqualTo("fromUserId", currentUserUid)
                }

                ConfessionCategory.CONFESSIONS_TO_ME -> {
                    query = query.whereEqualTo("userId", currentUserUid)
                }

                ConfessionCategory.OTHER_USER_CONFESSIONS -> {
                    query = query.whereEqualTo("userId", userUid)
                }

                ConfessionCategory.CONFESSIONS_TO_OTHERS -> {
                    query = query.whereEqualTo("fromUserId", userUid)
                }
            }

            query
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
            result.invoke(UiState.Failure("User not authenticated"))
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
                                result.invoke(UiState.Failure("An error occurred while loading confessions"))
                            }
                    } else {
                        result.invoke(UiState.Success(confessionsList))
                    }
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure("An error occurred while fetching followed users"))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
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
                        val fromUserImageUrl = confessionDoc.getString("imageUrl") ?: ""
                        val fromUserEmail = confessionDoc.getString("email") ?: ""
                        val username = confessionDoc.getString("fromUserUsername") ?: ""
                        val imageUrl = confessionDoc.getString("fromUserImageUrl") ?: ""
                        val email = confessionDoc.getString("fromUserEmail") ?: ""

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
                                    }
                                    .addOnFailureListener { exception ->
                                        result.invoke(UiState.Failure(exception.localizedMessage))
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
                                    }
                                    .addOnFailureListener { exception ->
                                        result.invoke(UiState.Failure(exception.localizedMessage))
                                    }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure(exception.localizedMessage))
                            }
                    } else {
                        result.invoke(UiState.Failure("Confession not found"))
                    }
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun favoriteAnswer(
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

                        val answerMap =
                            confessionDocumentSnapshot.get("answer") as MutableMap<String, Any>?
                        if (answerMap != null) {
                            answerMap["favorited"] = isFavorited

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
                                        }
                                        .addOnFailureListener { exception ->
                                            result.invoke(UiState.Failure(exception.localizedMessage))
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    result.invoke(UiState.Failure(exception.localizedMessage))
                                }
                        } else {
                            result.invoke(UiState.Failure("Answer not found"))
                        }
                    } else {
                        result.invoke(UiState.Failure("Confession not found"))
                    }
                }.addOnFailureListener {
                    result.invoke(UiState.Failure("Confession not found"))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
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
                            result.invoke(UiState.Failure("Answer not found"))
                        }
                    } else {
                        result.invoke(UiState.Failure("Confession not found"))
                    }
                }.addOnFailureListener {
                    result.invoke(UiState.Failure("Confession not found"))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
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
                                        "Could not be deleted."
                                    )
                                )
                            }
                    } else {
                        result.invoke(UiState.Failure("Confession not found"))
                    }
                }.addOnFailureListener { exception ->
                    result.invoke(UiState.Failure("Confession not found"))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
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
                    result.invoke(UiState.Success("Bookmark added successfully"))
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
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
                    result.invoke(UiState.Failure("Failed to retrieve bookmarks"))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
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
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }
}