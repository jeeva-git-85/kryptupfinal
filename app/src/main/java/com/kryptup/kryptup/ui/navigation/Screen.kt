package com.kryptup.kryptup.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Wallet : Screen("wallet")
    object Trading : Screen("trading")
    object Scanner : Screen("scanner")
    object Chat : Screen("chat")
    object BankLinking : Screen("bank_linking")
    object WalletReceive : Screen("wallet_receive")
    object WalletSend : Screen("wallet_send")
    object Rewards : Screen("rewards")
    object Profile : Screen("profile")
}
