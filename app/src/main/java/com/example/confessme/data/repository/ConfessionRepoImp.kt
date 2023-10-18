package com.example.confessme.data.repository

import android.util.Log
import com.example.confessme.data.model.Answer
import com.example.confessme.data.model.Confession
import com.example.confessme.util.ConfessionCategory
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.getField
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class ConfessionRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore,
    private val storage: FirebaseStorage
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
                        Log.d("Mesaj: " ,"Current user found")
                        val fromUserImageUrl = currentUserDocument.getString("imageUrl")
                        val fromUserUsername = currentUserDocument.getString("userName")
                        val fromUserEmail = currentUserDocument.getString("email")
                        val fromUserUid = currentUserDocument.getString("uid")

                        database.collection("users")
                            .whereEqualTo("uid", userUid)
                            .get()
                            .addOnSuccessListener { documents ->
                                Log.d("Mesaj: " ,"Other user found")
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
        Log.d("Mesaj: ", "Repoda userUid: $userUid")

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

    override fun addFavorite(confessionId: String, result: (UiState<Confession?>) -> Unit) {
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
                        val favorited = confessionDocumentSnapshot.getBoolean("favorited") ?: false

                        val updatedData = mapOf("favorited" to !favorited)

                        val documentRef = database.collection("users")
                            .document(currentUserUid)
                            .collection("confessions_to_me")
                            .document(confessionQuerySnapshot.documents[0].id)

                        batch.update(documentRef, updatedData)

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

                                    val updatedData1 = mapOf("favorited" to !favorited)

                                    batch.update(myConfessionDocRef, updatedData1)

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
                        result.invoke(UiState.Failure("Confession not found"))
                    }
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun favoriteAnswer(confessionId: String, result: (UiState<Confession?>) -> Unit) {
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
                            val currentFavorited =
                                answerMap["favorited"] as Boolean
                            answerMap["favorited"] = !currentFavorited

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
                                                        val currentFavorited =
                                                            answerMap["favorited"] as Boolean
                                                        answerMap["favorited"] = !currentFavorited

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
                                                result.invoke(UiState.Failure("Confession not found 1"))
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

/*
    private fun checkIfUsernameOrBioValid(userName: String, bio: String): String? {
        if (userName.contains(" ")) {
            return "Username cannot contain spaces."
        }
        if(userName.isBlank()) {
            return "Username cannot be blank."
        }
        if (userName.length < 3) {
            return "Username must be at least 3 characters long."
        }
        if (userName.length > 30) {
            return "Username cannot exceed 30 characters."
        }
        if (bio.length > 200) {
            return "Bio cannot exceed 200 characters."
        }
        return null
    }
*/

    /*
        private fun isValidPassword(password: String): Boolean {
            val passwordRegex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$".toRegex()
            if (password.contains(" ")) {
                return false
            }
            return passwordRegex.matches(password)
        }

        private fun checkIfUsernameOrBioValid(userName: String, bio: String): String? {
            if (userName.contains(" ")) {
                return "Username cannot contain spaces."
            }
            if(userName.isBlank()) {
                return "Username cannot be blank."
            }
            if (userName.length < 3) {
                return "Username must be at least 3 characters long."
            }
            if (userName.length > 30) {
                return "Username cannot exceed 30 characters."
            }
            if (bio.length > 200) {
                return "Bio cannot exceed 200 characters."
            }
            return null
        }

        private fun generateRandomUsername(length: Int): String {
            val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }
    */
}