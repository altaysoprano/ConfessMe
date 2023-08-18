package com.example.confessme.data.repository

import android.net.Uri
import android.util.Log
import com.example.confessme.data.model.User
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
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

    override fun signUp(email: String, pass: String, confirmPass: String, result: (UiState<String>) -> Unit) {
        // Bütün alanların dolu olup olmadığının kontrolü
        if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
            // Şifrelerin uyuşup uyuşmadığının kontrolü
            if (pass == confirmPass) {
                // Şifre geçerliliği kontrolü
                if (!isValidPassword(pass)) {
                    result.invoke(UiState.Failure("Password must contain at least one uppercase letter, one digit, one special character and must be at least 8 characters long."))
                    return
                }
                val randomUsername = generateRandomUsername(10)
                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { authTask ->
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
                            // Kullanıcı zaten varsa
                            result.invoke(UiState.Failure("User already exists"))
                        } else {
                            // Farklı bir hata durumu
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

    override fun updateProfile(previousUserName: String, userName: String, bio: String, imageUri: Uri, result: (UiState<String>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val uid = user.uid

            // Kullanıcı adı boşsa veya 3 karakterden kısa ise hata döndür
            if (userName.isBlank() || userName.length < 3) {
                result.invoke(UiState.Failure("Username must be at least 3 characters long."))
                return
            }

            // Kullanıcı adının daha önce alınıp alınmadığını kontrol et
            database.collection("users")
                .whereEqualTo("userName", userName)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty && previousUserName != userName) {
                        result.invoke(UiState.Failure("Username is already taken. Please choose a different one."))
                    } else {
                        // Kullanıcı adı uygunsa güncelleme işlemini yap
                        val userDocument = database.collection("users").document(uid)

                        val profileUpdate = mutableMapOf<String, Any?>(
                            "userName" to userName,
                            "bio" to bio
                        )

                        // Eğer imageUri boş değilse, profil fotoğrafını da güncelle
                        if (imageUri != Uri.EMPTY) {
                            val reference = storage.reference.child("Profile").child(Date().time.toString())
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

            // Kullanıcının belirli bir alanını Firestore'dan çekme işlemi
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

    override fun searchUsers(query: String, result: (UiState<List<User>>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            database.collection("users")
                .orderBy("userName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .addOnSuccessListener { documents ->
                    val userList = mutableListOf<User>()

                    for (document in documents) {
                        val user = document.toObject(User::class.java)
                        userList.add(user)
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

    fun isValidPassword(password: String): Boolean {
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