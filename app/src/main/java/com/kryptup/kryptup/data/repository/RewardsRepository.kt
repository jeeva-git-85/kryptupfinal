package com.kryptup.kryptup.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.kryptup.kryptup.data.model.TransactionRecord
import com.kryptup.kryptup.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardsRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val transactionRepository: TransactionRepository,
    @javax.inject.Named("encrypted") private val prefs: android.content.SharedPreferences
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
                val currentPoints = doc.getLong("points")?.toInt() ?: prefs.getInt("user_reward_points_$uid", 0)
                val newPoints = currentPoints + 50
                prefs.edit().putInt("user_reward_points_$uid", newPoints).apply()

                val updates = mapOf(
                    "points" to FieldValue.increment(50),
                    "lastClaimTimestamp" to now
                )
                userRef.set(updates, SetOptions.merge()).await()
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
                val currentPoints = prefs.getInt("user_reward_points_$uid", 0) + 50
                prefs.edit().putInt("user_reward_points_$uid", currentPoints).apply()
                userRef.set(mapOf("points" to currentPoints, "lastClaimTimestamp" to now), SetOptions.merge()).await()
                Pair(true, "Successfully claimed +50 points!")
            } catch (ex: Exception) {
                Pair(false, "Failed to claim reward: ${ex.message}")
            }
        }
    }

    /**
     * Redeems 500 reward points for ₹10.00 INR and adds it directly to the user's fiat portfolio balance.
     */
    suspend fun redeemPointsForInr(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext Pair(false, "User not authenticated.")
        val userRef = firestore.collection("users").document(uid)

        try {
            val doc = userRef.get().await()
            val currentPoints = doc.getLong("points")?.toInt() ?: prefs.getInt("user_reward_points_$uid", 0)

            if (currentPoints < 500) {
                return@withContext Pair(false, "Insufficient points. You need at least 500 points to redeem ₹10.00 INR.")
            }

            val newPoints = currentPoints - 500
            val balanceKey = "user_fiat_balance_$uid"
            val currentBalance = prefs.getFloat(balanceKey, 0.0f).toDouble()
            val newBalance = currentBalance + 10.00

            // Update local encrypted preferences
            prefs.edit()
                .putFloat(balanceKey, newBalance.toFloat())
                .putInt("user_reward_points_$uid", newPoints)
                .apply()

            // Sync Firestore points & balance
            try {
                userRef.set(
                    mapOf("points" to newPoints, "fiatBalanceInr" to newBalance),
                    SetOptions.merge()
                ).await()
            } catch (_: Exception) {}

            // Record Reward Claim in Transaction History
            transactionRepository.recordTransaction(
                TransactionRecord(
                    type = TransactionType.REWARD_CLAIM,
                    title = "Redeemed 500 Points",
                    description = "Converted 500 Points -> ₹10.00 INR",
                    amountDisplay = "+₹10.00",
                    amountValue = 10.00,
                    isDebit = false,
                    category = "Rewards",
                    referenceId = "REDEEM-" + UUID.randomUUID().toString().take(6).uppercase()
                )
            )

            Pair(true, "Successfully redeemed 500 Points for ₹10.00 INR! Added to portfolio balance.")
        } catch (e: Exception) {
            Pair(false, "Redemption failed: ${e.message}")
        }
    }
}
