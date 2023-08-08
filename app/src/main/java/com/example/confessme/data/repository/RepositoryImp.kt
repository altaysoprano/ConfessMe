package com.example.confessme.data.repository

import com.example.confessme.data.model.User
import com.example.confessme.util.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class RepositoryImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore
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
                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        if (user != null) {
                            val uid = user.uid
                            database.collection("users").document(uid)
                                .set(User(email, pass))
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

    fun isValidPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$".toRegex()
        return passwordRegex.matches(password)
    }
}