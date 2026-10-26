package com.kryptup.kryptup.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kryptup.kryptup.data.model.ChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    fun getRoomId(currentUid: String, otherUid: String): String {
        return if (currentUid < otherUid) "${currentUid}_${otherUid}" else "${otherUid}_${currentUid}"
    }

    fun getRoomMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val roomRef = database.getReference("chat_rooms").child(roomId).child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    child.getValue(ChatMessage::class.java)?.let {
                        messages.add(it)
                    }
                }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        roomRef.addValueEventListener(listener)
        awaitClose { roomRef.removeEventListener(listener) }
    }

    fun sendMessageToRoom(roomId: String, message: ChatMessage) {
        val roomRef = database.getReference("chat_rooms").child(roomId).child("messages")
        val newRef = roomRef.push()
        val messageWithId = message.copy(id = newRef.key ?: "")
        newRef.setValue(messageWithId)
    }
}
