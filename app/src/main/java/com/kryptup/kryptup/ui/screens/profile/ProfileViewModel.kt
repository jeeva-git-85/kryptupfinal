package com.kryptup.kryptup.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kryptup.kryptup.data.model.User
import com.kryptup.kryptup.data.repository.AuthRepository
import com.kryptup.kryptup.data.wallet.WalletManager
import com.kryptup.kryptup.security.PinSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val walletManager: WalletManager,
    private val auth: FirebaseAuth,
    val pinSecurityManager: PinSecurityManager
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _walletAddress = MutableStateFlow("")
    val walletAddress: StateFlow<String> = _walletAddress.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = auth.currentUser
            val uid = current?.uid ?: ""
            val defaultDisplayName = current?.displayName.takeIf { !it.isNullOrEmpty() }
                ?: current?.email?.substringBefore("@")
                ?: "KryptUp Investor"
            val defaultEmail = current?.email ?: "user@kryptup.com"
            val sanitized = defaultDisplayName.lowercase().replace("[^a-z0-9]".toRegex(), "").ifEmpty { "user" }
            val uidSuffix = uid.take(4).lowercase()

            _user.value = User(
                uid = uid,
                email = defaultEmail,
                displayName = defaultDisplayName,
                upiId = "$sanitized$uidSuffix@kryptup",
                fiatBalanceInr = 0.00
            )

            val userProfile = authRepository.getCurrentUserProfile()
            if (userProfile != null) {
                _user.value = userProfile
            }

            val savedMnemonic = walletManager.getMnemonic()
            if (savedMnemonic != null) {
                val addr = walletManager.createWallet(savedMnemonic, "")
                _walletAddress.value = addr
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
