package com.kryptup.kryptup.ui.screens.banking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kryptup.kryptup.ui.screens.security.PinSetupDialog
import com.kryptup.kryptup.ui.screens.security.PinVerificationDialog
import com.kryptup.kryptup.ui.theme.*

data class BankOption(val name: String, val code: String, val color: Color)

val popularBanks = listOf(
    BankOption("HDFC Bank", "HDFC", CyberCyan),
    BankOption("State Bank of India", "SBI", EmeraldGreen),
    BankOption("ICICI Bank", "ICICI", CoralRed),
    BankOption("Axis Bank", "AXIS", ElectricViolet),
    BankOption("Chase Bank", "CHASE", CyberCyan)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankLinkingScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: BankingViewModel = hiltViewModel()
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var showOtpDialog by remember { mutableStateOf(false) }
    var selectedInstantBank by remember { mutableStateOf<BankOption?>(null) }
    
    var pendingOtp by remember { mutableStateOf<String?>(null) }
    var pendingBankName by remember { mutableStateOf<String?>(null) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinVerificationDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val isVerifying by viewModel.isVerifying.collectAsState()
    val linkingSuccess by viewModel.linkingSuccess.collectAsState()
    val simulatedOtp by viewModel.simulatedOtp.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val isBankLinked by viewModel.isBankLinked.collectAsState()
    val linkedBankName by viewModel.linkedBankName.collectAsState()
    val linkedCardMasked by viewModel.linkedCardMasked.collectAsState()

    LaunchedEffect(linkingSuccess) {
        if (linkingSuccess) {
            onSuccess()
        }
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            TopAppBar(
                title = { Text("Link Bank Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isBankLinked) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.12f)),
                    border = BorderStroke(1.5.dp, EmeraldGreen),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bank Account Linked", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldGreen,
                                contentColor = Color.White
                            ) {
                                Text("PRIMARY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = linkedBankName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = linkedCardMasked, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bank account assigned to your profile. Further bank account linking is disabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // 1-Tap Popular Bank Direct Selector Header
            Text(
                text = "1-Tap Direct Bank Linking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = if (isBankLinked) "Bank account already linked for this user." else "Select your bank to link instantly without typing card numbers",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(popularBanks) { bank ->
                    val isSelected = selectedInstantBank == bank
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) bank.color.copy(alpha = 0.15f) else LightSurface,
                        border = BorderStroke(1.5.dp, if (isSelected) bank.color else LightBorder),
                        modifier = Modifier
                            .width(130.dp)
                            .clickable { selectedInstantBank = bank }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = bank.color.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = bank.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = bank.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (selectedInstantBank != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { 
                        pendingBankName = selectedInstantBank!!.name
                        viewModel.requestOtp { 
                            showOtpDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = selectedInstantBank!!.color, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link ${selectedInstantBank!!.name} Instantly", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "OR Link via Debit/Credit Card",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Quick Auto-Fill Demo Button with Luhn-compliant test number
                TextButton(
                    onClick = {
                        cardNumber = "4111111111111111"
                        expiryDate = "1228"
                        cvv = "888"
                    }
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Demo Auto-Fill", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { if (it.length <= 16) cardNumber = it },
                label = { Text("16-Digit Card Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = CreditCardTransformation(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = LightBorder,
                    focusedContainerColor = LightSurface,
                    unfocusedContainerColor = LightSurface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { if (it.length <= 4) expiryDate = it },
                    label = { Text("Expiry (MM/YY)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ExpiryDateTransformation(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = LightBorder,
                        focusedContainerColor = LightSurface,
                        unfocusedContainerColor = LightSurface
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = cvv,
                    onValueChange = { if (it.length <= 3) cvv = it },
                    label = { Text("CVV") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = LightBorder,
                        focusedContainerColor = LightSurface,
                        unfocusedContainerColor = LightSurface
                    )
                )
            }

            if (!errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CoralRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CoralRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = CoralRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    pendingBankName = "Card Ending in ${cardNumber.takeLast(4)}"
                    viewModel.requestOtp { 
                        showOtpDialog = true 
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isBankLinked && cardNumber.length == 16 && expiryDate.length == 4 && cvv.length == 3 && !isVerifying,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandCharcoal, contentColor = Color.White)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else if (isBankLinked) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bank Account Already Linked", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify & Link Card", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showOtpDialog) {
        OtpVerificationDialog(
            simulatedOtp = simulatedOtp,
            onDismiss = { showOtpDialog = false },
            onConfirm = { otp ->
                showOtpDialog = false
                pendingOtp = otp
                if (!viewModel.pinSecurityManager.isPinSet()) {
                    showPinSetupDialog = true
                } else {
                    showPinVerificationDialog = true
                }
            }
        )
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
            title = "Authorize Bank Account Link",
            transactionDetails = "Link ${pendingBankName ?: "Bank Account"} to KryptUp",
            pinSecurityManager = viewModel.pinSecurityManager,
            onDismiss = { showPinVerificationDialog = false },
            onPinVerified = {
                showPinVerificationDialog = false
                pendingOtp?.let { 
                    viewModel.verifyOtp(
                        otp = it,
                        bankName = pendingBankName ?: "HDFC Bank",
                        cardNumber = cardNumber.ifEmpty { "4111111111111111" }
                    ) 
                }
            }
        )
    }
}

@Composable
fun OtpVerificationDialog(
    simulatedOtp: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var otp by remember { mutableStateOf(simulatedOtp ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("2FA Bank Verification", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Enter the 6-digit OTP sent to your bank registered mobile number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                if (!simulatedOtp.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simulated SMS OTP: $simulatedOtp",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it },
                    label = { Text("6-Digit OTP") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(otp) },
                enabled = otp.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("Verify OTP")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

class CreditCardTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 16) text.text.substring(0..15) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 15) out += " "
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset + 1
                if (offset <= 11) return offset + 2
                if (offset <= 16) return offset + 3
                return 19
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                if (offset <= 19) return offset - 3
                return 16
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

class ExpiryDateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 4) text.text.substring(0..3) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1) out += "/"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 4) return offset + 1
                return 5
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                return 4
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
