package com.kryptup.kryptup.ui.screens.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kryptup.kryptup.data.model.TransactionRecord
import com.kryptup.kryptup.data.model.TransactionType
import com.kryptup.kryptup.data.repository.MarketRepository
import com.kryptup.kryptup.data.repository.TransactionRepository
import com.kryptup.kryptup.security.PinSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SpotHolding(
    val symbol: String,
    val name: String,
    val amount: Double,
    val avgBuyPrice: Double,
    val currentPrice: Double
) {
    val totalValueUsd: Double get() = amount * currentPrice
    val totalValueInr: Double get() = totalValueUsd * 83.50
    val pnlPercent: Double get() = if (avgBuyPrice > 0) ((currentPrice - avgBuyPrice) / avgBuyPrice) * 100.0 else 0.0
}

data class OpenOrder(
    val id: String,
    val symbol: String,
    val type: String, // "Limit Buy", "Limit Sell"
    val amount: Double,
    val targetPrice: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "OPEN"
)

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
    val pinSecurityManager: PinSecurityManager,
    private val transactionRepository: TransactionRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    @javax.inject.Named("encrypted") private val prefs: android.content.SharedPreferences
) : ViewModel() {

    private val _prices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val prices = _prices.asStateFlow()

    private val _chartData = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val chartData = _chartData.asStateFlow()

    private val _holdings = MutableStateFlow<List<SpotHolding>>(
        listOf(
            SpotHolding("ETH", "Ethereum", 1.25, 2350.00, 2500.00),
            SpotHolding("BTC", "Bitcoin", 0.015, 88500.00, 94250.00),
            SpotHolding("SOL", "Solana", 4.50, 160.00, 185.50)
        )
    )
    val holdings = _holdings.asStateFlow()

    private val _openOrders = MutableStateFlow<List<OpenOrder>>(
        listOf(
            OpenOrder("ORD-101", "ETH", "Limit Buy", 0.5, 2400.00),
            OpenOrder("ORD-102", "SOL", "Limit Sell", 2.0, 210.00)
        )
    )
    val openOrders = _openOrders.asStateFlow()

    fun cancelOrder(orderId: String) {
        _openOrders.value = _openOrders.value.filter { it.id != orderId }
    }

    init {
        fetchPrices()
        fetchChartData("ethereum")
    }

    fun fetchPrices() {
        viewModelScope.launch {
            val response = marketRepository.getPrices("ethereum,tether,bitcoin,solana,cardano,dogecoin,matic-network,ripple")
            val priceMap = mutableMapOf<String, Double>()
            response.forEach { (id, data) ->
                data["usd"]?.let { priceMap[id] = it }
            }
            _prices.value = priceMap
        }
    }

    fun fetchChartData(coinId: String) {
        viewModelScope.launch {
            val response = marketRepository.getMarketChart(coinId, "1")
            response?.prices?.let { data ->
                val points = data.mapIndexed { index, list ->
                    Pair(index.toFloat(), list[1].toFloat())
                }
                _chartData.value = points
            }
        }
    }

    fun executeOrder(type: String, symbol: String, amountStr: String, price: Double) {
        viewModelScope.launch {
            val amount = amountStr.toDoubleOrNull() ?: return@launch
            if (!amount.isFinite() || amount <= 0.0 || !price.isFinite() || price <= 0.0) return@launch
            val totalUsd = amount * price
            val totalInr = totalUsd * 83.50
            val isBuy = type.equals("Buy", ignoreCase = true)

            val uid = auth.currentUser?.uid ?: "default"
            val balanceKey = "user_fiat_balance_$uid"
            val currentBalance = prefs.getFloat(balanceKey, 0.0f).toDouble()

            val newBalance = if (isBuy) {
                (currentBalance - totalInr).coerceAtLeast(0.0)
            } else {
                currentBalance + totalInr
            }

            prefs.edit().putFloat(balanceKey, newBalance.toFloat()).apply()

            try {
                firestore.collection("users").document(uid).set(
                    mapOf("fiatBalanceInr" to newBalance),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            } catch (_: Exception) {}

            transactionRepository.recordTransaction(
                TransactionRecord(
                    type = if (isBuy) TransactionType.SPOT_BUY else TransactionType.SPOT_SELL,
                    title = "$type $amountStr $symbol",
                    description = "Spot Trade @ $${String.format(java.util.Locale.US, "%.2f", price)}/unit",
                    amountDisplay = if (isBuy) "-₹${String.format(java.util.Locale.US, "%.2f", totalInr)}" else "+₹${String.format(java.util.Locale.US, "%.2f", totalInr)}",
                    amountValue = totalInr,
                    isDebit = isBuy,
                    category = "Trading",
                    referenceId = "SPOT-" + UUID.randomUUID().toString().take(6).uppercase()
                )
            )
        }
    }
}
