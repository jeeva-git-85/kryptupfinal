package com.kryptup.kryptup.ui.screens.home

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.kryptup.kryptup.BuildConfig
import com.kryptup.kryptup.data.model.TransactionRecord
import com.kryptup.kryptup.data.model.TransactionType
import com.kryptup.kryptup.ui.theme.*
import com.kryptup.kryptup.voice.AgentAction
import com.kryptup.kryptup.voice.FinancialNlpEngine
import com.kryptup.kryptup.voice.GeminiLiveWebSocketManager
import com.kryptup.kryptup.voice.GeminiManager
import com.kryptup.kryptup.voice.TextToSpeechManager
import com.kryptup.kryptup.voice.VoiceAssistantManager
import com.kryptup.kryptup.voice.VoiceIntent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimeframeData(
    val changeText: String,
    val isPositive: Boolean,
    val points: List<Float>,
    val strokeColor: Color
)

fun computeTimeframeData(
    transactions: List<com.kryptup.kryptup.data.model.TransactionRecord>,
    currentBalance: Double,
    selectedTimeframe: String
): TimeframeData {
    val now = System.currentTimeMillis()
    val timeframeMs = when (selectedTimeframe) {
        "1D" -> 24 * 60 * 60 * 1000L
        "1W" -> 7 * 24 * 60 * 60 * 1000L
        "1M" -> 30L * 24 * 60 * 60 * 1000L
        "1Y" -> 365L * 24 * 60 * 60 * 1000L
        else -> 365L * 24 * 60 * 60 * 1000L * 5
    }

    val startTime = now - timeframeMs
    val numPoints = 7
    val rawPoints = MutableList(numPoints) { 0.0 }

    val filteredTx = transactions.filter { it.timestamp in startTime..now }

    if (filteredTx.isNotEmpty()) {
        for (i in 0 until numPoints) {
            val t = startTime + (timeframeMs * i / (numPoints - 1))
            val txAfterT = transactions.filter { it.timestamp > t }
            var netChangeAfterT = 0.0
            for (tx in txAfterT) {
                if (!tx.isDebit) {
                    netChangeAfterT += tx.amountValue
                } else {
                    netChangeAfterT -= tx.amountValue
                }
            }
            rawPoints[i] = (currentBalance - netChangeAfterT).coerceAtLeast(0.0)
        }
    } else {
        // Varying portfolio curve per timeframe (1D, 1W, 1M, 1Y, All)
        val base = currentBalance.coerceAtLeast(100.0)
        val variationFactor = when (selectedTimeframe) {
            "1D" -> listOf(0.92, 0.94, 0.93, 0.97, 0.95, 0.99, 1.00)
            "1W" -> listOf(0.85, 0.88, 0.86, 0.92, 0.90, 0.96, 1.00)
            "1M" -> listOf(0.75, 0.80, 0.78, 0.88, 0.84, 0.93, 1.00)
            "1Y" -> listOf(0.60, 0.68, 0.65, 0.79, 0.74, 0.90, 1.00)
            else -> listOf(0.50, 0.60, 0.58, 0.72, 0.70, 0.88, 1.00)
        }
        for (i in 0 until numPoints) {
            rawPoints[i] = if (currentBalance > 0) base * variationFactor[i] else 0.0
        }
    }

    val startBal = rawPoints.first()
    val endBal = rawPoints.last()
    val diff = endBal - startBal
    val pct = if (startBal > 0) (diff / startBal) * 100.0 else if (endBal > 0) 100.0 else 0.0

    val minVal = rawPoints.minOrNull() ?: 0.0
    val maxVal = rawPoints.maxOrNull() ?: 1.0
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    val normalizedPoints = rawPoints.map { valVal ->
        (0.15f + ((valVal - minVal) / range) * 0.70f).toFloat()
    }

    val isPos = diff >= 0
    val sign = if (isPos) "+" else "-"
    val changeText = "$sign₹${String.format(Locale.US, "%.2f", kotlin.math.abs(diff))} ($sign${String.format(Locale.US, "%.2f", kotlin.math.abs(pct))}%)"
    val color = if (isPos) EmeraldGreen else CoralRed

    return TimeframeData(changeText, isPos, normalizedPoints, color)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBankLinking: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToTrading: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToRewards: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val userFirstName by viewModel.userFirstName.collectAsState()
    val fiatBalanceInr by viewModel.fiatBalanceInr.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val totalMonthlySpentUsd by viewModel.totalMonthlySpentUsd.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    val prefs = remember { context.getSharedPreferences("secret_shared_prefs", android.content.Context.MODE_PRIVATE) }
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("app_dark_theme_enabled", isSystemDark)) }
    var selectedLanguage by remember { mutableStateOf("English") }

    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    var selectedTxFilter by remember { mutableStateOf("All") }
    var selectedTxDetail by remember { mutableStateOf<TransactionRecord?>(null) }

    var isListening by remember { mutableStateOf(false) }
    var assistantResponse by remember { mutableStateOf("") }
    var isBalanceHidden by remember { mutableStateOf(false) }
    var selectedTimeframe by remember { mutableStateOf("1D") }
    var selectedCurrency by remember { mutableStateOf("USD") }

    val balanceUsd = fiatBalanceInr / 83.50

    val ttsManager = remember { TextToSpeechManager(context) }
    val nlpEngine = remember { FinancialNlpEngine() }
    val geminiManager = remember { GeminiManager() }
    val coroutineScope = rememberCoroutineScope()

    val voiceManager = remember {
        VoiceAssistantManager(
            context = context,
            onResult = { text ->
                isListening = false
                val intent = nlpEngine.parseCommand(text)
                when (intent) {
                    is VoiceIntent.CheckBalance -> {
                        assistantResponse = "Your current portfolio balance is $${String.format("%.2f", balanceUsd)} USD."
                        ttsManager.speak(assistantResponse)
                    }
                    is VoiceIntent.CheckPrice -> {
                        val currentMarketPrice = when (intent.symbol.uppercase()) {
                            "BTC", "BITCOIN" -> "$94,250.00 USD"
                            "ETH", "ETHEREUM" -> "$2,500.00 USD"
                            "USDT", "TETHER" -> "$1.00 USD"
                            "SOL", "SOLANA" -> "$185.50 USD"
                            "ADA", "CARDANO" -> "$0.85 USD"
                            "DOGE", "DOGECOIN" -> "$0.38 USD"
                            "MATIC", "POLYGON" -> "$0.62 USD"
                            "XRP", "RIPPLE" -> "$1.45 USD"
                            else -> "$2,500.00 USD"
                        }
                        assistantResponse = "The current market price of ${intent.symbol} is $currentMarketPrice."
                        ttsManager.speak(assistantResponse)
                    }
                    is VoiceIntent.SendCrypto -> {
                        assistantResponse = "Parsed transfer command: ${intent.amount} ${intent.symbol} to ${intent.recipient}."
                        ttsManager.speak(assistantResponse)
                        onNavigateToWallet()
                    }
                    is VoiceIntent.NavigateScreen -> {
                        val msg = "Opening ${intent.screen}..."
                        assistantResponse = msg
                        ttsManager.speak(msg)
                        when (intent.screen) {
                            "wallet" -> onNavigateToWallet()
                            "bank" -> onNavigateToBankLinking()
                            "trading" -> onNavigateToTrading()
                            "scanner" -> onNavigateToScanner()
                            "chat" -> onNavigateToChat()
                            "rewards" -> onNavigateToRewards()
                        }
                    }
                    is VoiceIntent.Unknown -> {
                        assistantResponse = "Agent processing..."
                        coroutineScope.launch {
                            val action = geminiManager.executeAgentRequest(text)
                            when (action) {
                                is AgentAction.TextResponse -> {
                                    assistantResponse = action.text
                                    ttsManager.speak(action.text)
                                }
                                is AgentAction.NavigateScreen -> {
                                    assistantResponse = action.message
                                    ttsManager.speak(action.message)
                                    when (action.screen.lowercase()) {
                                        "wallet", "vault" -> onNavigateToWallet()
                                        "bank", "linking" -> onNavigateToBankLinking()
                                        "trading", "trade" -> onNavigateToTrading()
                                        "scanner", "scan" -> onNavigateToScanner()
                                        "chat" -> onNavigateToChat()
                                        "rewards", "reward" -> onNavigateToRewards()
                                    }
                                }
                                is AgentAction.SendCrypto -> {
                                    assistantResponse = action.message
                                    ttsManager.speak(action.message)
                                    onNavigateToWallet()
                                }
                            }
                        }
                    }
                }
            },
            onError = { error ->
                isListening = false
                assistantResponse = "Voice AI: $error"
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            assistantResponse = "Listening..."
            isListening = true
            voiceManager.startListening()
        } else {
            assistantResponse = "Microphone permission is required for Voice AI."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
            voiceManager.destroy()
        }
    }

    val filteredTransactions = remember(transactions, selectedTxFilter) {
        if (selectedTxFilter == "All") {
            transactions
        } else {
            transactions.filter { it.category.equals(selectedTxFilter, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = LightBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isListening) {
                        voiceManager.stopListening()
                        isListening = false
                    } else {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            assistantResponse = "Listening..."
                            isListening = true
                            voiceManager.startListening()
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                containerColor = if (isListening) CoralRed else BrandCharcoal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = "Voice Assistant",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. TopBar Profile & Notification Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateToProfile() }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SoftAqua),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userFirstName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SoftAquaText
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MintGreen)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WELCOME BACK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.2.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userFirstName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = LightSurfaceVariant,
                            border = BorderStroke(1.dp, LightBorder),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.clickable {
                                showSettingsSheet = true
                            }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = LightSurfaceVariant,
                                border = BorderStroke(1.dp, LightBorder),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.clickable {
                                showNotificationsSheet = true
                                viewModel.markNotificationsRead()
                            }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = LightSurfaceVariant,
                                border = BorderStroke(1.dp, LightBorder),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (unreadCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFE03137),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(
                                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Voice AI Banner
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFD1FAE5),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MintGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Voice AI Active",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                }
                                Text(
                                    text = assistantResponse.ifEmpty { "\"Check BTC price\" or \"Send $50 USDT\"" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isListening) {
                                    voiceManager.stopListening()
                                    isListening = false
                                } else {
                                    assistantResponse = "Listening..."
                                    isListening = true
                                    voiceManager.startListening()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandCharcoal, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Talk", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Total Portfolio Net Worth
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TOTAL PORTFOLIO NET WORTH",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { isBalanceHidden = !isBalanceHidden },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Balance",
                                    tint = TextMuted
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF3F4F6),
                                border = BorderStroke(1.dp, LightBorder),
                                modifier = Modifier.clickable {
                                    selectedCurrency = if (selectedCurrency == "USD") "INR" else "USD"
                                }
                            ) {
                                Text(
                                    text = if (selectedCurrency == "USD") "$ USD" else "₹ INR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MintGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldGreen,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val displayBalance = if (isBalanceHidden) {
                        "••••••••"
                    } else if (selectedCurrency == "USD") {
                        "$${String.format("%.2f", balanceUsd)}"
                    } else {
                        "₹${String.format("%.2f", fiatBalanceInr)}"
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = displayBalance,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedCurrency,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val timeframeData = remember(transactions, fiatBalanceInr, selectedTimeframe) {
                        computeTimeframeData(transactions, fiatBalanceInr, selectedTimeframe)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (timeframeData.isPositive) Icons.Default.NorthEast else Icons.Default.SouthEast,
                            contentDescription = null,
                            tint = timeframeData.strokeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = timeframeData.changeText,
                            style = MaterialTheme.typography.labelLarge,
                            color = timeframeData.strokeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedTimeframe,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        val w = size.width
                        val h = size.height

                        val path = Path()
                        timeframeData.points.forEachIndexed { index, value ->
                            val x = (index.toFloat() / (timeframeData.points.size - 1)) * w
                            val y = h - (value * h)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = timeframeData.strokeColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        val lastX = w
                        val lastY = h - (timeframeData.points.last() * h)
                        drawCircle(
                            color = timeframeData.strokeColor,
                            radius = 6.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(lastX, lastY)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("1D", "1W", "1M", "1Y", "All").forEach { tf ->
                            val isActive = selectedTimeframe == tf
                            Surface(
                                shape = CircleShape,
                                color = if (isActive) Color(0xFFF2F4F7) else Color.Transparent,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { selectedTimeframe = tf }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tf,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isActive) TextPrimary else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickActionButtonSpec(
                        icon = Icons.Default.AccountBalance,
                        label = "Add funds",
                        isPrimary = true,
                        onClick = onNavigateToBankLinking
                    )
                    QuickActionButtonSpec(
                        icon = Icons.Default.QrCodeScanner,
                        label = "Pay",
                        onClick = onNavigateToScanner
                    )
                    QuickActionButtonSpec(
                        icon = Icons.Default.Add,
                        label = "Buy",
                        onClick = onNavigateToTrading
                    )
                    QuickActionButtonSpec(
                        icon = Icons.Default.NorthEast,
                        label = "Send",
                        onClick = onNavigateToWallet
                    )
                    QuickActionButtonSpec(
                        icon = Icons.Default.MoreHoriz,
                        label = "More",
                        onClick = onNavigateToChat
                    )
                }
            }

            // 5. Complete Real-Time Spending Analytics & Transaction History Feed
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Spending & Transaction Activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            val outflowText = if (selectedCurrency == "USD") {
                                "Total Outflow: $${String.format("%.2f", totalMonthlySpentUsd / 83.50)} USD"
                            } else {
                                "Total Outflow: ₹${String.format("%.2f", totalMonthlySpentUsd)} INR"
                            }
                            Text(
                                text = outflowText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LightSurfaceVariant,
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Text(
                                text = "${transactions.size} Records",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val categories = listOf("All", "Transfer", "Trading", "Banking", "Rewards")
                        items(categories) { category ->
                            val isSelected = selectedTxFilter == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTxFilter = category },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandCharcoal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredTransactions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = LightSurface,
                            border = BorderStroke(1.dp, LightBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transaction records found for '$selectedTxFilter'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredTransactions.take(8).forEach { tx ->
                                TransactionRecordTile(
                                    record = tx,
                                    onClick = { selectedTxDetail = tx }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Market Watchlist Ticker
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Market Watchlist",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "See all >",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToTrading() }
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(sampleWatchlist) { item ->
                        WatchlistCardLight(item, selectedCurrency)
                    }
                }
            }



            // 8. Super-App Hub Grid Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Super-App Hub",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Integrated DeFi, Fiat & Social Suite",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF2F4F7),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Text(
                            text = "v2.4 Ready",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "Link Bank",
                            subtitle = "Fiat Cards & Instant OTP",
                            icon = Icons.Default.CreditCard,
                            accentColor = CyberCyan,
                            onClick = onNavigateToBankLinking
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "Crypto Vault",
                            subtitle = "MPC Multi-Sig Security",
                            icon = Icons.Default.Shield,
                            accentColor = MintGreen,
                            onClick = onNavigateToWallet
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "Spot Trading",
                            subtitle = "Live Orderbooks & Depth",
                            icon = Icons.Default.BarChart,
                            accentColor = ElectricViolet,
                            onClick = onNavigateToTrading
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "P2P Scanner",
                            subtitle = "Instant Barcode Pay",
                            icon = Icons.Default.QrCodeScanner,
                            accentColor = CyberCyan,
                            onClick = onNavigateToScanner
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "Social Chat",
                            subtitle = "Pay Inside DM Threads",
                            icon = Icons.Default.Chat,
                            accentColor = MintGreen,
                            onClick = onNavigateToChat
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SuperAppCardLight(
                            title = "Rewards Hub",
                            subtitle = "Claim Daily APY Booster",
                            icon = Icons.Default.EmojiEvents,
                            accentColor = GoldAmber,
                            onClick = onNavigateToRewards
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

        if (showNotificationsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNotificationsSheet = false },
                containerColor = LightSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Message Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        TextButton(onClick = { showNotificationsSheet = false }) {
                            Text("Close", color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No message notifications yet.", color = TextMuted)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            items(notifications) { notif ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = LightSurfaceVariant,
                                    border = BorderStroke(1.dp, LightBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showNotificationsSheet = false
                                            onNavigateToChat()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldGreen.copy(alpha = 0.15f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Chat,
                                                    contentDescription = null,
                                                    tint = EmeraldGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "New Message from ${notif.senderName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = notif.messageText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transaction Detail Modal BottomSheet
        selectedTxDetail?.let { record ->
            ModalBottomSheet(
                onDismissRequest = { selectedTxDetail = null },
                containerColor = LightSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transaction Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = { selectedTxDetail = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = record.amountDisplay,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (record.isDebit) CoralRed else EmeraldGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = record.status,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LightSurfaceVariant,
                        border = BorderStroke(1.dp, LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Title", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(record.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            HorizontalDivider(color = LightBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Category", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(record.category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            HorizontalDivider(color = LightBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Description", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(record.description, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            }
                            HorizontalDivider(color = LightBorder)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Timestamp", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(
                                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(record.timestamp)),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }
                            HorizontalDivider(color = LightBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reference ID", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        record.referenceId,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    IconButton(
                                        onClick = { clipboardManager.setText(AnnotatedString(record.referenceId)) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        if (showSettingsSheet) {
            SettingsModalPanel(
                onDismiss = { showSettingsSheet = false },
                isDarkMode = isDarkMode,
                onDarkModeToggle = { enabled ->
                    isDarkMode = enabled
                    prefs.edit().putBoolean("app_dark_theme_enabled", enabled).apply()
                },
                selectedLanguage = selectedLanguage,
                onLanguageChange = { selectedLanguage = it },
                onOpenTerms = { showTermsDialog = true },
                onOpenPrivacy = { showPrivacyDialog = true },
                onOpenLicenses = { showLicensesDialog = true },
                onOpenHelp = { showHelpDialog = true },
                onOpenFeedback = { showFeedbackDialog = true }
            )
        }

        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Welcome to KryptUp Super-App.", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("1. Acceptance of Terms: By accessing or using KryptUp, you agree to comply with and be bound by these Terms of Service.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("2. Non-Custodial Asset Management: Private keys and wallet mnemonic phrases are stored securely on your local device via Android Keystore. You are solely responsible for maintaining your seed phrase backups.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("3. Simulated P2P Transfers & Trading: P2P Rupee transfers and spot trading desk simulated orders are provided for financial management and testing.")
                    }
                },
                confirmButton = {
                    Button(onClick = { showTermsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = BrandCharcoal)) {
                        Text("I Agree")
                    }
                }
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("KryptUp Privacy Protection Commitment", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("1. Data Encryption: Sensitive wallet keys are protected with hardware-backed AES-256-GCM encryption.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("2. Biometric Security: Fingerprint and facial authentication processing is handled locally by Android's BiometricPrompt framework.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("3. No Third-Party Tracking: KryptUp does not sell or share personal transaction records with external advertising networks.")
                    }
                },
                confirmButton = {
                    Button(onClick = { showPrivacyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = BrandCharcoal)) {
                        Text("Close")
                    }
                }
            )
        }

        if (showLicensesDialog) {
            AlertDialog(
                onDismissRequest = { showLicensesDialog = false },
                title = { Text("Software Licenses", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Open Source Software & Attributions", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("• Web3j Core (Apache-2.0 License)")
                        Text("• Jetpack Compose & Material 3 (Apache-2.0)")
                        Text("• Firebase Android SDK (Apache-2.0)")
                        Text("• MPAndroidChart (Apache-2.0)")
                        Text("• ML Kit Barcode Scanning (Apache-2.0)")
                        Text("• Kotlin Coroutines & Flow (Apache-2.0)")
                    }
                },
                confirmButton = {
                    Button(onClick = { showLicensesDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = BrandCharcoal)) {
                        Text("OK")
                    }
                }
            )
        }

        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text("Help & FAQ Guide", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Frequently Asked Questions", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Q: How do I link my bank account?", fontWeight = FontWeight.Bold)
                        Text("A: Tap 'Link Bank Account' on the home dashboard, choose your bank or demo card, and enter the OTP.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Q: How do I send money via QR code?", fontWeight = FontWeight.Bold)
                        Text("A: Tap the Scanner icon, point at the recipient's QR code, and confirm with your 4-digit PIN.")
                    }
                },
                confirmButton = {
                    Button(onClick = { showHelpDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = BrandCharcoal)) {
                        Text("Got It")
                    }
                }
            )
        }

        if (showFeedbackDialog) {
            var feedbackText by remember { mutableStateOf("") }
            var feedbackSent by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showFeedbackDialog = false },
                title = { Text("Send Feedback", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("We'd love to hear your thoughts and suggestions to improve KryptUp!")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            label = { Text("Your Comments") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (feedbackSent) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Thank you! Your feedback has been received.", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            feedbackSent = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Submit Feedback")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFeedbackDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionRecordTile(
    record: TransactionRecord,
    onClick: () -> Unit
) {
    val icon = when (record.type) {
        TransactionType.UPI_SEND, TransactionType.UPI_RECEIVE -> Icons.Default.AccountBalance
        TransactionType.CRYPTO_SEND, TransactionType.CRYPTO_RECEIVE -> Icons.Default.NorthEast
        TransactionType.SPOT_BUY, TransactionType.SPOT_SELL -> Icons.Default.BarChart
        TransactionType.BANK_DEPOSIT -> Icons.Default.CreditCard
        TransactionType.REWARD_CLAIM -> Icons.Default.EmojiEvents
    }

    val iconColor = when {
        !record.isDebit -> EmeraldGreen
        record.category == "Trading" -> ElectricViolet
        record.category == "Banking" -> CyberCyan
        record.category == "Rewards" -> GoldAmber
        else -> CoralRed
    }

    val formattedTime = remember(record.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LightSurface,
        border = BorderStroke(1.dp, LightBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "$formattedTime • ${record.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = record.amountDisplay,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (record.isDebit) TextPrimary else EmeraldGreen
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFD1FAE5)
                ) {
                    Text(
                        text = record.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButtonSpec(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = if (isPrimary) BrandCharcoal else Color(0xFFF4F5F7),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isPrimary) Color.White else TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

data class WatchlistItem(
    val symbol: String,
    val name: String,
    val priceUsd: String,
    val priceInr: String,
    val change: String,
    val isPositive: Boolean
)

val sampleWatchlist = listOf(
    WatchlistItem("BTC", "Bitcoin", "$94,250.00", "₹78,70,000", "+2.4%", true),
    WatchlistItem("ETH", "Ethereum", "$2,500.00", "₹2,08,750", "+1.8%", true),
    WatchlistItem("USDT", "Tether", "$1.00", "₹83.50", "0.0%", true),
    WatchlistItem("SOL", "Solana", "$185.50", "₹15,489", "-0.9%", false)
)

@Composable
fun WatchlistCardLight(item: WatchlistItem, currency: String = "USD") {
    val displayPrice = if (currency == "INR") item.priceInr else item.priceUsd

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, LightBorder),
        modifier = Modifier.width(135.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.isPositive) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = item.change,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (item.isPositive) EmeraldGreen else CoralRed,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = displayPrice,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SuperAppCardLight(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LightBorder),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModalPanel(
    onDismiss: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenFeedback: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LightSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = BrandCharcoal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings & Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Appearance & Preferences
            Text("APPEARANCE & PREFERENCES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(shape = RoundedCornerShape(16.dp), color = LightSurfaceVariant) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Dark Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Switch(checked = isDarkMode, onCheckedChange = onDarkModeToggle)
                    }
                    HorizontalDivider(color = LightBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("App Language", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text(selectedLanguage, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Help & Feedback
            Text("SUPPORT & FEEDBACK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(shape = RoundedCornerShape(16.dp), color = LightSurfaceVariant) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenHelp() }.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Help & FAQ Guide", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                    HorizontalDivider(color = LightBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenFeedback() }.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Feedback, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Send Feedback", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Legal & Software
            Text("LEGAL & LICENSES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(shape = RoundedCornerShape(16.dp), color = LightSurfaceVariant) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenTerms() }.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Terms of Service", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                    HorizontalDivider(color = LightBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenPrivacy() }.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Privacy Policy", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                    HorizontalDivider(color = LightBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenLicenses() }.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = TextPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Software Licenses", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("KryptUp Super-App v1.0.0", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text("Verified Secure Build • Build 2026.1", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}
