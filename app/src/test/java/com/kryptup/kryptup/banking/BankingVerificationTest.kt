package com.kryptup.kryptup.banking

import org.junit.Assert.*
import org.junit.Test

class BankingVerificationTest {

    private fun isCardValid(cardNumber: String): Boolean {
        val cleanNumber = cardNumber.replace("\\s+".toRegex(), "")
        return cleanNumber.length == 16 && cleanNumber.all { it.isDigit() }
    }

    private fun isOtpValid(otp: String): Boolean {
        return otp.trim() == "123456"
    }

    private fun validateUpiTransfer(
        senderUid: String,
        recipientUid: String,
        senderBalance: Double,
        transferAmount: Double
    ): Result<Unit> {
        if (senderUid == recipientUid) {
            return Result.failure(IllegalArgumentException("Cannot send money to your own UPI ID"))
        }
        if (transferAmount <= 0) {
            return Result.failure(IllegalArgumentException("Transfer amount must be greater than zero"))
        }
        if (senderBalance < transferAmount) {
            return Result.failure(IllegalStateException("Insufficient balance"))
        }
        return Result.success(Unit)
    }

    @Test
    fun isCardValid_validSixteenDigitCard_returnsTrue() {
        assertTrue(isCardValid("4111 2222 3333 4444"))
        assertTrue(isCardValid("4111222233334444"))
    }

    @Test
    fun isCardValid_shortOrAlphabetCard_returnsFalse() {
        assertFalse(isCardValid("4111 2222 3333"))
        assertFalse(isCardValid("4111 2222 3333 ABCD"))
    }

    @Test
    fun isOtpValid_correctSimulationOtp_returnsTrue() {
        assertTrue(isOtpValid("123456"))
        assertTrue(isOtpValid(" 123456 "))
    }

    @Test
    fun isOtpValid_wrongOtp_returnsFalse() {
        assertFalse(isOtpValid("000000"))
        assertFalse(isOtpValid("12345"))
    }

    @Test
    fun validateUpiTransfer_sufficientBalanceDifferentUser_succeeds() {
        val result = validateUpiTransfer(
            senderUid = "user_1",
            recipientUid = "user_2",
            senderBalance = 5000.0,
            transferAmount = 1500.0
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun validateUpiTransfer_selfTransfer_fails() {
        val result = validateUpiTransfer(
            senderUid = "user_1",
            recipientUid = "user_1",
            senderBalance = 5000.0,
            transferAmount = 500.0
        )
        assertTrue(result.isFailure)
        assertEquals("Cannot send money to your own UPI ID", result.exceptionOrNull()?.message)
    }

    @Test
    fun validateUpiTransfer_insufficientBalance_fails() {
        val result = validateUpiTransfer(
            senderUid = "user_1",
            recipientUid = "user_2",
            senderBalance = 100.0,
            transferAmount = 500.0
        )
        assertTrue(result.isFailure)
        assertEquals("Insufficient balance", result.exceptionOrNull()?.message)
    }
}
