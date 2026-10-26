package com.kryptup.kryptup.ui.screens.trading

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.kryptup.kryptup.ui.screens.security.PinSetupDialog
import com.kryptup.kryptup.ui.screens.security.PinVerificationDialog
import com.kryptup.kryptup.ui.theme.*

data class CoinItem(val id: String, val name: String, val symbol: String)

val supportedCoins = listOf(
    CoinItem("ethereum", "Ethereum", "ETH"),
    CoinItem("bitcoin", "Bitcoin", "BTC"),
    CoinItem("solana", "Solana", "SOL"),
    CoinItem("cardano", "Cardano", "ADA"),
    CoinItem("dogecoin", "Dogecoin", "DOGE"),
    CoinItem("matic-network", "Polygon", "MATIC"),
    CoinItem("ripple", "XRP", "XRP"),
    CoinItem("tether", "Tether", "USDT")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingScreen(
    onBackClick: () -> Unit,
    viewModel: TradingViewModel = hiltViewModel()
) {
    val prices by viewModel.prices.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    
    var selectedCoin by remember { mutableStateOf(supportedCoins[0]) }
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderType by remember { mutableStateOf("Buy") }
    
    var pendingOrderAmount by remember { mutableStateOf("1.0") }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinVerificationDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val currentPrice = prices[selectedCoin.id] ?: when (selectedCoin.symbol) {
        "ETH" -> 2500.00
        "BTC" -> 94250.00
        "SOL" -> 185.50
        "ADA" -> 0.85
        "DOGE" -> 0.38
        "MATIC" -> 0.62
        "XRP" -> 1.45
        else -> 1.00
    }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            TopAppBar(
                title = { Text("Spot Trading Desk", fontWeight = FontWeight.Bold) },
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
                .padding(20.dp)
        ) {
            // Coin Selector Chips
            Text(
                text = "Select Coin / Market",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(supportedCoins) { coin ->
                    val isSelected = selectedCoin == coin
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) BrandCharcoal else LightSurface,
                        border = BorderStroke(1.dp, if (isSelected) BrandCharcoal else LightBorder),
                        modifier = Modifier.clickable {
                            selectedCoin = coin
                            viewModel.fetchChartData(coin.id)
                        }
                    ) {
                        Text(
                            text = "${coin.symbol} / USD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current Coin Price Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, LightBorder),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedCoin.name} (${selectedCoin.symbol})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = "$${String.format("%.2f", currentPrice)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFD1FAE5)
                    ) {
                        Text(
                            text = "+2.4% 24h",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Live Orderbook Depth (${selectedCoin.symbol}/USD)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            PriceChart(data = chartData)

            OrderbookDepthSection(coinSymbol = selectedCoin.symbol, currentPrice = currentPrice)

            Spacer(modifier = Modifier.height(24.dp))

            // Buy & Sell Order Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = { 
                        orderType = "Buy"
                        showOrderDialog = true 
                    }, 
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buy ${selectedCoin.symbol}", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { 
                        orderType = "Sell"
                        showOrderDialog = true 
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sell ${selectedCoin.symbol}", fontWeight = FontWeight.Bold)
                }
            }

            val holdings by viewModel.holdings.collectAsState()
            val openOrders by viewModel.openOrders.collectAsState()

            HoldingsAndOpenOrdersSection(
                holdings = holdings,
                openOrders = openOrders,
                onCancelOrder = { id -> viewModel.cancelOrder(id) }
            )
        }
    }

    if (showOrderDialog) {
        OrderDialog(
            coin = selectedCoin,
            type = orderType,
            price = currentPrice,
            onDismiss = { showOrderDialog = false },
            onConfirm = { amountStr ->
                pendingOrderAmount = amountStr
                showOrderDialog = false
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
        val totalCost = (pendingOrderAmount.toDoubleOrNull() ?: 1.0) * currentPrice
        PinVerificationDialog(
            title = "Authorize $orderType Order",
            transactionDetails = "$orderType $pendingOrderAmount ${selectedCoin.symbol} (~$${String.format("%.2f", totalCost)})",
            pinSecurityManager = viewModel.pinSecurityManager,
            onDismiss = { showPinVerificationDialog = false },
            onPinVerified = {
                showPinVerificationDialog = false
                viewModel.executeOrder(orderType, selectedCoin.symbol, pendingOrderAmount, currentPrice)
            }
        )
    }
}

@Composable
fun OrderbookDepthSection(coinSymbol: String, currentPrice: Double) {
    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LightBorder),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Live Spot Orderbook ($coinSymbol/USD)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Price (USD)", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                Text("Size ($coinSymbol)", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                Text("Total (USD)", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            val askPrices = listOf(currentPrice * 1.006, currentPrice * 1.004, currentPrice * 1.002)
            val askSizes = listOf(1.45, 0.88, 2.10)
            askPrices.forEachIndexed { i, p ->
                val size = askSizes[i]
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$${String.format(java.util.Locale.US, "%.2f", p)}", style = MaterialTheme.typography.bodySmall, color = CoralRed, fontWeight = FontWeight.Bold)
                    Text(String.format(java.util.Locale.US, "%.2f", size), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Text("$${String.format(java.util.Locale.US, "%.2f", p * size)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = LightBorder)

            val bidPrices = listOf(currentPrice * 0.998, currentPrice * 0.996, currentPrice * 0.994)
            val bidSizes = listOf(3.12, 1.75, 4.50)
            bidPrices.forEachIndexed { i, p ->
                val size = bidSizes[i]
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$${String.format(java.util.Locale.US, "%.2f", p)}", style = MaterialTheme.typography.bodySmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    Text(String.format(java.util.Locale.US, "%.2f", size), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Text("$${String.format(java.util.Locale.US, "%.2f", p * size)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun HoldingsAndOpenOrdersSection(
    holdings: List<SpotHolding>,
    openOrders: List<OpenOrder>,
    onCancelOrder: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Spot Holdings (${holdings.size})", fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Open Orders (${openOrders.size})", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            if (holdings.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LightSurface,
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No spot holdings active.", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(20.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    holdings.forEach { holding ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = LightSurface,
                            border = BorderStroke(1.dp, LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = CyberCyan.copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(holding.symbol.take(1), fontWeight = FontWeight.Bold, color = CyberCyan)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(holding.symbol, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("${holding.amount} ${holding.symbol}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$${String.format(java.util.Locale.US, "%.2f", holding.totalValueUsd)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                                    val isPos = holding.pnlPercent >= 0
                                    val sign = if (isPos) "+" else ""
                                    Text(
                                        text = "$sign${String.format(java.util.Locale.US, "%.2f", holding.pnlPercent)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPos) EmeraldGreen else CoralRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (openOrders.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LightSurface,
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No pending open limit orders.", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(20.dp))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    openOrders.forEach { order ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = LightSurface,
                            border = BorderStroke(1.dp, LightBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (order.type.contains("Buy")) EmeraldGreen.copy(alpha = 0.15f) else CoralRed.copy(alpha = 0.15f)
                                        ) {
                                            Text(order.type, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (order.type.contains("Buy")) EmeraldGreen else CoralRed, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${order.amount} ${order.symbol}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Target: $${String.format(java.util.Locale.US, "%.2f", order.targetPrice)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }

                                TextButton(onClick = { onCancelOrder(order.id) }) {
                                    Text("Cancel", color = CoralRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderDialog(
    coin: CoinItem,
    type: String,
    price: Double,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var amount by remember { mutableStateOf("1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$type ${coin.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Current Market Rate: $${String.format("%.2f", price)} USD", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (${coin.symbol})") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount) },
                colors = ButtonDefaults.buttonColors(containerColor = if (type == "Buy") EmeraldGreen else CoralRed)
            ) {
                Text("Confirm $type")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun PriceChart(data: List<Pair<Float, Float>>) {
    val displayData = if (data.isEmpty()) {
        listOf(
            Pair(0f, 100f), Pair(1f, 105f), Pair(2f, 102f), Pair(3f, 110f),
            Pair(4f, 108f), Pair(5f, 115f), Pair(6f, 120f)
        )
    } else {
        data
    }

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(true)
                xAxis.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.textColor = AndroidColor.GRAY
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = displayData.map { Entry(it.first, it.second) }
            val dataSet = LineDataSet(entries, "Price").apply {
                color = AndroidColor.parseColor("#059669")
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2.5f
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}
