package com.example.confessme.data.repository

import android.util.Log
import com.example.confessme.data.model.Answer
import com.example.confessme.data.model.Confession
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.getField
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

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

                        database.collection("users")
                            .whereEqualTo("uid", userUid)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val userDocument = documents.documents[0]
                                    val userId = userDocument.id
                                    val imageUrl =
                                        userDocument.getString("imageUrl")
                                    val userName =
                                        userDocument.getString("userName")
                                    val userEmail =
                                        userDocument.getString("email")

                                    val confessionCollection =
                                        database.collection("users").document(currentUserUid)
                                            .collection("my_confessions")
                                    val newConfessionDocument = confessionCollection.document()

                                    val confessionData = hashMapOf(
                                        "id" to newConfessionDocument.id,
                                        "userId" to userUid,
                                        "fromUserId" to fromUserUid,
                                        "text" to confessionText,
                                        "username" to userName,
                                        "email" to userEmail,
                                        "imageUrl" to imageUrl,
                                        "fromUserEmail" to fromUserEmail,
                                        "fromUserUsername" to fromUserUsername,
                                        "fromUserImageUrl" to fromUserImageUrl,
                                        "timestamp" to FieldValue.serverTimestamp()
                                    )

                                    val batch = database.batch()

                                    batch.set(newConfessionDocument, confessionData)

                                    val confessionToMeCollection =
                                        database.collection("users").document(userId)
                                            .collection("confessions_to_me")
                                    val newConfessionToMeDocument =
                                        confessionToMeCollection.document()

                                    batch.set(newConfessionToMeDocument, confessionData)

                                    batch.commit()
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

            val confessionCollection = when (confessionCategory) {
                ConfessionCategory.MY_CONFESSIONS -> {
                    database.collection("users").document(currentUserUid)
                        .collection("my_confessions")
                }

                ConfessionCategory.CONFESSIONS_TO_ME -> {
                    database.collection("users").document(currentUserUid)
                        .collection("confessions_to_me")
                }

                ConfessionCategory.OTHER_USER_CONFESSIONS -> {
                    database.collection("users").document(userUid)
                        .collection("my_confessions")
                }

                ConfessionCategory.CONFESSIONS_TO_OTHERS -> {
                    database.collection("users").document(userUid)
                        .collection("confessions_to_me")
                }
            }

            confessionCollection
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

            val followingCollection = database.collection("users").document(currentUserUid)
                .collection("following")

            followingCollection.get()
                .addOnSuccessListener { followingDocuments ->
                    val followingUserUids = followingDocuments.documents.map { it.id }

                    val tasks = followingUserUids.map { followingUid ->
                        val myConfessionsTask = database.collection("users").document(followingUid)
                            .collection("my_confessions")
                            .limit(limit)
                            .get()

                        val confessionsToMeTask = database.collection("users").document(followingUid)
                            .collection("confessions_to_me")
                            .limit(limit)
                            .get()

                        Tasks.whenAllSuccess<QuerySnapshot>(myConfessionsTask, confessionsToMeTask)
                            .continueWith { task ->
                                val myConfessions =
                                    (task.result?.get(0) as QuerySnapshot).toObjects(Confession::class.java)
                                val confessionsToMe =
                                    (task.result?.get(1) as QuerySnapshot).toObjects(Confession::class.java)

                                myConfessions + confessionsToMe
                            }
                    }

                    Tasks.whenAllSuccess<List<Confession>>(*tasks.toTypedArray())
                        .addOnSuccessListener { combinedResults ->
                            val combinedConfessions = combinedResults.flatten()
                            val uniqueConfessions = combinedConfessions.distinctBy { it.id }
                            val sortedConfessions = uniqueConfessions.sortedByDescending { it.timestamp.toString() }
                            sortedConfessions.map { Log.d("Mesaj: ", "${it.id}") }
                            result.invoke(UiState.Success(sortedConfessions))
                        }
                        .addOnFailureListener { exception ->
                            result.invoke(UiState.Failure("An error occurred while loading confessions"))
                        }
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure("An error occurred while loading confessions"))
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
            val currentUserUid = user.uid

            val confessionDocRef = database.collection("users")
                .document(currentUserUid)
                .collection("confessions_to_me")
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

                        val usersCollection = database.collection("users")
                        val userQuery = usersCollection.whereEqualTo("email", email)
                        userQuery.get()
                            .addOnSuccessListener { userQuerySnapshot ->
                                if (!userQuerySnapshot.isEmpty) {
                                    val userDoc = userQuerySnapshot.documents[0]
                                    val userUid = userDoc.id

                                    val myConfessionsCollection = usersCollection.document(userUid)
                                        .collection("my_confessions")

                                    val confessionDocRef1 =
                                        myConfessionsCollection.document(confessionId)

                                    val updatedData1 = mapOf("answered" to true)
                                    batch.update(confessionDocRef1, updatedData1)

                                    val answerField1 = mapOf("answer" to answerData)
                                    batch.set(confessionDocRef1, answerField1, SetOptions.merge())

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
                                    result.invoke(UiState.Failure("User could not be found"))
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
            val currentUserUid = user.uid

            val batch = database.batch()

            val confessionDocRef = database.collection("users")
                .document(currentUserUid)
                .collection("confessions_to_me")
                .whereEqualTo("id", confessionId)

            confessionDocRef.get()
                .addOnSuccessListener { confessionQuerySnapshot ->
                    if (!confessionQuerySnapshot.isEmpty) {
                        val confessionDocumentSnapshot = confessionQuerySnapshot.documents[0]

                        val updatedData = mapOf("favorited" to favorited)

                        val documentRef = database.collection("users")
                            .document(currentUserUid)
                            .collection("confessions_to_me")
                            .document(confessionQuerySnapshot.documents[0].id)

                        batch.update(documentRef, updatedData)

                        val userUid =
                            confessionDocumentSnapshot.getString("fromUserId") ?: ""
                        val myConfessionDocumentRef =
                            database.collection("users").document(userUid)
                                .collection("my_confessions")
                                .document(confessionId)

                        val updatedData1 = mapOf("favorited" to favorited)

                        batch.update(myConfessionDocumentRef, updatedData1)

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
            val currentUserUid = user.uid

            val batch = database.batch()

            val confessionDocRef = database.collection("users")
                .document(currentUserUid)
                .collection("my_confessions")
                .whereEqualTo("id", confessionId)

            confessionDocRef.get()
                .addOnSuccessListener { confessionQuerySnapshot ->
                    if (!confessionQuerySnapshot.isEmpty) {
                        val confessionDocumentSnapshot = confessionQuerySnapshot.documents[0]

                        val answerMap =
                            confessionDocumentSnapshot.get("answer") as MutableMap<String, Any>?
                        if (answerMap != null) {

                            answerMap["favorited"] = isFavorited

                            batch.update(
                                confessionDocumentSnapshot.reference,
                                mapOf("answer" to answerMap)
                            )

                            val email =
                                confessionDocumentSnapshot.getString("email") ?: ""
                            val userQuery =
                                database.collection("users").whereEqualTo("email", email)

                            userQuery.get()
                                .addOnSuccessListener { userQuerySnapshot ->
                                    if (!userQuerySnapshot.isEmpty) {
                                        val userDoc = userQuerySnapshot.documents[0]
                                        val userUid = userDoc.id

                                        val myConfessionDoc = database.collection("users")
                                            .document(userUid)
                                            .collection("confessions_to_me")
                                            .whereEqualTo("id", confessionId)

                                        myConfessionDoc.get()
                                            .addOnSuccessListener { myConfessionQuerySnapshot ->
                                                if (!myConfessionQuerySnapshot.isEmpty) {
                                                    val myConfessionDocumentSnapshot =
                                                        myConfessionQuerySnapshot.documents[0]
                                                    val answerMap =
                                                        myConfessionDocumentSnapshot.get("answer") as MutableMap<String, Any>?
                                                    if (answerMap != null) {

                                                        answerMap["favorited"] = isFavorited

                                                        batch.update(
                                                            myConfessionDocumentSnapshot.reference,
                                                            mapOf("answer" to answerMap)
                                                        )

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
                                                }
                                            }
                                    } else {
                                        result.invoke(UiState.Failure("User could not be found"))
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    result.invoke(UiState.Failure(exception.localizedMessage))
                                }
                        } else {
                            result.invoke(UiState.Failure("Confession not found"))
                        }
                    }
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun deleteAnswer(confessionId: String, result: (UiState<Confession?>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            val batch = database.batch()

            val confessionDocRef = database.collection("users")
                .document(currentUserUid)
                .collection("confessions_to_me")
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

                            val email =
                                confessionDocumentSnapshot.getString("fromUserEmail") ?: ""
                            val userQuery =
                                database.collection("users").whereEqualTo("email", email)

                            userQuery.get()
                                .addOnSuccessListener { userQuerySnapshot ->
                                    if (!userQuerySnapshot.isEmpty) {
                                        val userDoc = userQuerySnapshot.documents[0]
                                        val userUid = userDoc.id

                                        val myConfessionsCollection = database.collection("users")
                                            .document(userUid)
                                            .collection("my_confessions")
                                        val myConfessionDocRef =
                                            myConfessionsCollection.document(confessionId)

                                        myConfessionDocRef.get()
                                            .addOnSuccessListener { myConfessionDocumentSnapshot ->
                                                if (myConfessionDocumentSnapshot.exists()) {
                                                    val answerMap1 =
                                                        myConfessionDocumentSnapshot.get("answer") as MutableMap<String, Any>?

                                                    if (answerMap1 != null) {
                                                        val answerFieldUpdate1 =
                                                            mapOf("answer" to FieldValue.delete())

                                                        val answeredFieldUpdate1 =
                                                            mapOf("answered" to false)

                                                        batch.update(
                                                            myConfessionDocRef,
                                                            answerFieldUpdate1
                                                        )
                                                        batch.update(
                                                            myConfessionDocRef,
                                                            answeredFieldUpdate1
                                                        )
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
                                                    result.invoke(UiState.Failure("Confession could not be found"))
                                                }
                                            }
                                    } else {
                                        result.invoke(UiState.Failure("User could not be found"))
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    result.invoke(UiState.Failure(exception.localizedMessage))
                                }
                        } else {
                            result.invoke(UiState.Failure("Confession not found"))
                        }
                    }
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun deleteConfession(confessionId: String, result: (UiState<Confession?>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            val batch = database.batch()

            val confessionDocRef = database.collection("users")
                .document(currentUserUid)
                .collection("my_confessions")
                .whereEqualTo("id", confessionId)

            confessionDocRef.get()
                .addOnSuccessListener { confessionQuerySnapshot ->
                    if (!confessionQuerySnapshot.isEmpty) {
                        val confessionDocumentSnapshot = confessionQuerySnapshot.documents[0]

                        val confessionRef = confessionDocumentSnapshot.reference
                        batch.delete(confessionRef)

                        val email =
                            confessionDocumentSnapshot.getString("email") ?: ""
                        val userQuery =
                            database.collection("users").whereEqualTo("email", email)

                        userQuery.get()
                            .addOnSuccessListener { userQuerySnapshot ->
                                if (!userQuerySnapshot.isEmpty) {
                                    val userDoc = userQuerySnapshot.documents[0]
                                    val userUid = userDoc.id

                                    val myConfessionDoc = database.collection("users")
                                        .document(userUid)
                                        .collection("confessions_to_me")
                                        .whereEqualTo("id", confessionId)

                                    myConfessionDoc.get()
                                        .addOnSuccessListener { myConfessionQuerySnapshot ->
                                            if (!myConfessionQuerySnapshot.isEmpty) {
                                                val myConfessionDocumentSnapshot =
                                                    myConfessionQuerySnapshot.documents[0]

                                                val deletedConfession =
                                                    myConfessionDocumentSnapshot.toObject(Confession::class.java)

                                                batch.delete(myConfessionDocumentSnapshot.reference)

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
                                                result.invoke(UiState.Failure("Confession not found"))
                                            }
                                        }
                                } else {
                                    result.invoke(UiState.Failure("User could not be found"))
                                }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure(exception.localizedMessage))
                            }
                    } else {
                        result.invoke(UiState.Failure("Confession not found"))
                    }
                }.addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
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

                    val timestampMap = mutableMapOf<String, Timestamp>() // Map to store timestamps

                    var fetchedConfessionCount = 0

                    for (document in documents) {
                        val confessionId = document.id
                        val userUid = document.getString("userUid") ?: ""

                        val myConfessionsCollection = database.collection("users").document(userUid)
                            .collection("my_confessions")
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
                    result.invoke(UiState.Failure(exception.localizedMessage))
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