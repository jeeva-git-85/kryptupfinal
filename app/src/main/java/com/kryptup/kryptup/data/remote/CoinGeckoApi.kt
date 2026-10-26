package com.kryptup.kryptup.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface CoinGeckoApi {
    @GET("simple/price")
    suspend fun getSimplePrice(
        @Query("ids") ids: String,
        @Query("vs_currencies") vsCurrencies: String = "usd",
        @Query("include_24hr_change") include24hrChange: Boolean = true
    ): Map<String, Map<String, Double>>

    @GET("coins/{id}/market_chart")
    suspend fun getMarketChart(
        @retrofit2.http.Path("id") id: String,
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("days") days: String = "1"
    ): MarketChartResponse
}

data class MarketChartResponse(
    @SerializedName("prices")
    val prices: List<List<Double>>
)
