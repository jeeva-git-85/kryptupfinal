package com.kryptup.kryptup.voice

import java.math.BigDecimal

sealed class VoiceIntent {
    data class SendCrypto(val amount: BigDecimal, val symbol: String, val recipient: String) : VoiceIntent()
    data class NavigateScreen(val screen: String) : VoiceIntent()
    object CheckBalance : VoiceIntent()
    data class CheckPrice(val symbol: String) : VoiceIntent()
    data class Unknown(val rawText: String) : VoiceIntent()
}

class FinancialNlpEngine {

    fun parseCommand(text: String): VoiceIntent {
        val lower = text.lowercase()

        // 1. Send Crypto checked first with word boundaries to avoid collision with recipient names
        if (Regex("\\b(send|pay|transfer)\\b").containsMatchIn(lower)) {
            val sendIntent = parseSendIntent(lower, text)
            if (sendIntent is VoiceIntent.SendCrypto) {
                return sendIntent
            }
        }

        // 2. Check Balance
        if (lower.contains("balance") || lower.contains("how much do i have")) {
            return VoiceIntent.CheckBalance
        }

        // 3. Check Price with word boundary matching so names like Costa/Socrates don't collide
        if (Regex("\\b(price|rate|cost)\\b").containsMatchIn(lower)) {
            return parsePriceIntent(lower)
        }

        // 4. Screen Navigation
        if (lower.contains("open") || lower.contains("go to") || lower.contains("show") || lower.contains("navigate")) {
            return parseNavigateIntent(lower, text)
        }

        return VoiceIntent.Unknown(text)
    }

    private fun parseSendIntent(lowerText: String, originalText: String): VoiceIntent {
        val regex = Regex("(?:send|pay|transfer)\\s+([0-9.]+)\\s+([a-zA-Z]+)\\s+to\\s+([a-zA-Z0-9]+)")
        val match = regex.find(lowerText)

        return if (match != null && match.groupValues.size == 4) {
            val amount = match.groupValues[1].toBigDecimalOrNull() ?: BigDecimal.ZERO
            val symbol = match.groupValues[2].uppercase()
            val recipient = match.groupValues[3].replaceFirstChar { it.uppercase() }
            VoiceIntent.SendCrypto(amount, symbol, recipient)
        } else {
            VoiceIntent.Unknown(originalText)
        }
    }

    private fun parseNavigateIntent(lowerText: String, originalText: String): VoiceIntent {
        val target = when {
            lowerText.contains("wallet") || lowerText.contains("vault") -> "wallet"
            lowerText.contains("bank") || lowerText.contains("linking") || lowerText.contains("card") -> "bank"
            lowerText.contains("trading") || lowerText.contains("trade") || lowerText.contains("chart") -> "trading"
            lowerText.contains("scanner") || lowerText.contains("scan") || lowerText.contains("qr") -> "scanner"
            lowerText.contains("chat") || lowerText.contains("messages") -> "chat"
            lowerText.contains("reward") || lowerText.contains("points") -> "rewards"
            else -> null
        }

        return if (target != null) {
            VoiceIntent.NavigateScreen(target)
        } else {
            VoiceIntent.Unknown(originalText)
        }
    }

    private fun parsePriceIntent(lowerText: String): VoiceIntent {
        val symbol = when {
            lowerText.contains("usdt") || lowerText.contains("tether") -> "USDT"
            lowerText.contains("btc") || lowerText.contains("bitcoin") -> "BTC"
            lowerText.contains("eth") || lowerText.contains("ethereum") -> "ETH"
            else -> "ETH"
        }
        return VoiceIntent.CheckPrice(symbol)
    }
}
