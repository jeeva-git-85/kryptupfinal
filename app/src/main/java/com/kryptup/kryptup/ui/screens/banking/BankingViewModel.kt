package com.kryptup.kryptup.ui.screens.banking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kryptup.kryptup.data.repository.BankingRepository
import com.kryptup.kryptup.security.PinSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BankingViewModel @Inject constructor(
    private val repository: BankingRepository,
    val pinSecurityManager: PinSecurityManager
) : ViewModel() {

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _linkingSuccess = MutableStateFlow(false)
    val linkingSuccess: StateFlow<Boolean> = _linkingSuccess.asStateFlow()

    private val _simulatedOtp = MutableStateFlow<String?>(null)
    val simulatedOtp: StateFlow<String?> = _simulatedOtp.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isBankLinked = MutableStateFlow(repository.isBankLinked())
    val isBankLinked: StateFlow<Boolean> = _isBankLinked.asStateFlow()

    private val _linkedBankName = MutableStateFlow(repository.getLinkedBankName())
    val linkedBankName: StateFlow<String> = _linkedBankName.asStateFlow()

    private val _linkedCardMasked = MutableStateFlow(repository.getLinkedCardMasked())
    val linkedCardMasked: StateFlow<String> = _linkedCardMasked.asStateFlow()

    init {
        checkLinkedStatus()
    }

    fun checkLinkedStatus() {
        _isBankLinked.value = repository.isBankLinked()
        _linkedBankName.value = repository.getLinkedBankName()
        _linkedCardMasked.value = repository.getLinkedCardMasked()
    }

    fun requestOtp(onOtpReady: (String) -> Unit) {
        viewModelScope.launch {
            _isVerifying.value = true
            _errorMessage.value = null
            repository.requestBankLinkingOtp()
                .onSuccess { otp ->
                    _simulatedOtp.value = otp
                    _errorMessage.value = null
                    onOtpReady(otp)
                }
                .onFailure {
                    _errorMessage.value = it.message
                }
            _isVerifying.value = false
        }
    }

    suspend fun isCardValid(cardNumber: String, expiry: String, cvv: String): Boolean {
        return repository.verifyCard(cardNumber, expiry, cvv)
    }

    fun verifyOtp(otp: String, bankName: String = "Bank Account", cardNumber: String = "4111111111111111") {
        viewModelScope.launch {
            _isVerifying.value = true
            _errorMessage.value = null
            repository.verifyOtp(otp, bankName, cardNumber)
                .onSuccess {
                    repository.saveLinkedBank(bankName, cardNumber)
                    checkLinkedStatus()
                    _linkingSuccess.value = true
                }
                .onFailure {
                    _errorMessage.value = it.message ?: "Verification failed"
                }
            _isVerifying.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
