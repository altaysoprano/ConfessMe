package com.example.confessme.data.repository

import android.util.Log
import com.example.confessme.data.model.User
import com.example.confessme.util.UiState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class AuthRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore,
) : AuthRepo {
    override fun signIn(email: String, pass: String, result: (UiState<String>) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                    val user = firebaseAuth.currentUser
                    val uid = user?.uid ?: return@addOnSuccessListener

                    database.collection("users").document(uid)
                        .update("token", fcmToken)
                        .addOnSuccessListener {
                            result.invoke(UiState.Success("Login successful"))
                        }
                        .addOnFailureListener { exception ->
                            result.invoke(
                                UiState.Failure(
                                    exception.localizedMessage ?: "Token update failed"
                                )
                            )
                        }
                }.addOnFailureListener { exception ->
                    result.invoke(
                        UiState.Failure(
                            exception.localizedMessage ?: "FCM Token retrieval failed"
                        )
                    )
                }
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
                                FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                                    database.collection("users").document(uid)
                                        .set(
                                            User(
                                                uid = uid,
                                                email = email,
                                                password = pass,
                                                userName = randomUsername,
                                                token = fcmToken
                                            )
                                        )
                                        .addOnSuccessListener { result.invoke(UiState.Success("Successfully signed up")) }
                                        .addOnFailureListener { exception ->
                                            result.invoke(UiState.Failure(exception.localizedMessage))
                                        }
                                }.addOnFailureListener { exception ->
                                    result.invoke(
                                        UiState.Failure(
                                            exception.localizedMessage
                                                ?: "FCM Token retrieval failed"
                                        )
                                    )
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

    override fun signOut(result: (UiState<String>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            firebaseAuth.signOut()
            result.invoke(UiState.Success("Logout successful"))
        } else {
            result.invoke(UiState.Failure("No user signed in"))
        }
    }

    /*
        override fun signOut(result: (UiState<String>) -> Unit) {
            val user = firebaseAuth.currentUser

            if (user != null) {
                FirebaseMessaging.getInstance().deleteToken()
                    .addOnSuccessListener {
                        val uid = user.uid
                        database.collection("users").document(uid)
                            .update("token", "")
                            .addOnSuccessListener {
                                firebaseAuth.signOut()
                                result.invoke(UiState.Success("Logout successful"))
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(
                                    UiState.Failure(
                                        exception.localizedMessage ?: "Token deletion failed"
                                    )
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        result.invoke(
                            UiState.Failure(
                                exception.localizedMessage ?: "FCM Token deletion failed"
                            )
                        )
                    }
            } else {
                result.invoke(UiState.Failure("No user signed in"))
            }
        }
    */

    override fun googleSignIn(idToken: String, googleSignInAccount: GoogleSignInAccount?, result: (UiState<String>) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val uid = user?.uid ?: return@addOnCompleteListener

                    val email = googleSignInAccount?.email ?: ""
                    val photoUrl = googleSignInAccount?.photoUrl?.toString() ?: ""

                    val randomUsername = generateRandomUsername(10)
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->

                        val newUser = User(
                            uid = uid,
                            email = email,
                            userName = randomUsername,
                            token = fcmToken,
                            imageUrl = photoUrl
                        )

                        database.collection("users").document(uid)
                            .set(newUser)
                            .addOnSuccessListener {
                                result.invoke(UiState.Success("Google Sign-In successful"))
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(
                                    UiState.Failure(
                                        exception.localizedMessage ?: "User data update failed"
                                    )
                                )
                            }
                    }.addOnFailureListener { exception ->
                        result.invoke(
                            UiState.Failure(
                                exception.localizedMessage
                                    ?: "FCM Token retrieval failed"
                            )
                        )
                    }
                } else {
                    result.invoke(UiState.Failure("Google Sign-In failed"))
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        result.invoke(UiState.Failure("User already exists"))
                    } else {
                        result.invoke(UiState.Failure("Google Sign-In failed"))
                    }
                }
            }
    }

    override fun updatePassword(
        previousPassword: String,
        newPassword: String,
        result: (UiState<String>) -> Unit
    ) {
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