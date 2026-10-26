package com.kryptup.kryptup.ui.screens.security

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kryptup.kryptup.security.BiometricPaymentSecurityManager
import com.kryptup.kryptup.security.BiometricStatus
import com.kryptup.kryptup.security.PinSecurityManager
import com.kryptup.kryptup.ui.theme.*

@Composable
fun PinVerificationDialog(
    title: String,
    transactionDetails: String,
    pinSecurityManager: PinSecurityManager,
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFaceScanningActive by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = remember(context) { context as? FragmentActivity }

    val biometricPaymentSecurityManager = remember(context) {
        BiometricPaymentSecurityManager(context.applicationContext)
    }

    val triggerHardwareBiometricPrompt: () -> Unit = {
        if (activity != null && pinSecurityManager.isBiometricEnabled()) {
            val status = biometricPaymentSecurityManager.canAuthenticateStrong()
            if (status == BiometricStatus.SUCCESS) {
                try {
                    val cryptoObject = biometricPaymentSecurityManager.createCryptoObject()
                    if (cryptoObject != null) {
                        isFaceScanningActive = true
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Face ID / Biometric Payment Authorization")
                            .setSubtitle(transactionDetails)
                            .setNegativeButtonText("Use PIN Instead")
                            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            .build()

                        val executor = ContextCompat.getMainExecutor(activity)
                        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                isFaceScanningActive = false
                                val cipher = result.cryptoObject?.cipher
                                if (cipher != null) {
                                    // Encrypt transaction payload using unlocked hardware CryptoObject cipher
                                    val mockPayload = "TX_PAYLOAD:$transactionDetails".toByteArray()
                                    biometricPaymentSecurityManager.encryptTransactionPayload(cipher, mockPayload)
                                }
                                onPinVerified()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                isFaceScanningActive = false
                                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                    errorMessage = errString.toString()
                                }
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                isFaceScanningActive = false
                                errorMessage = "Biometric not recognized. Try again or enter PIN."
                            }
                        })

                        biometricPrompt.authenticate(promptInfo, cryptoObject)
                    } else {
                        errorMessage = "Keystore key error. Enter PIN to authorize."
                    }
                } catch (e: KeyPermanentlyInvalidatedException) {
                    biometricPaymentSecurityManager.resetKeyStoreKey()
                    errorMessage = "New biometric enrolled in OS settings. Key reset. Please enter PIN."
                } catch (e: Exception) {
                    errorMessage = "Hardware biometric unavailable. Enter PIN."
                }
            } else {
                errorMessage = when (status) {
                    BiometricStatus.NO_HARDWARE -> "Device has no biometric hardware. Use PIN."
                    BiometricStatus.NONE_ENROLLED -> "No face/fingerprint enrolled in OS settings. Use PIN."
                    else -> "Biometrics unavailable. Enter PIN."
                }
            }
        }
    }

    // Auto trigger hardware BiometricPrompt on launch if enabled
    LaunchedEffect(Unit) {
        triggerHardwareBiometricPrompt()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = LightBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Round Front-Camera Face Scanner Preview Overlay Window
                CameraFaceScannerView(sizeDp = 130)

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LightSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = transactionDetails,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isFaceScanningActive) "Scanning face via camera..." else "Enter 4-digit PIN or tap Face ID",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isFaceScanningActive) EmeraldGreen else TextMuted,
                    fontWeight = if (isFaceScanningActive) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(12.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) EmeraldGreen else Color.Transparent)
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) EmeraldGreen else TextMuted,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = CoralRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // On-screen Numeric Keypad with Face ID button on left
                NumericKeypad(
                    onDigitClick = { digit ->
                        if (enteredPin.length < 4) {
                            errorMessage = null
                            val newPin = enteredPin + digit
                            enteredPin = newPin
                            if (newPin.length == 4) {
                                if (pinSecurityManager.isLockedOut()) {
                                    val remaining = pinSecurityManager.getRemainingLockoutSeconds()
                                    errorMessage = "Locked out for $remaining seconds."
                                    enteredPin = ""
                                } else if (pinSecurityManager.verifyPin(newPin)) {
                                    onPinVerified()
                                } else {
                                    if (pinSecurityManager.isLockedOut()) {
                                        val remaining = pinSecurityManager.getRemainingLockoutSeconds()
                                        errorMessage = "5 failed attempts. Locked out for $remaining seconds."
                                    } else {
                                        errorMessage = "Incorrect PIN. Please try again."
                                    }
                                    enteredPin = ""
                                }
                            }
                        }
                    },
                    onDeleteClick = {
                        errorMessage = null
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    },
                    onLeftActionClick = {
                        triggerHardwareBiometricPrompt()
                    },
                    leftActionIcon = {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Trigger Face ID",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel Transaction", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
