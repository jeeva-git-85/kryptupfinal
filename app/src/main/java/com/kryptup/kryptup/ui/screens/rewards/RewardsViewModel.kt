package com.kryptup.kryptup.ui.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kryptup.kryptup.data.repository.RewardsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val rewardsRepository: RewardsRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _points = MutableStateFlow(0)
    val points: StateFlow<Int> = _points.asStateFlow()

    private val _claimMessage = MutableStateFlow<String?>(null)
    val claimMessage: StateFlow<String?> = _claimMessage.asStateFlow()

    private var pointsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        listenToUserPoints()
    }

    private fun listenToUserPoints() {
        val uid = auth.currentUser?.uid ?: return
        pointsListener?.remove()
        pointsListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val pts = snapshot.getLong("points")?.toInt() ?: 0
                    _points.value = pts
                }
            }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val (_, message) = rewardsRepository.claimDailyLoginReward()
            _claimMessage.value = message
        }
    }

    override fun onCleared() {
        super.onCleared()
        pointsListener?.remove()
    }
}
