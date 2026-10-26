package com.kryptup.kryptup.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardsRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    /**
     * Calls the [claimDailyReward] Cloud Function, or falls back to direct Firestore mutation
     * with 24-hour streak validation if the Cloud Function is not deployed.
     */
    suspend fun claimDailyLoginReward(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext Pair(false, "User not authenticated.")

        // 1. Attempt Cloud Function call
        try {
            val result = functions
                .getHttpsCallable("claimDailyReward")
                .call()
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any>
            if (data?.get("success") == true) {
                return@withContext Pair(true, "Successfully claimed +50 points!")
            }
        } catch (e: Exception) {
            android.util.Log.w("RewardsRepository", "Cloud function unavailable (${e.message}), executing direct Firestore fallback")
        }

        // 2. Direct Firestore Fallback with 24-hour streak check
        try {
            val userRef = firestore.collection("users").document(uid)
            val doc = userRef.get().await()
            val now = System.currentTimeMillis()
            val lastClaim = doc.getLong("lastClaimTimestamp") ?: 0L
            val twentyFourHoursMs = 24 * 60 * 60 * 1000L

            if (now - lastClaim >= twentyFourHoursMs) {
                val updates = mapOf(
                    "points" to FieldValue.increment(50),
                    "lastClaimTimestamp" to now
                )
                userRef.update(updates).await()
                Pair(true, "Successfully claimed +50 points!")
            } else {
                val remainingHours = ((twentyFourHoursMs - (now - lastClaim)) / (60 * 60 * 1000L)).coerceAtLeast(1)
                Pair(false, "Daily reward already claimed. Next claim available in ~$remainingHours hr(s).")
            }
        } catch (e: Exception) {
            android.util.Log.e("RewardsRepository", "Firestore fallback claim failed: ${e.message}", e)
            try {
                val userRef = firestore.collection("users").document(uid)
                val now = System.currentTimeMillis()
                userRef.set(mapOf("points" to 50, "lastClaimTimestamp" to now), SetOptions.merge()).await()
                Pair(true, "Successfully claimed +50 points!")
            } catch (ex: Exception) {
                Pair(false, "Failed to claim reward: ${ex.message}")
            }
        }
    }
}
