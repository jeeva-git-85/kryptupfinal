package com.kryptup.kryptup

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.kryptup.kryptup.security.BiometricPromptManager
import com.kryptup.kryptup.security.PinSecurityManager
import com.kryptup.kryptup.ui.navigation.SetupNavGraph
import com.kryptup.kryptup.ui.screens.security.PinVerificationDialog
import com.kryptup.kryptup.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var pinSecurityManager: PinSecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent screen capture / task snapshot data leak of sensitive wallet keys
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        enableHighRefreshRate()
        
        val biometricPromptManager = BiometricPromptManager(this)

        setContent {
            KryptupTheme {
                var isAuthenticated by remember { mutableStateOf(false) }
                var showPinFallback by remember { mutableStateOf(false) }
                val canBiometric = remember { biometricPromptManager.canAuthenticate() }
                val isPinSet = remember { pinSecurityManager.isPinSet() }

                fun triggerBiometrics() {
                    biometricPromptManager.showBiometricPrompt(
                        title = "KryptUp Secure Entry",
                        subtitle = "Authenticate to access your wallet",
                        onSuccess = { 
                            isAuthenticated = true
                            showPinFallback = false
                        },
                        onError = { _ ->
                            // Never auto-authenticate on cancel/failure. Stay locked!
                            isAuthenticated = false
                        }
                    )
                }
                
                LaunchedEffect(Unit) {
                    if (canBiometric) {
                        triggerBiometrics()
                    } else if (!isPinSet) {
                        // First-time install on a device with zero biometrics enrolled
                        isAuthenticated = true
                    } else {
                        showPinFallback = true
                    }
                }

                if (showPinFallback && !isAuthenticated) {
                    PinVerificationDialog(
                        title = "Enter Transaction PIN",
                        transactionDetails = "Verify your 4-digit PIN to unlock KryptUp",
                        pinSecurityManager = pinSecurityManager,
                        onDismiss = { showPinFallback = false },
                        onPinVerified = {
                            showPinFallback = false
                            isAuthenticated = true
                        }
                    )
                }

                if (isAuthenticated) {
                    val navController = rememberNavController()
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            SetupNavGraph(navController = navController)
                        }
                    }
                } else if (!showPinFallback) {
                    // Secure Lock Screen: Requires explicit user action to unlock
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "KryptUp Locked",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Biometric or PIN verification required to access your financial vault.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        if (canBiometric) {
                            Button(
                                onClick = { triggerBiometrics() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock with Biometrics", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isPinSet) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showPinFallback = true },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text("Unlock with PIN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val currentDisplay = display
            if (currentDisplay != null) {
                val supportedModes = currentDisplay.supportedModes
                // Find mode with highest refresh rate (e.g. 120Hz / 144Hz)
                val maxMode = supportedModes.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val layoutParams = window.attributes
                    layoutParams.preferredDisplayModeId = maxMode.modeId
                    window.attributes = layoutParams
                }
            }
        }
    }
}
