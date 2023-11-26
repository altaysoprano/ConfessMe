package com.example.confessme.data.repository

import android.net.Uri
import android.util.Log
import com.example.confessme.data.model.FollowUser
import com.example.confessme.data.model.Notification
import com.example.confessme.data.model.User
import com.example.confessme.util.Constants
import com.example.confessme.util.FollowType
import com.example.confessme.util.ProfilePhotoAction
import com.example.confessme.util.UiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
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
import java.util.Date

class UserRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepo {
    override fun updateProfile(
        previousUserName: String,
        previousImageUrl: String,
        userName: String,
        bio: String,
        imageUri: Uri,
        profilePhotoAction: ProfilePhotoAction,
        result: (UiState<String>) -> Unit
    ) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val uid = user.uid

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
                        when (profilePhotoAction) {
                            ProfilePhotoAction.CHANGE -> {
                                val reference =
                                    storage.reference.child("Profile")
                                        .child(Date().time.toString())
                                reference.putFile(imageUri).addOnCompleteListener { uploadTask ->
                                    if (uploadTask.isSuccessful) {
                                        reference.downloadUrl.addOnSuccessListener { imageUrl ->
                                            val newImageUrl = imageUrl.toString()
                                            profileUpdate["imageUrl"] = newImageUrl

                                            // eğer previousImageUrl boş değilse önceki profil resmini siliyoruz
                                            if (previousImageUrl.isNotEmpty()) {
                                                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(previousImageUrl)
                                                storageRef.delete().addOnSuccessListener {
                                                    // userdaki resmi güncelliyoruz
                                                    userDocument.update(profileUpdate)
                                                        .addOnSuccessListener {
                                                            // confessionları güncelliyoruz
                                                            updateConfessionsUsernamesAndImageUrls(previousUserName, previousImageUrl, userName, newImageUrl) { success ->
                                                                if (success) {
                                                                    result.invoke(UiState.Success("Profile successfully updated"))
                                                                } else {
                                                                    result.invoke(UiState.Failure("An error occurred while updating profile"))
                                                                }
                                                            }
                                                        }
                                                        .addOnFailureListener { exception ->
                                                            result.invoke(UiState.Failure(exception.localizedMessage))
                                                        }
                                                }.addOnFailureListener { exception ->
                                                    result.invoke(UiState.Failure(exception.localizedMessage))
                                                }
                                            } else {
                                                // eğer previousImageUrl boşsa userın pp'yi güncelliyoruz
                                                userDocument.update(profileUpdate)
                                                    .addOnSuccessListener {
                                                        // confessionları güncelliyoruz
                                                        updateConfessionsUsernamesAndImageUrls(previousUserName, previousImageUrl, userName, newImageUrl) { success ->
                                                            if (success) {
                                                                result.invoke(UiState.Success("Profile successfully updated"))
                                                            } else {
                                                                result.invoke(UiState.Failure("An error occurred while updating profile"))
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        result.invoke(UiState.Failure(exception.localizedMessage))
                                                    }
                                            }
                                        }
                                    } else {
                                        result.invoke(UiState.Failure("An error occurred while updating the profile photo."))
                                    }
                                }
                            }

                            ProfilePhotoAction.REMOVE -> {
                                profileUpdate["imageUrl"] = ""

                                // users güncelleme
                                userDocument.update(profileUpdate)
                                    .addOnSuccessListener {
                                        // Eğer previousImageUrl boş değilse storage'ten fotoğrafı siliyoruz
                                        if (previousImageUrl.isNotEmpty()) {
                                            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(previousImageUrl)
                                            storageRef.delete().addOnSuccessListener {
                                                // confessionsları güncelleme
                                                updateConfessionsUsernamesAndImageUrls(previousUserName, previousImageUrl, userName, "") { success ->
                                                    if (success) {
                                                        result.invoke(UiState.Success("Profile successfully updated"))
                                                    } else {
                                                        result.invoke(UiState.Failure("An error occurred while updating profile"))
                                                    }
                                                }
                                            }.addOnFailureListener { exception ->
                                                result.invoke(UiState.Failure(exception.localizedMessage))
                                            }
                                        } else {
                                            // eğer previousImageUrl boşsa doğrudan confessionsları güncelleme işlemine geçiyoruz
                                            updateConfessionsUsernamesAndImageUrls(previousUserName, previousImageUrl, userName, "") { success ->
                                                if (success) {
                                                    result.invoke(UiState.Success("Profile successfully updated"))
                                                } else {
                                                    result.invoke(UiState.Failure("An error occurred while updating profile"))
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        result.invoke(UiState.Failure(exception.localizedMessage))
                                    }
                            }

                            ProfilePhotoAction.DO_NOT_CHANGE -> {
                                userDocument.update(profileUpdate)
                                    .addOnSuccessListener {
                                        updateConfessionsUsernames(previousUserName, userName) { success ->
                                            if (success) {
                                                result.invoke(UiState.Success("Profile successfully updated"))
                                            } else {
                                                result.invoke(UiState.Failure("An error occurred while updating profile"))
                                            }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        result.invoke(UiState.Failure(exception.localizedMessage))
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

    fun updateConfessionsUsernamesAndImageUrls(previousUsername: String, previousImageUrl: String, newUsername: String, newImageUrl: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val confessionsRef = db.collection("confessions")

        confessionsRef.whereEqualTo("fromUserUsername", previousUsername)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                documents.forEach { document ->
                    val docRef = confessionsRef.document(document.id)
                    val answerMap = document.get("answer") as? HashMap<String, Any> ?: hashMapOf()
                    val newAnswerMap = answerMap.mapValues { (key, value) ->
                        when (key) {
                            "username", "fromUserUsername" -> if (value == previousUsername) newUsername else value
                            "imageUrl" -> {
                                val newValue = if (value == previousImageUrl || value == "") newImageUrl else value
                                newValue
                            }
                            else -> value
                        }
                    }
                    batch.update(docRef, "answer", newAnswerMap)
                    batch.update(docRef, "fromUserUsername", newUsername)
                    batch.update(docRef, "fromUserImageUrl", newImageUrl)
                }

                confessionsRef.whereEqualTo("username", previousUsername)
                    .get()
                    .addOnSuccessListener { documents ->
                        documents.forEach { document ->
                            val docRef = confessionsRef.document(document.id)
                            val answerMap = document.get("answer") as? HashMap<String, Any> ?: hashMapOf()
                            val newAnswerMap = answerMap.mapValues { (key, value) ->
                                when (key) {
                                    "username", "fromUserUsername" -> if (value == previousUsername) newUsername else value
                                    "fromUserImageUrl" -> {
                                        val newValue = if (value == previousImageUrl || value == "") newImageUrl else value
                                        newValue
                                    }
                                    else -> value
                                }
                            }
                            batch.update(docRef, "answer", newAnswerMap)
                            batch.update(docRef, "username", newUsername)
                            batch.update(docRef, "imageUrl", newImageUrl)
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                callback.invoke(true)
                            }
                            .addOnFailureListener { exception ->
                                callback.invoke(false)
                            }
                    }
                    .addOnFailureListener { exception ->
                        callback.invoke(false)
                    }
            }
            .addOnFailureListener { exception ->
                callback.invoke(false)
            }
    }

    fun updateConfessionsUsernames(previousUsername: String, newUsername: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val confessionsRef = db.collection("confessions")

        confessionsRef.whereEqualTo("fromUserUsername", previousUsername)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                documents.forEach { document ->
                    val docRef = confessionsRef.document(document.id)
                    val answerMap = document.get("answer") as? HashMap<String, Any> ?: hashMapOf()
                    val newAnswerMap = answerMap.mapValues { (key, value) ->
                        if (key == "username" && value == previousUsername) {
                            newUsername
                        } else if (key == "fromUserUsername" && value == previousUsername) {
                            newUsername
                        } else {
                            value
                        }
                    }
                    batch.update(docRef, "answer", newAnswerMap)
                    batch.update(docRef, "fromUserUsername", newUsername)
                }

                confessionsRef.whereEqualTo("username", previousUsername)
                    .get()
                    .addOnSuccessListener { documents ->
                        documents.forEach { document ->
                            val docRef = confessionsRef.document(document.id)
                            val answerMap = document.get("answer") as? HashMap<String, Any> ?: hashMapOf()
                            val newAnswerMap = answerMap.mapValues { (key, value) ->
                                if (key == "username" && value == previousUsername) {
                                    newUsername
                                } else if (key == "fromUserUsername" && value == previousUsername) {
                                    newUsername
                                } else {
                                    value
                                }
                            }
                            batch.update(docRef, "answer", newAnswerMap)
                            batch.update(docRef, "username", newUsername)
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                callback.invoke(true)
                            }
                            .addOnFailureListener { exception ->
                                callback.invoke(false)
                            }
                    }
                    .addOnFailureListener { exception ->
                        callback.invoke(false)
                    }
            }
            .addOnFailureListener { exception ->
                callback.invoke(false)
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
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun fetchUserProfileByUid(userUid: String, result: (UiState<User?>) -> Unit) {
        val userRef = database.collection("users").document(userUid)
        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {
            val currentUserUid = currentUser.uid
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

                                        val isFollower =
                                            followingQuerySnapshot.documents.any { it.id == currentUserUid }
                                        user?.isFollower = isFollower

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
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override fun searchUsers(query: String, result: (UiState<List<User>>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val currentUserUid = user.uid

            database.collection("users")
                .orderBy("userName")
                .limit(10)
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .addOnSuccessListener { documents ->
                    val userList = mutableListOf<User>()

                    val usersProcessed = mutableListOf<Int>()

                    if (documents.isEmpty()) {
                        result.invoke(UiState.Success(userList))
                        return@addOnSuccessListener
                    }

                    for (document in documents) {
                        val uid = document.id
                        val user = document.toObject(User::class.java)

                        if (uid != currentUserUid) {
                            userList.add(user)
                        }

                        val myFollowingsRef = database.collection("users")
                            .document(currentUserUid)
                            .collection("following")
                            .document(uid)
                        myFollowingsRef.get().addOnSuccessListener { documentSnapshot ->
                            user.isFollowing = documentSnapshot.exists()

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
                .addOnFailureListener { exception ->
                    result.invoke(UiState.Failure(exception.localizedMessage))
                }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
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
        userName: String,
        userToken: String,
        callback: (UiState<FollowUser>) -> Unit
    ) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users").document(currentUserUid)
                .collection("following").document(userUidToFollow)
            val followersRef = database.collection("users").document(userUidToFollow)
                .collection("followers").document(currentUserUid)
            val fcmToken = userToken

            val batch = database.batch()

            batch.set(followingRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
            batch.set(followersRef, mapOf("timestamp" to FieldValue.serverTimestamp()))

            batch.commit()
                .addOnSuccessListener {
                    val followUser =
                        FollowUser(userUid = followingRef.id, isFollowed = true)
                    callback.invoke(UiState.Success(followUser))
                    database.collection("users").document(currentUserUid).get()
                        .addOnSuccessListener {
                            val username = it.getString("userName") ?: ""
                            val fromUserImageUrl = it.getString("imageUrl") ?: ""
                            val fromUserToken = it.getString("token") ?: ""

                            if (fcmToken != "") {
                                sendNotification(
                                    "$username followed you",
                                    "",
                                    userUidToFollow,
                                    fcmToken
                                )
                            }
                            addNotificationToUser(userUidToFollow, currentUserUid, fromUserToken,"", username, fromUserImageUrl, "", "followed you")
                        }
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

    override fun addSearchToHistory(userUid: String) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val searchHistoryRef = database.collection("users").document(currentUserUid)
                .collection("searchHistory")

            searchHistoryRef.document(userUid)
                .set(mapOf("timestamp" to FieldValue.serverTimestamp()))
        }
    }

    override suspend fun getSearchHistoryUsers(limit: Long, result: (UiState<List<User>>) -> Unit) {

        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val followingRef = database.collection("users")
                .document(currentUserUid)
                .collection("following")

            val followingQuerySnapshot = followingRef.get().await()
            val followingUserIds = followingQuerySnapshot.documents.map { it.id }

            val searchHistoryRef = database.collection("users")
                .document(currentUserUid)
                .collection("searchHistory")

            val querySnapshot = searchHistoryRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val searchHistoryUsers = mutableListOf<User>()

            for (document in querySnapshot.documents) {
                val userUid = document.id
                val userDocument = database.collection("users")
                    .document(userUid)
                    .get()
                    .await()

                if (userDocument.exists()) {
                    val user = userDocument.toObject(User::class.java)
                    user?.let {
                        searchHistoryUsers.add(it)

                        it.isFollowing = followingUserIds.contains(userUid)
                    }
                }
            }

            result(UiState.Success(searchHistoryUsers))
        } else {
            result(UiState.Success(emptyList()))
        }
    }

    override suspend fun deleteSearchHistoryCollection(result: (UiState<Boolean>) -> Unit) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val searchHistoryRef = database.collection("users")
                .document(currentUserUid)
                .collection("searchHistory")

            try {
                val querySnapshot = searchHistoryRef.get().await()
                for (document in querySnapshot.documents) {
                    searchHistoryRef.document(document.id).delete().await()
                }

                result(UiState.Success(true))
            } catch (e: Exception) {
                result.invoke(UiState.Failure("Deletion failed"))
            }
        } else {
            result.invoke(UiState.Failure("User not authenticated"))
        }
    }

    override suspend fun deleteSearchHistoryDocument(
        documentIdToDelete: String,
        result: (UiState<String>) -> Unit
    ) {
        val currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid != null) {
            val searchHistoryRef = database.collection("users")
                .document(currentUserUid)
                .collection("searchHistory")

            try {
                searchHistoryRef.document(documentIdToDelete).delete().await()

                result(UiState.Success(documentIdToDelete))
            } catch (e: Exception) {
                result(UiState.Failure("Deletion failed"))
            }
        } else {
            result(UiState.Failure("User not authenticated"))
        }
    }

    private fun sendNotification(title: String, message: String, currentUserId: String, token: String) {

        try {
            val jsonObject = JSONObject()

            val notificationObject = JSONObject()

            notificationObject.put("title", title)
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
        description: String
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
            description = " $description",
            timestamp = FieldValue.serverTimestamp()
        )

        notificationsCollection.add(notification)
            .addOnSuccessListener { documentReference ->
                val notificationId = documentReference.id
                val updatedNotification = notification.copy(id = notificationId)

                notificationsCollection.document(notificationId)
                    .set(updatedNotification)
            }
    }
}