package com.example.confessme.data.repository

import android.net.Uri
import android.util.Log
import com.example.confessme.data.model.FollowUser
import com.example.confessme.data.model.User
import com.example.confessme.util.FollowType
import com.example.confessme.util.UiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class UserRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepo {
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

            val validationError = checkIfUsernameOrBioValid(userName, bio)
            if (validationError != null) {
                result.invoke(UiState.Failure(validationError))
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
            val userRef = database.collection("users").document(uid)

            userRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val userProfile = documentSnapshot.toObject(User::class.java)

                        userRef.collection("followers").get()
                            .addOnSuccessListener { followersQuerySnapshot ->
                                val followersCount = followersQuerySnapshot.size()

                                userRef.collection("following").get()
                                    .addOnSuccessListener { followingQuerySnapshot ->
                                        val followingCount = followingQuerySnapshot.size()

                                        userProfile?.followCount = followingCount
                                        userProfile?.followersCount = followersCount

                                        result.invoke(UiState.Success(userProfile))
                                    }
                                    .addOnFailureListener { exception ->
                                        result.invoke(UiState.Failure("An error occurred while pulling the following count"))
                                    }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure("An error occurred while pulling the follower count"))
                            }
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

    override fun fetchUserProfileByUid(userUid: String, result: (UiState<User?>) -> Unit) {
        val userRef = database.collection("users").document(userUid)

        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(User::class.java)

                    userRef.collection("followers").get()
                        .addOnSuccessListener { followersQuerySnapshot ->
                            val followersCount = followersQuerySnapshot.size()

                            userRef.collection("following").get()
                                .addOnSuccessListener { followingQuerySnapshot ->
                                    val followingCount = followingQuerySnapshot.size()

                                    user?.followCount = followingCount
                                    user?.followersCount = followersCount

                                    result.invoke(UiState.Success(user))
                                }
                                .addOnFailureListener { exception ->
                                    result.invoke(UiState.Failure("An error occurred while pulling the following count"))
                                }
                        }
                        .addOnFailureListener { exception ->
                            result.invoke(UiState.Failure("An error occurred while pulling the follower count"))
                        }
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

                    val usersProcessed = mutableListOf<Int>() // İşlenmiş kullanıcılar listesi

                    if (documents.isEmpty()) {
                        // Hiç sonuç yok, direk success olarak işaretleyebiliriz
                        result.invoke(UiState.Success(userList))
                        return@addOnSuccessListener
                    }

                    for (document in documents) {
                        val uid = document.id

                        if (uid != currentUserUid) {
                            val user = document.toObject(User::class.java)
                            userList.add(user)

                            // İşlenen her kullanıcı için takip durumunu kontrol et
                            val myFollowingsRef = database.collection("users")
                                .document(currentUserUid)
                                .collection("following")
                                .document(uid)
                            myFollowingsRef.get().addOnSuccessListener { documentSnapshot ->
                                user.isFollowing = documentSnapshot.exists()
                                Log.d(
                                    "Mesaj: ",
                                    "${user.userName} isFollowing: ${user.isFollowing}"
                                )

                                // Tüm kullanıcılar işlenip işlenmediğini kontrol et
                                usersProcessed.add(userList.indexOf(user))
                                if (usersProcessed.size == documents.size() - 1
                                    || usersProcessed.size == documents.size()
                                    || documents.size() == 0
                                ) {
                                    result.invoke(UiState.Success(userList))
                                }
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

    override fun getFollowersOrFollowing(
        userUid: String,
        limit: Long,
        followType: FollowType,
        result: (UiState<List<User>>) -> Unit
    ) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = when (followType) {
                FollowType.MyFollowings -> database.collection("users").document(currentUserUid)
                    .collection("following")

                FollowType.MyFollowers -> database.collection("users").document(currentUserUid)
                    .collection("followers")

                FollowType.OtherUserFollowings -> database.collection("users").document(userUid)
                    .collection("following")

                FollowType.OtherUserFollowers -> database.collection("users").document(userUid)
                    .collection("followers")
            }

            followingRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener { followingDocuments ->

                    val followedUserProfiles = mutableListOf<User>()
                    var counter = 0

                    for (followingDocument in followingDocuments) {
                        val userRef = database.collection("users").document(followingDocument.id)

                        userRef.get()
                            .addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {
                                    val userProfile = documentSnapshot.toObject(User::class.java)
                                    if (userProfile != null) {
                                        followedUserProfiles.add(userProfile)
                                        userProfile.timestampFollow =
                                            followingDocument.getTimestamp("timestamp") as Timestamp
                                    }
                                } else {
                                    val followedUid = followingDocument.id
                                    followingRef.document(followedUid).delete()
                                }

                                counter++

                                if (counter == followingDocuments.size()) {
                                    val sortedFollowUserProfiles =
                                        followedUserProfiles.sortedByDescending { it.timestampFollow.toString() }
                                    sortedFollowUserProfiles.forEach { user ->
                                        val myFollowingsRef = database.collection("users")
                                            .document(currentUserUid)

                                            .collection("following")
                                            .document(user.uid)
                                        myFollowingsRef.get()
                                            .addOnSuccessListener { documentSnapshot ->
                                                user.isFollowing = documentSnapshot.exists()

                                                if (sortedFollowUserProfiles.indexOf(user) == sortedFollowUserProfiles.size - 1) {
                                                    result.invoke(
                                                        UiState.Success(
                                                            sortedFollowUserProfiles
                                                        )
                                                    )
                                                }
                                            }
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure(exception.localizedMessage))
                            }
                    }

                    if (followingDocuments.isEmpty) {
                        result.invoke(UiState.Success(followedUserProfiles))
                    }
                }
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun followUser(
        userUidToFollow: String,
        callback: (UiState<FollowUser>) -> Unit
    ) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users").document(currentUserUid)
                .collection("following").document(userUidToFollow)
            val followersRef = database.collection("users").document(userUidToFollow)
                .collection("followers").document(currentUserUid)

            val batch = database.batch()

            batch.set(followingRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
            batch.set(followersRef, mapOf("timestamp" to FieldValue.serverTimestamp()))

            batch.commit()
                .addOnSuccessListener {
                    val followUser =
                        FollowUser(userUid = followingRef.id, isFollowed = true)
                    callback.invoke(UiState.Success(followUser))
                }
                .addOnFailureListener { exception ->
                    callback.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            callback.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun unfollowUser(
        userUidToUnfollow: String,
        callback: (UiState<FollowUser>) -> Unit
    ) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users").document(currentUserUid)
                .collection("following").document(userUidToUnfollow)
            val followersRef = database.collection("users").document(userUidToUnfollow)
                .collection("followers").document(currentUserUid)

            val batch = database.batch()

            batch.delete(followingRef)
            batch.delete(followersRef)

            batch.commit()
                .addOnSuccessListener {
                    val followUser =
                        FollowUser(isFollowed = false, userUid = followingRef.id)
                    callback.invoke(UiState.Success(followUser))
                }
                .addOnFailureListener { exception ->
                    callback.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            callback.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun checkIfUserFollowed(
        userUidToCheck: String,
        callback: (UiState<FollowUser>) -> Unit
    ) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users").document(currentUserUid)
                .collection("following").document(userUidToCheck)

            followingRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    val isFollowed = documentSnapshot.exists()
                    val followUser = FollowUser(
                        isFollowed = isFollowed,
                        userUid = documentSnapshot.id
                    )
                    callback.invoke(UiState.Success(followUser))
                }
                .addOnFailureListener { exception ->
                    callback.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            callback.invoke(UiState.Failure("User not authenticated"))
        }
    }

    private fun checkIfUsernameOrBioValid(userName: String, bio: String): String? {
        if (userName.contains(" ")) {
            return "Username cannot contain spaces."
        }
        if (userName.isBlank()) {
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
}