package com.example.confessme.data.repository

import android.content.Context
import android.util.Log
import com.example.confessme.R
import com.example.confessme.data.model.User
import com.example.confessme.util.UiState
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext

class AuthRepoImp(
    private val firebaseAuth: FirebaseAuth,
    private val database: FirebaseFirestore,
    @ApplicationContext private val context: Context
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
                            database.collection("users").document(uid)
                                .get()
                                .addOnSuccessListener { documentSnapshot ->
                                    val userName = documentSnapshot.getString("userName") ?: ""
                                    result.invoke(UiState.Success(userName))
                                }
                                .addOnFailureListener { exception ->
                                    result.invoke(
                                        UiState.Failure(
                                            exception.localizedMessage ?: context.getString(R.string.token_update_failed)
                                        )
                                    )
                                }
                        }
                        .addOnFailureListener { exception ->
                            result.invoke(
                                UiState.Failure(
                                    exception.localizedMessage ?: context.getString(R.string.token_update_failed)
                                )
                            )
                        }
                }.addOnFailureListener { exception ->
                    result.invoke(
                        UiState.Failure(
                            exception.localizedMessage ?: context.getString(R.string.fcm_token_retrieval_failed)
                        )
                    )
                }
            } else {
                val exception = task.exception
                if (exception is FirebaseAuthInvalidUserException) {
                    result.invoke(UiState.Failure(context.getString(R.string.invalid_email)))
                } else if (exception is FirebaseAuthInvalidCredentialsException) {
                    result.invoke(UiState.Failure(context.getString(R.string.invalid_password)))
                } else {
                    result.invoke(UiState.Failure(context.getString(R.string.an_error_occurred_please_try_again_later)))
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
            if (!isValidEmail(email)) {
                result.invoke(UiState.Failure(context.getString(R.string.invalid_email)))
                return
            }
            if (pass == confirmPass) {
                if (!isValidPassword(pass)) {
                    result.invoke(UiState.Failure(context.getString(R.string.password_must_contain_at_least_one_uppercase_letter_one_digit_one_special_character_and_must_be_at_least_8_characters_long_it_should_not_contain_spaces)))
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
                                        .addOnSuccessListener { result.invoke(UiState.Success(context.getString(
                                                                                    R.string.successfully_signed_up))) }
                                        .addOnFailureListener { exception ->
                                            result.invoke(UiState.Failure(exception.localizedMessage))
                                        }
                                }.addOnFailureListener { exception ->
                                    result.invoke(
                                        UiState.Failure(
                                            exception.localizedMessage
                                                ?: context.getString(R.string.fcm_token_retrieval_failed)
                                        )
                                    )
                                }
                            }
                        } else {
                            val exception = authTask.exception
                            if (exception is FirebaseAuthUserCollisionException) {
                                result.invoke(UiState.Failure(context.getString(R.string.user_already_exists)))
                            } else {
                                result.invoke(UiState.Failure(context.getString(R.string.unknown_error) + exception?.localizedMessage))
                            }
                        }
                    }
            } else {
                result.invoke(UiState.Failure(context.getString(R.string.passwords_do_not_match)))
            }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.please_fill_in_all_fields)))
        }
    }

    override fun signOut(result: (UiState<String>) -> Unit) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            firebaseAuth.signOut()
            result.invoke(UiState.Success(context.getString(R.string.logout_successful)))
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.no_user_signed_in)))
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
                            result.invoke(UiState.Success(context.getString(R.string.logout_successful)))
                        }
                        .addOnFailureListener { exception ->
                            result.invoke(
                                UiState.Failure(
                                    exception.localizedMessage ?: context.getString(R.string.token_deletion_failed)
                                )
                            )
                        }
                }
                .addOnFailureListener { exception ->
                    result.invoke(
                        UiState.Failure(
                            exception.localizedMessage ?: context.getString(R.string.fcm_token_deletion_failed)
                        )
                    )
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.no_user_signed_in)))
        }
    }
