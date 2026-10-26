package com.kryptup.kryptup.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.core.methods.request.Transaction as Web3jTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import org.web3j.crypto.Credentials
import org.web3j.tx.ChainId
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.tx.gas.StaticGasProvider
import com.kryptup.kryptup.data.model.WalletTransaction
import com.kryptup.kryptup.data.model.TransactionStatus

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Singleton
class EthereumRepository @Inject constructor(
    private val web3j: Web3j,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun getTransactionHistory(address: String): List<WalletTransaction> = withContext(Dispatchers.IO) {
        val currentUid = auth.currentUser?.uid ?: ""
        val list = mutableListOf<WalletTransaction>()

        try {
            if (currentUid.isNotEmpty()) {
                val snapshot = firestore.collection("users")
                    .document(currentUid)
                    .collection("transactions")
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    val typeStr = doc.getString("type") ?: ""
                    if (typeStr.contains("CRYPTO") || typeStr.contains("ETH") || typeStr.contains("SPOT")) {
                        val amountVal = doc.getDouble("amountValue") ?: 0.0
                        val isDebit = doc.getBoolean("isDebit") ?: true
                        val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val refId = doc.getString("referenceId") ?: "0x..."
                        val title = doc.getString("title") ?: "Crypto Transaction"

                        val symbol = if (title.contains("USDT")) "USDT" else "ETH"

                        list.add(
                            WalletTransaction(
                                hash = refId,
                                from = if (isDebit) address else "0xSepoliaFaucet",
                                to = if (isDebit) "0xRecipient" else address,
                                value = BigDecimal(amountVal.toString()),
                                symbol = symbol,
                                timestamp = ts,
                                isIncoming = !isDebit
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Dynamically reflect Sepolia Faucet claims if live balance > 0 and not yet logged
        try {
            val currentEth = getEthBalance(address)
            if (currentEth > BigDecimal.ZERO && list.none { it.isIncoming && it.symbol == "ETH" }) {
                list.add(
                    0,
                    WalletTransaction(
                        hash = "0x" + UUID.randomUUID().toString().replace("-", "").take(12),
                        from = "0xSepoliaFaucet",
                        to = address,
                        value = currentEth,
                        symbol = "ETH",
                        timestamp = System.currentTimeMillis(),
                        isIncoming = true
                    )
                )
            }
        } catch (_: Exception) {}

        list.sortedByDescending { it.timestamp }
    }

    suspend fun getEthBalance(address: String): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val balanceResponse = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send()
            val balanceWei = balanceResponse.balance
            Convert.fromWei(balanceWei.toString(), Convert.Unit.ETHER)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    suspend fun getErc20Balance(contractAddress: String, ownerAddress: String, decimals: Int): BigDecimal = withContext(Dispatchers.IO) {
        try {
            val function = Function(
                "balanceOf",
                listOf(Address(ownerAddress)),
                listOf(object : TypeReference<Uint256>() {})
            )
            val encodedFunction = FunctionEncoder.encode(function)
            val response = web3j.ethCall(
                Web3jTransaction.createEthCallTransaction(ownerAddress, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send()
            
            val results = FunctionReturnDecoder.decode(response.value, function.outputParameters)
            if (results.isNotEmpty()) {
                val balance = results[0].value as BigInteger
                balance.toBigDecimal().divide(BigDecimal.TEN.pow(decimals))
            } else {
                BigDecimal.ZERO
            }
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    suspend fun sendEth(credentials: Credentials, toAddress: String, amountEth: BigDecimal): String = withContext(Dispatchers.IO) {
        kotlinx.coroutines.withTimeout(30000L) {
            val transactionReceipt = Transfer.sendFunds(
                web3j, credentials, toAddress,
                amountEth, Convert.Unit.ETHER
            ).send()
            transactionReceipt.transactionHash
        }
    }

    suspend fun sendErc20(
        credentials: Credentials,
        contractAddress: String,
        toAddress: String,
        amount: BigDecimal,
        decimals: Int
    ): String = withContext(Dispatchers.IO) {
        kotlinx.coroutines.withTimeout(30000L) {
            val amountWei = amount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger()
            
            val function = Function(
                "transfer",
                listOf(Address(toAddress), Uint256(amountWei)),
                emptyList()
            )
            val encodedFunction = FunctionEncoder.encode(function)

            val chainId: Long = 11155111 // Sepolia Chain ID
            val transactionManager = RawTransactionManager(web3j, credentials, chainId)
            
            val dynamicGasPrice = try {
                web3j.ethGasPrice().send().gasPrice
            } catch (_: Exception) {
                DefaultGasProvider.GAS_PRICE
            }
            val erc20GasLimit = BigInteger.valueOf(100000L)

            val ethSendTransaction = transactionManager.sendTransaction(
                dynamicGasPrice,
                erc20GasLimit,
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
            )

            if (ethSendTransaction.hasError()) {
                throw Exception(ethSendTransaction.error.message)
            }
            ethSendTransaction.transactionHash
        }
    }
}
