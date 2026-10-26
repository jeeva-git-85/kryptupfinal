package com.kryptup.kryptup.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kryptup.kryptup.data.model.TransactionRecord
import com.kryptup.kryptup.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    fun observeTransactions(): Flow<List<TransactionRecord>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid.isNullOrEmpty()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(currentUid)
            .collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val userList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val typeStr = doc.getString("type") ?: TransactionType.UPI_SEND.name
                        val type = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.UPI_SEND }
                        val title = doc.getString("title") ?: ""
                        val description = doc.getString("description") ?: ""
                        val amountDisplay = doc.getString("amountDisplay") ?: ""
                        val amountValue = doc.getDouble("amountValue") ?: 0.0
                        val isDebit = doc.getBoolean("isDebit") ?: true
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val status = doc.getString("status") ?: "CONFIRMED"
                        val category = doc.getString("category") ?: "Transfer"
                        val referenceId = doc.getString("referenceId") ?: ""

                        TransactionRecord(
                            id = id,
                            userId = currentUid,
                            type = type,
                            title = title,
                            description = description,
                            amountDisplay = amountDisplay,
                            amountValue = amountValue,
                            isDebit = isDebit,
                            timestamp = timestamp,
                            status = status,
                            category = category,
                            referenceId = referenceId
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                trySend(userList)
            }

        awaitClose { listener.remove() }
    }

    suspend fun recordTransaction(record: TransactionRecord) = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: return@withContext
        val txId = record.id.ifEmpty { "tx_" + UUID.randomUUID().toString().take(8) }
        val finalRecord = record.copy(id = txId, userId = currentUid)

        try {
            val map = hashMapOf(
                "type" to finalRecord.type.name,
                "title" to finalRecord.title,
                "description" to finalRecord.description,
                "amountDisplay" to finalRecord.amountDisplay,
                "amountValue" to finalRecord.amountValue,
                "isDebit" to finalRecord.isDebit,
                "timestamp" to finalRecord.timestamp,
                "status" to finalRecord.status,
                "category" to finalRecord.category,
                "referenceId" to finalRecord.referenceId
            )
            firestore.collection("users")
                .document(currentUid)
                .collection("transactions")
                .document(txId)
                .set(map)
                .await()
        } catch (e: Exception) {
            // Failed to persist transaction record — surface to logging in production
            android.util.Log.e("TransactionRepository", "Failed to record transaction: ${e.message}", e)
        }
    }

    fun calculateTotalSpent(records: List<TransactionRecord>): Double {
        return records.filter { it.isDebit }.sumOf { it.amountValue }
    }
}
