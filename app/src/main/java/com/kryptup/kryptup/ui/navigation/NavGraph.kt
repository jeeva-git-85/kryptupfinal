package com.kryptup.kryptup.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text
import com.kryptup.kryptup.ui.screens.auth.LoginScreen
import com.kryptup.kryptup.ui.screens.auth.RegisterScreen
import com.kryptup.kryptup.ui.screens.banking.BankLinkingScreen
import com.kryptup.kryptup.ui.screens.home.HomeScreen
import com.kryptup.kryptup.ui.screens.wallet.WalletScreen
import com.kryptup.kryptup.ui.screens.wallet.SendScreen
import com.kryptup.kryptup.ui.screens.wallet.ReceiveScreen
import com.kryptup.kryptup.ui.screens.trading.TradingScreen
import com.kryptup.kryptup.ui.screens.chat.ChatScreen
import com.kryptup.kryptup.ui.screens.rewards.RewardsScreen
import com.kryptup.kryptup.ui.screens.splash.SplashScreen
import com.kryptup.kryptup.ui.screens.profile.ProfileScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.kryptup.kryptup.ui.screens.wallet.WalletViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeIn(animationSpec = tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(220)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = { fadeOut(animationSpec = tween(220)) }
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateNext = { isLoggedIn ->
                    val targetRoute = if (isLoggedIn) Screen.Home.route else Screen.Login.route
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        composable(route = Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToBankLinking = {
                    navController.navigate(Screen.BankLinking.route)
                },
                onNavigateToWallet = {
                    navController.navigate(Screen.Wallet.route)
                },
                onNavigateToScanner = {
                    navController.navigate(Screen.Scanner.route)
                },
                onNavigateToTrading = {
                    navController.navigate(Screen.Trading.route)
                },
                onNavigateToChat = {
                    navController.navigate(Screen.Chat.route)
                },
                onNavigateToRewards = {
                    navController.navigate(Screen.Rewards.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }
        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Trading.route) {
            TradingScreen(onBackClick = { navController.popBackStack() })
        }
        composable(route = Screen.Chat.route) {
            ChatScreen(onBackClick = { navController.popBackStack() })
        }
        composable(route = Screen.Rewards.route) {
            RewardsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(route = Screen.BankLinking.route) {
            BankLinkingScreen(
                onBackClick = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Wallet.route) {
            WalletScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSend = { navController.navigate(Screen.WalletSend.route) },
                onNavigateToReceive = { navController.navigate(Screen.WalletReceive.route) }
            )
        }
        composable(
            route = "${Screen.WalletSend.route}?address={address}",
            arguments = listOf(
                androidx.navigation.navArgument("address") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address")
            SendScreen(
                initialAddress = address,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.WalletReceive.route) {
            val walletViewModel: WalletViewModel = hiltViewModel()
            val address by walletViewModel.address.collectAsState()
            ReceiveScreen(
                address = address,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Scanner.route) {
            com.kryptup.kryptup.ui.screens.scanner.ScannerScreen(
                onQrScanned = { rawQr ->
                    navController.popBackStack()
                    val cleanRaw = rawQr.trim()
                    val ethRegex = Regex("^(?:ethereum:)?(0x[0-9a-fA-F]{40})(?:@.*|\\?.*)?$", RegexOption.IGNORE_CASE)
                    val upiRegex = Regex("(?:pa=)?([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+)", RegexOption.IGNORE_CASE)

                    val ethMatch = ethRegex.find(cleanRaw)
                    val upiMatch = upiRegex.find(cleanRaw)

                    val targetAddress = when {
                        ethMatch != null -> ethMatch.groupValues[1]
                        upiMatch != null -> upiMatch.groupValues[1]
                        cleanRaw.contains("@") -> cleanRaw
                        cleanRaw.startsWith("0x") -> cleanRaw
                        else -> cleanRaw
                    }

                    if (targetAddress.isNotEmpty()) {
                        val encoded = java.net.URLEncoder.encode(targetAddress, "UTF-8")
                        navController.navigate("${Screen.WalletSend.route}?address=$encoded")
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
