# KryptUp Unit Test Report — 42 Passed Unit Tests

All 42 unit tests executed via `./gradlew testDebugUnitTest` passed successfully with **0 failures** and **0 skipped**.

---

## 1. Auth Validation Tests (`com.kryptup.kryptup.auth.AuthValidationTest`) — 7/7 Passed
1. `generateUpiId_withValidNameAndUid_createsCorrectHandle`
2. `generateUpiId_withEmptyName_fallsBackToUser`
3. `isValidResetEmail_missingAtSymbol_returnsFalse`
4. `isValidResetEmail_emptyString_returnsFalse`
5. `isValidResetEmail_validEmail_returnsTrue`
6. `isValidResetEmail_missingDomainDot_returnsFalse`
7. `generateUpiId_withSpecialCharacters_sanitizesCorrectly`

---

## 2. Banking Verification Tests (`com.kryptup.kryptup.banking.BankingVerificationTest`) — 7/7 Passed
8. `isOtpValid_correctSimulationOtp_returnsTrue`
9. `isOtpValid_wrongOtp_returnsFalse`
10. `validateUpiTransfer_insufficientBalance_fails`
11. `isCardValid_shortOrAlphabetCard_returnsFalse`
12. `validateUpiTransfer_selfTransfer_fails`
13. `isCardValid_validSixteenDigitCard_returnsTrue`
14. `validateUpiTransfer_sufficientBalanceDifferentUser_succeeds`

---

## 3. Financial Voice NLP Engine Tests (`com.kryptup.kryptup.voice.FinancialNlpEngineTest`) — 5/5 Passed
15. `parseCommand_unknownText_returnsUnknownIntent`
16. `parseCommand_checkPrice_returnsCheckPriceIntent`
17. `parseCommand_checkBalance_returnsCheckBalanceIntent`
18. `parseCommand_sendCrypto_returnsSendCryptoIntent`
19. `parseCommand_navigateScreen_returnsNavigateScreenIntent`

---

## 4. Voice NLP Comprehensive Tests (`com.kryptup.kryptup.voice.VoiceNlpComprehensiveTest`) — 8/8 Passed
20. `parseCommand_navigationScreens_returnsProperScreenTargets`
21. `parseCommand_priceForBtc_returnsBtcPriceIntent`
22. `parseCommand_checkBalanceNaturalLanguage_returnsCheckBalanceIntent`
23. `parseCommand_sendWithPayKeyword_returnsSendCryptoIntent`
24. `parseCommand_sendWithSubstringsInRecipientNames_avoidsPriceCollision`
25. `parseCommand_priceForTether_returnsUsdtPriceIntent`
26. `parseCommand_sendWithTransferKeyword_returnsSendCryptoIntent`
27. `parseCommand_unsupportedCommand_returnsUnknownIntent`

---

## 5. PIN Security Validation Tests (`com.kryptup.kryptup.security.PinSecurityValidationTest`) — 3/3 Passed
28. `pbkdf2_differentSalt_producesDifferentHashes`
29. `lockout_triggersAfterFiveFailedAttempts`
30. `pbkdf2_withSalt_isDeterministic`

---

## 6. Trading Order Calculation Tests (`com.kryptup.kryptup.trading.TradingOrderCalculationTest`) — 4/4 Passed
31. `calculateOrder_sellBtc_computesCreditAndSpotSell`
32. `calculateOrder_negativeAmount_returnsNull`
33. `calculateOrder_invalidAmount_returnsNull`
34. `calculateOrder_buyEth_computesDebitAndSpotBuy`

---

## 7. Transaction Calculation Tests (`com.kryptup.kryptup.data.repository.TransactionCalculationTest`) — 3/3 Passed
35. `calculateTotalSpent_withAllCredits_returnsZero`
36. `calculateTotalSpent_sumsOnlyDebitTransactions`
37. `calculateTotalSpent_withEmptyList_returnsZero`

---

## 8. User Isolation Security Tests (`com.kryptup.kryptup.security.UserIsolationTest`) — 2/2 Passed
38. `testWalletMnemonicIsolationKeys`
39. `testUserIsolationKeys`

---

## 9. Wallet Manager Tests (`com.kryptup.kryptup.data.wallet.WalletManagerTest`) — 2/2 Passed
40. `deriveAddress_isDeterministic`
41. `generateMnemonic_returnsValidTwelveWordPhrase`

---

## 10. Core Setup Test (`com.kryptup.kryptup.ExampleUnitTest`) — 1/1 Passed
42. `addition_isCorrect`

---
*Report generated automatically from `./gradlew testDebugUnitTest` test results.*
