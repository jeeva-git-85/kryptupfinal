package com.kryptup.kryptup.ui.screens.profile

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.kryptup.kryptup.ui.screens.security.PinSetupDialog
import com.kryptup.kryptup.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val walletAddress by viewModel.walletAddress.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var isPinSet by remember { mutableStateOf(viewModel.pinSecurityManager.isPinSet()) }
    var isBiometricEnabled by remember { mutableStateOf(viewModel.pinSecurityManager.isBiometricEnabled()) }

    val name = user?.displayName?.ifEmpty { "KryptUp Investor" } ?: "KryptUp Investor"
    val email = user?.email?.ifEmpty { "user@kryptup.com" } ?: "user@kryptup.com"
    val upiId = user?.upiId?.ifEmpty { "investor@kryptup" } ?: "investor@kryptup"
    val fiatBalance = user?.fiatBalanceInr ?: 0.00
    val points = user?.points ?: 0

    val upiQrContent = "upi://pay?pa=$upiId&pn=${name.replace(" ", "%20")}&cu=INR"
    val upiQrBitmap = remember(upiQrContent) {
        generateProfileQrCode(upiQrContent)
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            TopAppBar(
                title = { Text("Account Profile & QR ID", fontWeight = FontWeight.Bold) },
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
            // User Identity Header Card with Embedded Personal QR Code
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, LightBorder),
                colors = CardDefaults.cardColors(containerColor = LightSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SoftAqua),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(1).uppercase(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = SoftAquaText
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Personal UPI QR Code Card Frame
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        border = BorderStroke(1.5.dp, LightBorder)
                    ) {
                        Box(
                            modifier = Modifier.size(170.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (upiQrBitmap != null) {
                                Image(
                                    bitmap = upiQrBitmap.asImageBitmap(),
                                    contentDescription = "Personal UPI QR Code",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                )
                            } else {
                                CircularProgressIndicator(color = MintGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Personal UPI QR ($upiId)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Security & Transaction Security Section
            Text(
                text = "Transaction Security & Authentication",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            // Security Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = LightSurface,
                border = BorderStroke(1.dp, LightBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldGreen.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Transaction PIN", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = if (isPinSet) "PIN Configured" else "PIN Not Set",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPinSet) EmeraldGreen else CoralRed
                                )
                            }
                        }

                        Button(
                            onClick = { showPinSetupDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandCharcoal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isPinSet) "Change PIN" else "Set PIN", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = LightBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Facial Recognition / Biometrics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Use Face ID / Fingerprint for transactions", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }

                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                isBiometricEnabled = enabled
                                viewModel.pinSecurityManager.setBiometricEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                checkedTrackColor = EmeraldGreen
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account Financial Details Section
            Text(
                text = "Financial Accounts & Credentials",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            // UPI Detail Card
            ProfileDetailTile(
                title = "Assigned UPI ID",
                value = upiId,
                subtitle = "Available Balance: ₹${String.format("%.2f", fiatBalance)} INR",
                icon = Icons.Default.AccountBalance,
                accentColor = CyberCyan,
                onCopy = { clipboardManager.setText(AnnotatedString(upiId)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Crypto Vault Address Detail Card
            ProfileDetailTile(
                title = "Sepolia Crypto Vault",
                value = if (walletAddress.isNotEmpty()) "${walletAddress.take(12)}...${walletAddress.takeLast(8)}" else "0x...",
                subtitle = "Non-Custodial HD Wallet",
                icon = Icons.Default.Shield,
                accentColor = EmeraldGreen,
                onCopy = { if (walletAddress.isNotEmpty()) clipboardManager.setText(AnnotatedString(walletAddress)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Rewards Balance Card
            ProfileDetailTile(
                title = "Reward Balance",
                value = "$points Points",
                subtitle = "Gamification Status: Active",
                icon = Icons.Default.Star,
                accentColor = GoldAmber,
                onCopy = null
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = {
                    viewModel.logout()
                    onLogoutSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = androidx.compose.ui.graphics.Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of KryptUp", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            pinSecurityManager = viewModel.pinSecurityManager,
            onDismiss = { showPinSetupDialog = false },
            onPinSetSuccess = {
                showPinSetupDialog = false
                isPinSet = viewModel.pinSecurityManager.isPinSet()
                isBiometricEnabled = viewModel.pinSecurityManager.isBiometricEnabled()
            }
        )
    }
}

@Composable
fun ProfileDetailTile(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onCopy: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = LightSurface,
        border = BorderStroke(1.dp, LightBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            if (onCopy != null) {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun generateProfileQrCode(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
