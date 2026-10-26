package com.kryptup.kryptup.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kryptup.kryptup.data.model.ChatMessage
import com.kryptup.kryptup.data.model.NotificationItem
import com.kryptup.kryptup.data.model.TransactionRecord
import com.kryptup.kryptup.data.repository.AuthRepository
import com.kryptup.kryptup.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Named

import com.google.firebase.firestore.FirebaseFirestore

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    @Named("encrypted") private val prefs: SharedPreferences
) : ViewModel() {

    private val _userFirstName = MutableStateFlow("Investor")
    val userFirstName: StateFlow<String> = _userFirstName.asStateFlow()

    private val _fiatBalanceInr = MutableStateFlow(0.00)
    val fiatBalanceInr: StateFlow<Double> = _fiatBalanceInr.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactions: StateFlow<List<TransactionRecord>> = _transactions.asStateFlow()

    private val _totalMonthlySpentUsd = MutableStateFlow(0.00)
    val totalMonthlySpentUsd: StateFlow<Double> = _totalMonthlySpentUsd.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var userProfileListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var chatRoomsRef: com.google.firebase.database.DatabaseReference? = null
    private var chatListener: ValueEventListener? = null

    private fun getLastReadTimestamp(): Long {
        val uid = auth.currentUser?.uid ?: "default"
        return prefs.getLong("chat_last_read_$uid", 0L)
    }

    private fun setLastReadTimestamp(time: Long) {
        val uid = auth.currentUser?.uid ?: "default"
        prefs.edit().putLong("chat_last_read_$uid", time).apply()
    }

    init {
        loadUserProfile()
        observeUserProfileRealtime()
        observeTransactions()
        listenForMessageNotifications()
    }

    private fun getLocalBalance(): Double {
        val uid = auth.currentUser?.uid ?: "default"
        return prefs.getFloat("user_fiat_balance_$uid", 0.0f).toDouble()
    }

    private fun setLocalBalance(balance: Double) {
        val uid = auth.currentUser?.uid ?: "default"
        prefs.edit().putFloat("user_fiat_balance_$uid", balance.toFloat()).apply()
    }

    private fun observeUserProfileRealtime() {
        val uid = auth.currentUser?.uid ?: return
        userProfileListener?.remove()
        userProfileListener = firestore.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val remoteBalance = snapshot.getDouble("fiatBalanceInr") ?: 0.00
                setLocalBalance(remoteBalance)
                _fiatBalanceInr.value = remoteBalance
            } else {
                _fiatBalanceInr.value = getLocalBalance()
            }
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val userProfile = authRepository.getCurrentUserProfile()
            val fullName = userProfile?.displayName.takeIf { !it.isNullOrEmpty() }
                ?: auth.currentUser?.displayName.takeIf { !it.isNullOrEmpty() }
                ?: auth.currentUser?.email?.substringBefore("@")
                ?: "Investor"

            val firstName = fullName.trim().split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Investor"
            _userFirstName.value = firstName

            val remoteBalance = userProfile?.fiatBalanceInr
            if (remoteBalance != null) {
                setLocalBalance(remoteBalance)
                _fiatBalanceInr.value = remoteBalance
            } else {
                _fiatBalanceInr.value = getLocalBalance()
            }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionRepository.observeTransactions().collect { list ->
                _transactions.value = list
                val totalSpentInrOrUsd = transactionRepository.calculateTotalSpent(list)
                _totalMonthlySpentUsd.value = totalSpentInrOrUsd
            }
        }
    }

    private fun listenForMessageNotifications() {
        val currentUid = auth.currentUser?.uid ?: return
        chatRoomsRef = database.getReference("chat_rooms")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newNotifications = mutableListOf<NotificationItem>()
                val lastRead = getLastReadTimestamp()

                for (roomSnap in snapshot.children) {
                    val roomId = roomSnap.key ?: continue
                    if (roomId.contains(currentUid)) {
                        val messagesSnap = roomSnap.child("messages")
                        for (msgSnap in messagesSnap.children) {
                            val msg = msgSnap.getValue(ChatMessage::class.java) ?: continue
                            // Surface unread messages received from others after last read timestamp
                            if (msg.senderId != currentUid && msg.timestamp > lastRead) {
                                newNotifications.add(
                                    NotificationItem(
                                        id = msg.id.ifEmpty { msgSnap.key ?: "" },
                                        senderName = msg.senderName.ifEmpty { "KryptUp Contact" },
                                        senderUid = msg.senderId,
                                        messageText = msg.text,
                                        timestamp = msg.timestamp
                                    )
                                )
                            }
                        }
                    }
                }
                val sorted = newNotifications.sortedByDescending { it.timestamp }
                _notifications.value = sorted
                _unreadCount.value = sorted.size
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        chatListener = listener
        chatRoomsRef?.addValueEventListener(listener)
    }

    fun markNotificationsRead() {
        setLastReadTimestamp(System.currentTimeMillis())
        _unreadCount.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        userProfileListener?.remove()
        chatListener?.let { chatRoomsRef?.removeEventListener(it) }
    }
}
