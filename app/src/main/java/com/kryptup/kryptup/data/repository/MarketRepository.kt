package com.kryptup.kryptup.data.repository

import com.kryptup.kryptup.data.remote.CoinGeckoApi
import com.kryptup.kryptup.data.remote.MarketChartResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepository @Inject constructor(
    private val api: CoinGeckoApi
) {
    suspend fun getPrices(ids: String): Map<String, Map<String, Double>> = withContext(Dispatchers.IO) {
        try {
            api.getSimplePrice(ids)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getMarketChart(id: String, days: String): MarketChartResponse? = withContext(Dispatchers.IO) {
        try {
            api.getMarketChart(id, days = days)
        } catch (e: Exception) {
            null
        }
    }
}
