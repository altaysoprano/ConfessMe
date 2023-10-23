package com.example.confessme.data.repository

import android.net.Uri
import com.example.confessme.data.model.User
import com.example.confessme.util.FollowType
import com.example.confessme.util.UiState
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

    override fun fetchUserProfileByUid(userUid: String, result: (UiState<User?>) -> Unit) {
        val userRef = database.collection("users").document(userUid)

        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(User::class.java)
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

    override fun getFollowersOrFollowing(userUid: String, followType: FollowType, result: (UiState<List<User>>) -> Unit) {
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
                .get()
                .addOnSuccessListener { followingDocuments ->
                    val followedUserUids = followingDocuments.documents.map { it.id }

                    val followedUserProfiles = mutableListOf<User>()

                    for (followedUid in followedUserUids) {
                        val userRef = database.collection("users").document(followedUid)

                        userRef.get()
                            .addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {
                                    val userProfile = documentSnapshot.toObject(User::class.java)
                                    if (userProfile != null) {
                                        followedUserProfiles.add(userProfile)
                                    }
                                }
                                if (followedUserUids.size == followedUserProfiles.size) {
                                    result.invoke(UiState.Success(followedUserProfiles))
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

    override fun followUser(userUidToFollow: String, callback: (UiState<String>) -> Unit) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users").document(currentUserUid)
                .collection("following").document(userUidToFollow)
            val followersRef = database.collection("users").document(userUidToFollow)
                .collection("followers").document(currentUserUid)
            val currentUserRef = database.collection("users").document(currentUserUid)
            val otherUserRef = database.collection("users").document(userUidToFollow)

            val batch = database.batch()

            batch.update(otherUserRef, "followersCount", FieldValue.increment(1))
            batch.update(currentUserRef, "followCount", FieldValue.increment(1))

            batch.set(followingRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
            batch.set(followersRef, mapOf("timestamp" to FieldValue.serverTimestamp()))

            batch.commit()
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

    override fun unfollowUser(userUidToUnfollow: String, callback: (UiState<String>) -> Unit) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users").document(currentUserUid)
                .collection("following").document(userUidToUnfollow)
            val followersRef = database.collection("users").document(userUidToUnfollow)
                .collection("followers").document(currentUserUid)
            val currentUserRef = database.collection("users").document(currentUserUid)
            val otherUserRef = database.collection("users").document(userUidToUnfollow)

            val batch = database.batch()

            batch.update(otherUserRef, "followersCount", FieldValue.increment(-1))
            batch.update(currentUserRef, "followCount", FieldValue.increment(-1))

            batch.delete(followingRef)
            batch.delete(followersRef)

            batch.commit()
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
        userUidToCheck: String,
        callback: (UiState<Boolean>) -> Unit
    ) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users").document(currentUserUid)
                .collection("following").document(userUidToCheck)

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