*/

    override fun googleSignIn(
        idToken: String,
        googleSignInAccount: GoogleSignInAccount?,
        result: (UiState<String>) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val uid = user?.uid ?: return@addOnCompleteListener

                    val email = googleSignInAccount?.email ?: ""
                    val photoUrl = googleSignInAccount?.photoUrl?.toString() ?: ""

                    database.collection("users").document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            val storedUserName = document.getString("userName")
                            val storedBio = document.getString("bio")

                            //Google sign in'de daha önceden ayarlanmış bir username, bio veya imageUrl varsa onları çekiyoruz,
                            // Yoksa random username, boş bio ve google account'ın imageUrl'ini kullanıyoruz
                            val userName = if (storedUserName.isNullOrEmpty()) {
                                generateRandomUsername(10)
                            } else {
                                storedUserName
                            }

                            val bio = if (!storedBio.isNullOrEmpty()) {
                                storedBio
                            } else {
                                ""
                            }

                            // Burada fotoyu remove edip geri geldiğinde google fotoğrafı tekrar geliyordu,
                            // bu düzenleme bunun için yapıldı
                            val imageUrl = document.getString("imageUrl") ?: photoUrl

                            FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                                val googleSignInUser = User(
                                    uid = uid,
                                    email = email,
                                    userName = userName,
                                    token = fcmToken,
                                    imageUrl = imageUrl,
                                    bio = bio
                                )

                                database.collection("users").document(uid)
                                    .set(googleSignInUser)
                                    .addOnSuccessListener {
                                        result.invoke(UiState.Success(userName))
                                    }
                                    .addOnFailureListener { exception ->
                                        result.invoke(
                                            UiState.Failure(
                                                exception.localizedMessage
                                                    ?: context.getString(R.string.user_data_update_failed)
                                            )
                                        )
                                    }
                            }.addOnFailureListener { exception ->
                                result.invoke(
                                    UiState.Failure(
                                        exception.localizedMessage
                                            ?: context.getString(R.string.fcm_token_retrieval_failed)
                                    )
                                )
                            }
                        }
                        .addOnFailureListener { exception ->
                            result.invoke(
                                UiState.Failure(
                                    exception.localizedMessage ?: context.getString(R.string.error_getting_user_data)
                                )
                            )
                        }
                } else {
                    result.invoke(UiState.Failure(context.getString(R.string.google_sign_in_failed)))
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        result.invoke(UiState.Failure(context.getString(R.string.user_already_exists)))
                    } else {
                        result.invoke(UiState.Failure(context.getString(R.string.google_sign_in_failed)))
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

        //Burada kullanıcı eğer Google hesabıyla giriş yapmışsa gerekli uyarı veriliyor.
        user?.let {
            val providerData = it.providerData

            for (profile in providerData) {
                val providerId = profile.providerId

                if (providerId == GoogleAuthProvider.PROVIDER_ID) {
                    result.invoke(UiState.Failure(context.getString(R.string.you_ve_logged_in_with_your_google_account_please_visit_the_google_account_management_page_to_change_your_password)))
                    return
                }
            }
        }

        if (user == null) {
            result.invoke(UiState.Failure(context.getString(R.string.user_is_not_signed_in)))
            return
        }

        if (!isValidPassword(newPassword)) {
            result.invoke(UiState.Failure(context.getString(R.string.password_must_contain_at_least_one_uppercase_letter_one_digit_one_special_character_and_must_be_at_least_8_characters_long_it_should_not_contain_spaces)))
            return
        }

        if (previousPassword == newPassword) {
            result.invoke(UiState.Failure(context.getString(R.string.new_password_cannot_be_the_same_as_the_previous_password)))
            return
        }

        val credential = EmailAuthProvider.getCredential(user.email ?: "", previousPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                result.invoke(UiState.Success(context.getString(R.string.password_updated_successfully)))
                            } else {
                                val exception = task.exception
                                result.invoke(UiState.Failure(context.getString(R.string.password_update_failed) + " ${exception?.localizedMessage}"))
                            }
                        }
                } else {
                    result.invoke(UiState.Failure(context.getString(R.string.reauthentication_failed_make_sure_you_entered_the_correct_current_password)))
                }
            }
    }

    override fun deleteAccountWithConfessionsAndSignOut(
        currentPassword: String,
        result: (UiState<String>) -> Unit
    ) {
        val user = firebaseAuth.currentUser
        val email = user?.email

        if (user != null && !email.isNullOrEmpty()) {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)

            user.reauthenticate(credential)
                .addOnCompleteListener { reAuthTask ->
                    if (reAuthTask.isSuccessful) {
                        val uid = user.uid
                        val confessionsRef = database.collection("confessions")

                        confessionsRef.whereEqualTo("fromUserId", uid)
                            .get()
                            .addOnSuccessListener { documents ->
                                val batch = database.batch()

                                for (document in documents) {
                                    val docRef = confessionsRef.document(document.id)
                                    batch.delete(docRef)
                                }

                                batch.commit()
                                    .addOnCompleteListener { deleteAllTask ->
                                        if (deleteAllTask.isSuccessful) {
                                            user.delete()
                                                .addOnCompleteListener { deleteAccountTask ->
                                                    if (deleteAccountTask.isSuccessful) {
                                                        firebaseAuth.signOut()
                                                        result.invoke(UiState.Success(context.getString(R.string.account_deleted_successfully)))
                                                    } else {
                                                        result.invoke(
                                                            UiState.Failure(
                                                                context.getString(R.string.error_deleting_account) + ": " + deleteAccountTask.exception?.localizedMessage
                                                            )
                                                        )
                                                    }
                                                }
                                        } else {
                                            result.invoke(UiState.Failure(context.getString(R.string.error_deleting_account) + ": " + deleteAllTask.exception?.localizedMessage))
                                        }
                                    }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(
                                    UiState.Failure(
                                        context.getString(R.string.an_error_occured_please_try_again) + ": " + exception.localizedMessage
                                    )
                                )
                            }
                    } else {
                        result.invoke(UiState.Failure(context.getString(R.string.invalid_password)))
                    }
                }
        } else {
            result.invoke(UiState.Failure(context.getString(R.string.no_user_signed_in)))
        }
    }

    override fun updateLanguage(language: String) {
        val user = firebaseAuth.currentUser

        if (user != null) {
            val uid = user.uid

            database.collection("users").document(uid)
                .update("language", language)
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordRegex = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$".toRegex()
        if (password.contains(" ")) {
            return false
        }
        return passwordRegex.matches(password)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun generateRandomUsername(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

}