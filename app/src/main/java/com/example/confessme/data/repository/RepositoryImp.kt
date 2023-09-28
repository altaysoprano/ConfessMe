package com.example.confessme.data.repository

import android.net.Uri
import android.util.Log
import com.example.confessme.data.model.Answer
import com.example.confessme.data.model.Confession
import com.example.confessme.data.model.User
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.getField
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class RepositoryImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore,
    private val storage: FirebaseStorage
) : Repository {

    override fun signIn(email: String, pass: String, result: (UiState<String>) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
            if (it.isSuccessful) {
                result.invoke(
                    UiState.Success(it.result.toString())
                )
            } else {
                result.invoke(
                    UiState.Failure("Kullanıcı yok veya şifre hatalı")
                )
            }
        }
    }

    override fun signUp(
        email: String,
        pass: String,
        confirmPass: String,
        result: (UiState<String>) -> Unit
    ) {
        if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
            if (pass == confirmPass) {
                if (!isValidPassword(pass)) {
                    result.invoke(UiState.Failure("Password must contain at least one uppercase letter, one digit, one special character and must be at least 8 characters long."))
                    return
                }
                val randomUsername = generateRandomUsername(10)
                firebaseAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            if (user != null) {
                                val uid = user.uid
                                database.collection("users").document(uid)
                                    .set(User(email, pass, userName = randomUsername))
                                    .addOnSuccessListener { result.invoke(UiState.Success("Successfully signed up")) }
                                    .addOnFailureListener { exception ->
                                        result.invoke(UiState.Failure(exception.localizedMessage))
                                    }
                            }
                        } else {
                            val exception = authTask.exception
                            if (exception is FirebaseAuthUserCollisionException) {
                                result.invoke(UiState.Failure("User already exists"))
                            } else {
                                result.invoke(UiState.Failure("Unknown error: ${exception?.localizedMessage}"))
                            }
                        }
                    }
            } else {
                result.invoke(UiState.Failure("Passwords do not match."))
            }
        } else {
            result.invoke(UiState.Failure("Please fill in all fields."))
        }
    }

    override fun updateProfile(
        previousUserName: String,
        userName: String,
        bio: String,
        imageUri: Uri,
        result: (UiState<String>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val uid = user.uid

            if (userName.isBlank() || userName.length < 3) {
                result.invoke(UiState.Failure("Username must be at least 3 characters long."))
                return
            }

            database.collection("users")
                .whereEqualTo("userName", userName)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty && previousUserName != userName) {
                        result.invoke(UiState.Failure("Username is already taken. Please choose a different one."))
                    } else {
                        val userDocument = database.collection("users").document(uid)

                        val profileUpdate = mutableMapOf<String, Any?>(
                            "userName" to userName,
                            "bio" to bio
                        )
                        if (imageUri != Uri.EMPTY) {
                            val reference =
                                storage.reference.child("Profile").child(Date().time.toString())
                            reference.putFile(imageUri).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    reference.downloadUrl.addOnSuccessListener { imageUrl ->
                                        profileUpdate["imageUrl"] = imageUrl.toString()
                                        userDocument.update(profileUpdate)
                                            .addOnSuccessListener {
                                                result.invoke(UiState.Success("Profile successfully updated"))
                                            }
                                            .addOnFailureListener { exception ->
                                                result.invoke(UiState.Failure(exception.localizedMessage))
                                            }
                                    }
                                } else {
                                    result.invoke(UiState.Failure("An error occurred while updating the profile photo."))
                                }
                            }
                        } else {
                            // Profil fotoğrafı seçilmediyse sadece kullanıcı bilgilerini güncelle
                            userDocument.update(profileUpdate)
                                .addOnSuccessListener {
                                    result.invoke(UiState.Success("Profile successfully updated"))
                                }
                                .addOnFailureListener { exception ->
                                    result.invoke(UiState.Failure(exception.localizedMessage))
                                }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure("User not found"))
        }
    }

    override fun fetchUserProfile(result: (UiState<User?>) -> Unit) {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val uid = user.uid

            database.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userProfile = document.toObject(User::class.java)
                        result.invoke(UiState.Success(userProfile))
                    } else {
                        result.invoke(UiState.Failure("User data not found"))
                    }
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure("User not found"))
        }
    }

    override fun fetchUserProfileByUsername(username: String, result: (UiState<User?>) -> Unit) {
        database.collection("users")
            .whereEqualTo("userName", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val user = document.toObject(User::class.java)
                    result.invoke(UiState.Success(user))
                } else {
                    result.invoke(UiState.Failure("User data not found"))
                }
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage))
            }
    }


    override fun searchUsers(query: String, result: (UiState<List<User>>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            database.collection("users")
                .orderBy("userName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .addOnSuccessListener { documents ->
                    val userList = mutableListOf<User>()

                    for (document in documents) {
                        val uid = document.id

                        if (uid != currentUserUid) {
                            val user = document.toObject(User::class.java)
                            userList.add(user)
                        }
                    }

                    result.invoke(UiState.Success(userList))
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure("User not found"))
        }
    }

    override fun followUser(userIdToFollow: String, callback: (UiState<String>) -> Unit) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users").document(currentUserUid)
                .collection("following").document(userIdToFollow)

            followingRef.set(mapOf("timestamp" to FieldValue.serverTimestamp()))
                .addOnSuccessListener {
                    callback.invoke(UiState.Success("User followed"))
                }
                .addOnFailureListener { exception ->
                    callback.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            callback.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun unfollowUser(userIdToUnfollow: String, callback: (UiState<String>) -> Unit) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users").document(currentUserUid)
                .collection("following").document(userIdToUnfollow)

            followingRef.delete()
                .addOnSuccessListener {
                    callback.invoke(UiState.Success("User unfollowed"))
                }
                .addOnFailureListener { exception ->
                    callback.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            callback.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun checkIfUserFollowed(
        usernameToCheck: String,
        callback: (UiState<Boolean>) -> Unit
    ) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users").document(currentUserUid)
                .collection("following").document(usernameToCheck)

            followingRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    val isFollowed = documentSnapshot.exists()
                    callback.invoke(UiState.Success(isFollowed))
                }
                .addOnFailureListener { exception ->
                    callback.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            callback.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun addConfession(
        userName: String,
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

                        database.collection("users")
                            .whereEqualTo("userName", userName)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val userDocument = documents.documents[0]
                                    val userId = userDocument.id // Bu kullanıcının UID'sini verir
                                    val imageUrl =
                                        userDocument.getString("imageUrl") // Kullanıcının imageUrl'sini alın

                                    val confessionCollection =
                                        database.collection("users").document(currentUserUid)
                                            .collection("my_confessions")
                                    val newConfessionDocument = confessionCollection.document()

                                    val confessionData = hashMapOf(
                                        "id" to newConfessionDocument.id,
                                        "text" to confessionText,
                                        "username" to userName,
                                        "imageUrl" to imageUrl,
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
                                    result.invoke(UiState.Failure("User with username not found"))
                                }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure(exception.localizedMessage))
                            }
                    } else {
                        result.invoke(UiState.Failure("Current user not found in Firestore"))
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
        limit: Long,
        isMyConfessions: Boolean,
        result: (UiState<List<Confession>>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid
            val confessionCollection = if (isMyConfessions) {
                database.collection("users").document(currentUserUid)
                    .collection("my_confessions")
            } else {
                database.collection("users").document(currentUserUid)
                    .collection("confessions_to_me")
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
                        Log.d("Mesaj: ", "isAnswered: ${confession.answered}")
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
        result: (UiState<String>) -> Unit
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
                        val username = confessionDoc.getString("fromUserUsername") ?: ""
                        val imageUrl = confessionDoc.getString("fromUserImageUrl") ?: ""

                        val answerData = Answer(
                            text = answerText,
                            username = username,
                            fromUserUsername = fromUserUsername,
                            fromUserImageUrl = fromUserImageUrl,
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
                        val userQuery = usersCollection.whereEqualTo("userName", username)
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
                                            result.invoke(UiState.Success("Answered successfully"))
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
                .whereEqualTo("id", confessionId) // İlgili confessionId'ye sahip itirağı al

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

                        val username =
                            confessionDocumentSnapshot.getString("fromUserUsername") ?: ""
                        val userQuery =
                            database.collection("users").whereEqualTo("userName", username)

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

    private fun isValidPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$".toRegex()
        return passwordRegex.matches(password)
    }

    private fun generateRandomUsername(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}