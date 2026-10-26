package com.kryptup.kryptup.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kryptup.kryptup.data.model.ChatMessage
import com.kryptup.kryptup.data.model.User
import com.kryptup.kryptup.data.repository.AuthRepository
import com.kryptup.kryptup.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoadingUsers = MutableStateFlow(true)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers.asStateFlow()

    val currentUid: String
        get() = auth.currentUser?.uid ?: "anonymous"

    private var messageJob: Job? = null

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            _users.value = authRepository.getAllUsers()
            _isLoadingUsers.value = false
        }
    }

    fun selectUser(user: User) {
        _selectedUser.value = user
        val uid = currentUid
        val roomId = chatRepository.getRoomId(uid, user.uid)

        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            chatRepository.getRoomMessages(roomId).collect { msgList ->
                if (msgList.isNotEmpty()) {
                    _messages.value = msgList.sortedBy { it.timestamp }
                }
            }
        }
    }

    fun clearSelectedUser() {
        messageJob?.cancel()
        _selectedUser.value = null
        _messages.value = emptyList()
    }

    fun sendMessage(text: String) {
        val recipient = _selectedUser.value ?: return
        val uid = currentUid
        val currentName = auth.currentUser?.displayName.takeIf { !it.isNullOrEmpty() }
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: "User"
        val roomId = chatRepository.getRoomId(uid, recipient.uid)

        val message = ChatMessage(
            senderId = uid,
            senderName = currentName,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        // Immediate Optimistic Update: Append message to local list
        val updatedList = _messages.value.toMutableList()
        updatedList.add(message)
        _messages.value = updatedList.sortedBy { it.timestamp }

        // Async Push to Firebase Realtime DB
        chatRepository.sendMessageToRoom(roomId, message)
    }

    fun sendReceipt(amount: String, symbol: String, recipientUid: String) {
        val uid = currentUid
        val currentName = auth.currentUser?.displayName.takeIf { !it.isNullOrEmpty() }
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: "User"
        val roomId = chatRepository.getRoomId(uid, recipientUid)

        val message = ChatMessage(
            senderId = uid,
            senderName = currentName,
            text = "Sent $amount $symbol to user",
            isReceipt = true,
            receiptAmount = "$amount $symbol",
            timestamp = System.currentTimeMillis()
        )

        val updatedList = _messages.value.toMutableList()
        updatedList.add(message)
        _messages.value = updatedList.sortedBy { it.timestamp }

        chatRepository.sendMessageToRoom(roomId, message)
    }
}
