package com.kryptup.kryptup.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.kryptup.kryptup.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        try {
            val currentUid = auth.currentUser?.uid ?: ""
            val snapshot = firestore.collection("users").get().await()
            snapshot.documents.mapNotNull { doc ->
                val uid = doc.getString("uid").takeIf { !it.isNullOrEmpty() } ?: doc.id
                val email = doc.getString("email") ?: ""
                val displayName = doc.getString("displayName").takeIf { !it.isNullOrEmpty() }
                    ?: email.substringBefore("@").ifEmpty { "KryptUp User" }
                val upiId = doc.getString("upiId") ?: ""
                val fiatBalance = doc.getDouble("fiatBalanceInr") ?: 0.0

                if (uid.isNotEmpty() && uid != currentUid) {
                    User(
                        uid = uid,
                        email = email,
                        displayName = displayName,
                        upiId = upiId,
                        fiatBalanceInr = fiatBalance
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun parseAuthError(e: Exception): Exception {
        val msg = e.localizedMessage ?: ""
        return when {
            e is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> {
                Exception("Password must be at least 6 characters long.")
            }
            e is com.google.firebase.auth.FirebaseAuthUserCollisionException -> {
                Exception("An account already exists with this email address. Please login instead.")
            }
            e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                Exception("Invalid email address or credentials.")
            }
            msg.contains("unexpected end of stream") || msg.contains("IOException") || msg.contains("internal error") || msg.contains("API key") || msg.contains("PROJECT_FAULT") -> {
                Exception("Firebase connection failed. Please check your internet connection or Firebase setup.")
            }
            else -> e
        }
    }

    suspend fun login(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        try {
            kotlinx.coroutines.withTimeout(15000L) {
                val result = auth.signInWithEmailAndPassword(cleanEmail, pass).await()
                result.user?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Login failed: User is null"))
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(Exception("Login timed out. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(parseAuthError(e))
        }
    }

    suspend fun getCurrentUserProfile(): User? = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext null
        val email = auth.currentUser?.email ?: ""
        val name = auth.currentUser?.displayName ?: "User"
        val sanitized = name.lowercase().replace("[^a-z0-9]".toRegex(), "").ifEmpty { "user" }
        val uidSuffix = uid.take(4).lowercase()
        val upiId = "$sanitized$uidSuffix@kryptup"

        try {
            val docRef = firestore.collection("users").document(uid)
            val doc = docRef.get().await()
            if (doc.exists()) {
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    if (user.upiId.isEmpty()) {
                        docRef.set(mapOf("upiId" to upiId), com.google.firebase.firestore.SetOptions.merge())
                    }
                    return@withContext user.copy(upiId = user.upiId.ifEmpty { upiId })
                }
            }
            // Auto-ensure user document exists in Firestore
            val newUser = User(
                uid = uid,
                email = email,
                displayName = name,
                upiId = upiId,
                fiatBalanceInr = 0.00
            )
            docRef.set(newUser, com.google.firebase.firestore.SetOptions.merge()).await()
            newUser
        } catch (e: Exception) {
            User(uid = uid, email = email, displayName = name, upiId = upiId, fiatBalanceInr = 0.00)
        }
    }

    suspend fun register(email: String, pass: String, name: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            kotlinx.coroutines.withTimeout(15000L) {
                val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                val firebaseUser = result.user ?: throw Exception("Registration failed: User is null")
                
                // Update Firebase Auth user profile with display name
                try {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name.trim())
                        .build()
                    firebaseUser.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val sanitized = name.trim().lowercase().replace("[^a-z0-9]".toRegex(), "").ifEmpty { "user" }
                val uidSuffix = firebaseUser.uid.take(4).lowercase()
                val upiId = "$sanitized$uidSuffix@kryptup"

                val user = User(
                    uid = firebaseUser.uid,
                    email = email.trim(),
                    displayName = name.trim(),
                    upiId = upiId,
                    fiatBalanceInr = 0.00
                )
                try {
                    firestore.collection("users").document(firebaseUser.uid).set(user).await()
                } catch (fsEx: Exception) {
                    fsEx.printStackTrace()
                }
                
                Result.success(firebaseUser)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(Exception("Registration timed out after 15 seconds. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(parseAuthError(e))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(Exception("Please enter a valid email address (e.g. user@gmail.com)"))
        }
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Retry once on network glitch
            try {
                auth.sendPasswordResetEmail(cleanEmail).await()
                Result.success(Unit)
            } catch (retryEx: Exception) {
                Result.failure(parseAuthError(retryEx))
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun firebaseSignInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google sign in failed: User is null")
            
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            if (!userDoc.exists()) {
                val displayName = firebaseUser.displayName ?: "User"
                val sanitized = displayName.lowercase().replace("[^a-z0-9]".toRegex(), "").ifEmpty { "user" }
                val uidSuffix = firebaseUser.uid.take(8).lowercase()
                val upiId = "$sanitized$uidSuffix@kryptup"

                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = displayName,
                    upiId = upiId,
                    fiatBalanceInr = 0.00
                )
                firestore.collection("users").document(firebaseUser.uid).set(user).await()
            }
            
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
