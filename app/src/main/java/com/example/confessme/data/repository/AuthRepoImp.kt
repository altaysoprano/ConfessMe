package com.example.confessme.data.repository

import android.util.Log
import com.example.confessme.data.model.User
import com.example.confessme.util.UiState
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore,
): AuthRepo {
    override fun signIn(email: String, pass: String, result: (UiState<String>) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                result.invoke(UiState.Success("Login successful"))
            } else {
                val exception = task.exception
                if (exception is FirebaseAuthInvalidUserException) {
                    result.invoke(UiState.Failure("Invalid email."))
                } else if (exception is FirebaseAuthInvalidCredentialsException) {
                    result.invoke(UiState.Failure("Invalid password."))
                } else {
                    result.invoke(UiState.Failure("An error occurred. Please try again later."))
                }
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
                    result.invoke(UiState.Failure("Password must contain at least one uppercase letter, one digit, one special character, and must be at least 8 characters long. It should not contain spaces."))
                    return
                }
                val randomUsername = generateRandomUsername(10)
                firebaseAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            if (user != null) {
                                val uid = user.uid
                                Log.d("Mesaj: ", "uid: $uid")
                                database.collection("users").document(uid)
                                    .set(User(uid = uid, email = email, password = pass, userName = randomUsername))
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

    override fun updatePassword(previousPassword: String, newPassword: String, result: (UiState<String>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user == null) {
            result.invoke(UiState.Failure("User is not signed in."))
            return
        }

        if (!isValidPassword(newPassword)) {
            result.invoke(UiState.Failure("Password must contain at least one uppercase letter, one digit, one special character, and must be at least 8 characters long. It should not contain spaces."))
            return
        }

        if (previousPassword == newPassword) {
            result.invoke(UiState.Failure("New password cannot be the same as the previous password."))
            return
        }

        val credential = EmailAuthProvider.getCredential(user.email ?: "", previousPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                result.invoke(UiState.Success("Password updated successfully."))
                            } else {
                                val exception = task.exception
                                result.invoke(UiState.Failure("Password update failed: ${exception?.localizedMessage}"))
                            }
                        }
                } else {
                    result.invoke(UiState.Failure("Reauthentication failed. Make sure you entered the correct current password."))
                }
            }
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$".toRegex()
        if (password.contains(" ")) {
            return false
        }
        return passwordRegex.matches(password)
    }

    private fun generateRandomUsername(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

}