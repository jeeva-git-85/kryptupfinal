package com.kryptup.kryptup.ui.screens.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kryptup.kryptup.ui.screens.security.PinSetupDialog
import com.kryptup.kryptup.ui.screens.security.PinVerificationDialog
import com.kryptup.kryptup.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    initialAddress: String? = null,
    onBackClick: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    var toAddress by remember { mutableStateOf(initialAddress ?: "") }
    var amount by remember { mutableStateOf("") }
    var selectedToken by remember { mutableStateOf(if (initialAddress?.contains("@") == true) "INR" else "ETH") }
    
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinVerificationDialog by remember { mutableStateOf(false) }

    val tokens by viewModel.tokens.collectAsState()
    val isLoading by viewModel.isSending.collectAsState()
    val status by viewModel.transactionStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Assets") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = toAddress,
                onValueChange = { toAddress = it },
                label = { Text(if (selectedToken == "INR") "Recipient UPI ID / Address" else "Recipient Crypto Address") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (selectedToken == "INR") "user@kryptup or 0x..." else "0x...") }
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(if (selectedToken == "INR") "Amount (₹ INR)" else "Amount ($selectedToken)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Text("Select Transfer Currency / Asset", style = MaterialTheme.typography.labelMedium)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("INR (₹ UPI)", "ETH", "USDT").forEach { assetLabel ->
                    val symbol = assetLabel.split(" ").first()
                    FilterChip(
                        selected = selectedToken == symbol,
                        onClick = { selectedToken = symbol },
                        label = { Text(assetLabel) }
                    )
                }
            }

            status?.let { st ->
                val isError = st.startsWith("Error")
                val isSuccess = st.startsWith("Success!")
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = if (isError) CoralRed.copy(alpha = 0.15f) else if (isSuccess) EmeraldGreen.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isError) CoralRed else if (isSuccess) EmeraldGreen else CyberCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = st,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) CoralRed else if (isSuccess) EmeraldGreen else CyberCyan,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    if (!viewModel.pinSecurityManager.isPinSet()) {
                        showPinSetupDialog = true
                    } else {
                        showPinVerificationDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = toAddress.isNotEmpty() && amount.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirm Send")
                }
            }
        }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            pinSecurityManager = viewModel.pinSecurityManager,
            onDismiss = { showPinSetupDialog = false },
            onPinSetSuccess = {
                showPinSetupDialog = false
                showPinVerificationDialog = true
            }
        )
    }

    if (showPinVerificationDialog) {
        PinVerificationDialog(
            title = "Authorize Send Transaction",
            transactionDetails = "Sending $amount $selectedToken to ${toAddress.take(8)}...",
            pinSecurityManager = viewModel.pinSecurityManager,
            onDismiss = { showPinVerificationDialog = false },
            onPinVerified = {
                showPinVerificationDialog = false
                viewModel.sendTransaction(toAddress, amount, selectedToken)
            }
        )
    }
}
