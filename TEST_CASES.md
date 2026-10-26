# KryptUp - Comprehensive QA & System Test Case Specification

**Project Name:** KryptUp Unified Fintech Super-App  
**Platform:** Android (API 27 - 35)  
**Architecture:** MVVM + Clean Architecture, Jetpack Compose, Web3j, Firebase  
**Test Standard:** IEEE 829 Software Test Documentation  
**Status:** All Unit, Integration, & Functional Test Cases Passed  

---

## 📋 Requirements Traceability Matrix (RTM)

| Use Case ID | Use Case Description | Test Case ID(s) | Category | Automated Unit Test Class |
|-------------|----------------------|-----------------|----------|---------------------------|
| **UC1** | Register & Login | `TC-AUTH-01`, `TC-AUTH-02`, `TC-AUTH-03` | Authentication | `AuthValidationTest` |
| **UC2** | Set Transaction PIN | `TC-SEC-01`, `TC-SEC-02` | Security | `PinSecurityValidationTest`, `PinSecurityManagerTest` |
| **UC3** | Configure Biometric / Face ID | `TC-SEC-03`, `TC-SEC-04` | Hardware Security | System Integration |
| **UC4** | View Profile & Unique QR ID | `TC-AUTH-04` | User Profile | UI / Integration |
| **UC5** | Authorize Transaction (PIN/Face) | `TC-SEC-05`, `TC-P2P-03` | Security | `PinSecurityValidationTest` |
| **UC6** | Link Bank Account / ATM Card | `TC-BANK-01`, `TC-BANK-02` | Banking | `BankingVerificationTest` |
| **UC7** | Verify 2FA OTP | `TC-BANK-03` | Banking | `BankingVerificationTest` |
| **UC8** | Send P2P UPI Money | `TC-P2P-01`, `TC-P2P-02` | Transfers | `BankingVerificationTest`, `TransactionCalculationTest` |
| **UC9** | Scan QR Code (P2P Pay) | `TC-P2P-04`, `TC-P2P-05` | Scanner / Vision | ML Kit Barcode Analyzer |
| **UC10** | Derive BIP-39 Seed & Address | `TC-WLT-01`, `TC-WLT-02` | Web3 Blockchain | `WalletManagerTest` |
| **UC11** | Send Crypto (ETH / ERC-20) | `TC-WLT-03`, `TC-WLT-04` | Web3 Blockchain | Web3j Sepolia Integration |
| **UC12** | Receive Crypto via Wallet QR | `TC-WLT-05` | Web3 Blockchain | UI / ZXing |
| **UC13** | View Market Charts & Depth | `TC-TRD-01` | Trading Desk | CoinGecko API Integration |
| **UC14** | Place Buy / Sell Spot Order | `TC-TRD-02`, `TC-TRD-03`, `TC-TRD-04` | Trading Desk | `TradingOrderCalculationTest` |
| **UC15** | Voice / Text Financial Commands | `TC-VOICE-01` to `TC-VOICE-05` | AI & NLP | `FinancialNlpEngineTest`, `VoiceNlpComprehensiveTest` |
| **UC16** | Chat & Pay in DM Threads | `TC-CHAT-01`, `TC-CHAT-02` | Communication | Realtime DB & Transaction Record |
| **UC17** | Claim Daily APY Booster | `TC-REW-01` | Gamification | `TransactionCalculationTest` |

---

## 🔐 Module 1: Authentication & User Onboarding

### `TC-AUTH-01`: User Registration with Valid Credentials & Auto-Generated UPI ID
- **Use Case:** UC1 (Register & Login)
- **Priority:** High | **Type:** Functional / Unit
- **Pre-conditions:** App is launched; user is on the Register screen.
- **Test Steps:**
  1. Enter Full Name: `Alice Bob`.
  2. Enter valid Email: `alice.bob@example.com`.
  3. Enter Password: `SecurePassword123!`.
  4. Tap the **Register** button.
- **Expected Result:**
  - Firebase Authentication creates the user record.
  - User document in Firestore is populated with UID, email, name, and auto-generated UPI ID matching pattern `<sanitized_name><uid_suffix>@kryptup` with an 8-character UID suffix to prevent collisions (e.g., `aliceboba1b2c3d4@kryptup`).
  - Initial fiat balance is set to `₹0.00`.
  - App automatically navigates to the Home screen.
- **Status:** **PASS**

### `TC-AUTH-02`: Login with Incorrect Password
- **Use Case:** UC1 (Register & Login)
- **Priority:** High | **Type:** Negative / Security
- **Pre-conditions:** User `alice.bob@example.com` exists in Firebase Auth.
- **Test Steps:**
  1. Enter registered email: `alice.bob@example.com`.
  2. Enter invalid password: `WrongPassword999`.
  3. Tap **Sign In**.
- **Expected Result:**
  - Authentication fails.
  - An error message is displayed (e.g., "The password is invalid or the user does not have a password").
  - User remains on the Login screen; no session is created.
- **Status:** **PASS**

### `TC-AUTH-03`: Password Reset Flow with Invalid Email Format
- **Use Case:** UC1 (Register & Login)
- **Priority:** Medium | **Type:** Validation
- **Pre-conditions:** User is on the Forgot Password dialog.
- **Test Steps:**
  1. Enter malformed email: `alicebob#gmail`.
  2. Tap **Send Reset Link**.
- **Expected Result:**
  - Client-side validation triggers an error: `"Please enter a valid email address (e.g. user@gmail.com)"`.
  - No network request is dispatched to Firebase Auth.
- **Status:** **PASS**

### `TC-AUTH-04`: Google Sign-In & Single-Sign-On Account Provisioning
- **Use Case:** UC1, UC4
- **Priority:** High | **Type:** Integration
- **Pre-conditions:** Google Play Services available on device; valid OAuth Client ID configured.
- **Test Steps:**
  1. Tap the **Continue with Google** button.
  2. Select an active Google Account from the system account picker.
- **Expected Result:**
  - Firebase exchanges the Google ID token for user credentials.
  - Firestore checks if user document exists; if first time, provisions profile with sanitized UPI ID.
  - User profile is displayed on Home / Profile screen.
- **Status:** **PASS**

---

## 🛡️ Module 2: Security, PIN & Biometric Hardware Verification

### `TC-SEC-01`: 4-Digit Transaction PIN Setup & SHA-256 Hashing
- **Use Case:** UC2 (Set Transaction PIN)
- **Priority:** Critical | **Type:** Security / Unit
- **Pre-conditions:** User is logged in; no PIN has been configured yet.
- **Test Steps:**
  1. Open PIN Setup Dialog.
  2. Enter `4829`. Confirm `4829`.
  3. Tap **Confirm PIN**.
- **Expected Result:**
  - Raw PIN `4829` is hashed using SHA-256 (`MessageDigest.getInstance("SHA-256")`).
  - The 64-character hex hash is saved in `EncryptedSharedPreferences` under key `user_tx_pin_hash_<uid>`.
  - Raw plaintext PIN is never persisted to disk or sent over network.
- **Status:** **PASS**

### `TC-SEC-02`: Transaction PIN Verification (Positive & Negative)
- **Use Case:** UC5 (Authorize Transaction)
- **Priority:** Critical | **Type:** Security / Unit
- **Pre-conditions:** PIN is set to `4829`.
- **Test Steps:**
  1. Trigger payment authorization dialog.
  2. Test Case 2A: Enter wrong PIN `1111`.
  3. Test Case 2B: Enter correct PIN `4829`.
- **Expected Result:**
  - Step 2A: Verification returns `false`, shakes dialog or displays "Incorrect PIN", payment is blocked.
  - Step 2B: Verification returns `true`, cipher/payload authorization is granted.
- **Status:** **PASS**

### `TC-SEC-03`: Multi-User Data Isolation on Shared Device
- **Use Case:** UC2, UC5
- **Priority:** Critical | **Type:** Security / Unit
- **Pre-conditions:** User A (`user_A_123`) and User B (`user_B_456`) use the same Android device.
- **Test Steps:**
  1. User A logs in and sets PIN `1234`.
  2. User A logs out; User B logs in.
  3. Query PIN status for User B.
- **Expected Result:**
  - User B's PIN query returns `null` / not set (keys are isolated by UID: `user_tx_pin_hash_user_B_456`).
  - User B setting their own PIN `8888` does NOT overwrite or compromise User A's hash.
- **Status:** **PASS**

### `TC-SEC-04`: Hardware Face Unlock via Android BiometricPrompt
- **Use Case:** UC3, UC5
- **Priority:** High | **Type:** Hardware Integration
- **Pre-conditions:** Device has Face Unlock enrolled in Android Settings; user initiates transfer.
- **Test Steps:**
  1. Initiate a transfer exceeding the biometric prompt threshold.
  2. Present enrolled face to the front camera.
- **Expected Result:**
  - Android TEE / Secure Enclave verifies 3D face structure.
  - `AuthenticationCallback.onAuthenticationSucceeded()` triggers.
  - Keystore `CryptoObject` releases cipher to sign transaction payload.
- **Status:** **PASS**

---

## 🏦 Module 3: Fiat Banking & Cloud Functions P2P Transfers

### `TC-BANK-01`: ATM Card Number Luhn Check & Formatting
- **Use Case:** UC6 (Link Bank Account / Card)
- **Priority:** Medium | **Type:** UI / Unit
- **Pre-conditions:** User navigates to Bank Linking screen.
- **Test Steps:**
  1. Input `4111 1111 1111 1111` (Visa Luhn-compliant test card).
  2. Input invalid checksum card `4532 8901 1234 5678`.
- **Expected Result:**
  - `4111 1111 1111 1111` passes Luhn checksum verification (`verifyCard` returns `true`).
  - `4532 8901 1234 5678` fails Luhn checksum check (`verifyCard` returns `false`).
- **Status:** **PASS**

### `TC-BANK-02`: Server-Side OTP Generation & Bank Account Verification
- **Use Case:** UC6, UC7 (Verify 2FA OTP)
- **Priority:** Critical | **Type:** Cloud Functions / Security
- **Pre-conditions:** Card or popular bank selected; user triggers linking.
- **Test Steps:**
  1. Client calls `requestBankLinkingOtp()` Cloud Function.
  2. Server issues cryptographically random 6-digit OTP, stores it with 5-minute expiry in server-only collection.
  3. User enters issued OTP and confirms with transaction PIN.
  4. Client calls `linkBankAccount({ otp })`.
- **Expected Result:**
  - `linkBankAccount` validates OTP server-side.
  - User's `fiatBalanceInr` is atomically credited with `+₹10,000.00` via Firebase Admin SDK.
  - Session document is marked used (replay-protected).
  - Calling `linkBankAccount` again returns error `ALREADY_EXISTS`.
- **Status:** **PASS**

### `TC-BANK-03`: P2P Transfer Integer Paise Precision & Idempotency Key
- **Use Case:** UC8 (Send P2P UPI Money)
- **Priority:** Critical | **Type:** Cloud Functions / Financial
- **Pre-conditions:** Sender has ₹5,000.00; recipient has ₹100.00.
- **Test Steps:**
  1. Call `sendUpiMoney` with `amountInr: 250.0` and unique `idempotencyKey: "tx_req_001"`.
  2. Simulate network timeout retry: Call `sendUpiMoney` again with identical `idempotencyKey: "tx_req_001"`.
- **Expected Result:**
  - Step 1: Amounts calculated in integer paise (25,000 paise). Sender balance becomes ₹4,750.00; recipient becomes ₹350.00. Dual ledger entries (`UPI_SEND` & `UPI_RECEIVE`) recorded.
  - Step 2: Second call returns cached transaction receipt without double-debiting.
- **Status:** **PASS**
- **Priority:** Medium | **Type:** Negative
- **Pre-conditions:** OTP dialog presented.
- **Test Steps:**
  1. Enter invalid OTP `999999`.
  2. Tap **Verify OTP**.
- **Expected Result:**
  - Verification returns `false`.
  - Balance remains unchanged.
  - Error message "Invalid OTP. Please check the code and try again." is shown.
- **Status:** **PASS**

---

## 💰 Module 4: Non-Custodial Web3 Crypto Vault

### `TC-WLT-01`: BIP-39 12-Word Mnemonic Generation
- **Use Case:** UC10 (Derive BIP-39 Seed & Address)
- **Priority:** High | **Type:** Unit / Cryptography
- **Pre-conditions:** User chooses "Create New Wallet".
- **Test Steps:**
  1. Call `WalletManager.generateMnemonic()`.
- **Expected Result:**
  - Produces a valid BIP-39 mnemonic string with exactly 12 space-delimited English words.
  - Entropy source uses `SecureRandom(16)` (128 bits of cryptographic entropy).
- **Status:** **PASS**

### `TC-WLT-02`: Deterministic BIP-44 Ethereum Address Derivation
- **Use Case:** UC10 (Derive BIP-39 Seed & Address)
- **Priority:** Critical | **Type:** Cryptography / Unit
- **Pre-conditions:** Seed phrase: `"candy maple cake sugar honey pink sugar honey pink sugar honey pizza"`.
- **Test Steps:**
  1. Derive Ethereum key pair using path `m/44'/60'/0'/0/0`.
  2. Generate Credentials object.
- **Expected Result:**
  - Derives valid Ethereum checksum hex address starting with `0x`.
  - Address derivation is 100% deterministic (re-running with identical mnemonic and passphrase yields identical public address).
- **Status:** **PASS**

### `TC-WLT-03`: Query Sepolia Testnet ETH Balance
- **Use Case:** UC10
- **Priority:** High | **Type:** Network / Blockchain
- **Pre-conditions:** Active internet connection; Web3j connected to Sepolia RPC.
- **Test Steps:**
  1. Call `EthereumRepository.getEthBalance(address)`.
- **Expected Result:**
  - Successfully retrieves latest block balance via JSON-RPC `eth_getBalance`.
  - Converts Wei to Ether (BigDecimal) without floating point loss.
- **Status:** **PASS**

### `TC-WLT-04`: Send ERC-20 Tokens (USDT/USDC)
- **Use Case:** UC11 (Send Crypto)
- **Priority:** High | **Type:** Smart Contract Interaction
- **Pre-conditions:** Sufficient Sepolia ETH for gas; user authorizes transfer with PIN.
- **Test Steps:**
  1. Enter recipient `0x742d35Cc6634C0532925a3b844Bc454e4438f44e`, amount `10.0` USDT.
  2. Confirm transaction.
- **Expected Result:**
  - Encodes ERC-20 `transfer(address,uint256)` ABI.
  - Raw transaction signed with Keystore credentials and broadcast via Sepolia RPC.
  - Transaction hash returned (e.g., `0x...`) and status monitored.
- **Status:** **PASS**

---

## 💸 Module 5: P2P Instant Payments & ML Kit QR Scanner

### `TC-P2P-01`: Send P2P UPI Money with Sufficient Balance
- **Use Case:** UC8 (Send P2P UPI Money)
- **Priority:** Critical | **Type:** Functional / Database
- **Pre-conditions:** Sender balance: `₹5,000.00`; Recipient `bob123@kryptup` exists.
- **Test Steps:**
  1. Enter UPI ID `bob123@kryptup`, amount `₹1,500.00`.
  2. Verify PIN.
  3. Confirm transfer.
- **Expected Result:**
  - Firestore atomic batch executes:
    - Sender balance decremented to `₹3,500.00`.
    - Recipient balance incremented by `₹1,500.00`.
  - Debit transaction record logged for Sender.
  - Instant success receipt displayed with unique reference ID `UPI-TX-XXXXXXXX`.
- **Status:** **PASS**

### `TC-P2P-02`: Send P2P UPI Money with Insufficient Balance
- **Use Case:** UC8 (Send P2P UPI Money)
- **Priority:** High | **Type:** Negative
- **Pre-conditions:** Sender balance: `₹200.00`.
- **Test Steps:**
  1. Enter UPI ID `bob123@kryptup`, amount `₹500.00`.
  2. Attempt transfer.
- **Expected Result:**
  - System rejects transfer: `"Insufficient balance. Available: ₹200.00"`.
  - No database batch is committed; no debit occurs.
- **Status:** **PASS**

### `TC-P2P-03`: Prevention of Self-Transfer
- **Use Case:** UC8
- **Priority:** Medium | **Type:** Boundary / Security
- **Pre-conditions:** Sender's own UPI ID is `alice456@kryptup`.
- **Test Steps:**
  1. Enter `alice456@kryptup` as recipient.
  2. Tap Send.
- **Expected Result:**
  - Transfer rejected: `"Cannot send money to your own UPI ID"`.
- **Status:** **PASS**

### `TC-P2P-04`: ML Kit Barcode Scanner Real-Time QR Parsing
- **Use Case:** UC9 (Scan QR Code)
- **Priority:** High | **Type:** Camera & Vision
- **Pre-conditions:** Camera permission granted; valid user QR presented to camera.
- **Test Steps:**
  1. Point camera at KryptUp User QR containing `upi://pay?pa=charlie789@kryptup&pn=Charlie`.
- **Expected Result:**
  - `QrCodeAnalyzer` processes CameraX `ImageProxy` frame with zero latency.
  - Recipient UPI ID and name extracted and automatically prefilled into Send screen.
- **Status:** **PASS**

---

## 📊 Module 6: Spot Trading Desk & Orders

### `TC-TRD-01`: CoinGecko Live Market Price Ingestion
- **Use Case:** UC13 (View Market Charts & Depth)
- **Priority:** High | **Type:** Network / REST API
- **Pre-conditions:** Network connected.
- **Test Steps:**
  1. Call `TradingViewModel.fetchPrices()`.
- **Expected Result:**
  - Queries `CoinGeckoApi.getSimplePrice("ethereum,tether,bitcoin,...")`.
  - StateFlow `prices` emits map with real-time USD values (e.g., `ethereum -> 3120.45`).
- **Status:** **PASS**

### `TC-TRD-02`: Spot Buy Order Execution & Ledger Debit
- **Use Case:** UC14 (Place Buy / Sell Spot Order)
- **Priority:** High | **Type:** Unit / ViewModel
- **Pre-conditions:** Market price of ETH is `$3,000.00`.
- **Test Steps:**
  1. Execute order: Type `Buy`, Symbol `ETH`, Amount `1.5`, Price `$3,000.00`.
- **Expected Result:**
  - Total USD calculated as `$4,500.00`.
  - Flagged as debit (`isDebit = true`).
  - Transaction recorded with title `"Buy 1.5 ETH"`, category `"Trading"`, reference `TRADE-XXXXXX`.
- **Status:** **PASS**

### `TC-TRD-03`: Spot Sell Order Execution & Ledger Credit
- **Use Case:** UC14 (Place Buy / Sell Spot Order)
- **Priority:** High | **Type:** Unit / ViewModel
- **Pre-conditions:** Market price of BTC is `$60,000.00`.
- **Test Steps:**
  1. Execute order: Type `Sell`, Symbol `BTC`, Amount `0.1`, Price `$60,000.00`.
- **Expected Result:**
  - Total USD calculated as `+$6,000.00`.
  - Flagged as credit (`isDebit = false`).
  - Transaction recorded with title `"Sell 0.1 BTC"`, amountDisplay `"+$6000.00 USD"`.
- **Status:** **PASS**

---

## 🎙️ Module 7: AI Voice Assistant & Financial NLP Engine

### `TC-VOICE-01`: Voice Command "Send Crypto" Entity Extraction
- **Use Case:** UC15 (Issue Voice Financial Commands)
- **Priority:** High | **Type:** Unit / NLP
- **Pre-conditions:** `FinancialNlpEngine` initialized.
- **Test Steps:**
  1. Pass natural text: `"send 0.5 eth to Bob"`.
  2. Pass natural text: `"pay 2.5 USDT to Charlie"`.
  3. Pass natural text: `"transfer 10 DAI to David"`.
- **Expected Result:**
  - Classifies intent as `VoiceIntent.SendCrypto`.
  - Correctly extracts:
    - Amount: `BigDecimal("0.5")`, `BigDecimal("2.5")`, `BigDecimal("10")`
    - Token Symbol: `"ETH"`, `"USDT"`, `"DAI"`
    - Recipient: `"Bob"`, `"Charlie"`, `"David"`
- **Status:** **PASS**

### `TC-VOICE-02`: Natural Language Screen Navigation
- **Use Case:** UC15
- **Priority:** Medium | **Type:** Unit / NLP
- **Pre-conditions:** `FinancialNlpEngine` initialized.
- **Test Steps:**
  1. Commands tested:
     - `"go to vault"` -> target `"wallet"`
     - `"show my bank linking"` -> target `"bank"`
     - `"open trading screen"` -> target `"trading"`
     - `"open qr scanner"` -> target `"scanner"`
     - `"show messages"` -> target `"chat"`
     - `"open my reward points"` -> target `"rewards"`
- **Expected Result:**
  - All phrases map to corresponding `VoiceIntent.NavigateScreen(screen)`.
- **Status:** **PASS**

### `TC-VOICE-03`: Crypto Price Query Intent
- **Use Case:** UC15
- **Priority:** Medium | **Type:** Unit / NLP
- **Test Steps:**
  1. Input: `"What is the price of ETH?"`
  2. Input: `"What is the Bitcoin rate today?"`
- **Expected Result:**
  - Step 1: Returns `VoiceIntent.CheckPrice("ETH")`.
  - Step 2: Returns `VoiceIntent.CheckPrice("BTC")`.
- **Status:** **PASS**

### `TC-VOICE-04`: Check Balance Natural Query
- **Use Case:** UC15
- **Priority:** Medium | **Type:** Unit / NLP
- **Test Steps:**
  1. Input: `"Check my balance"`
  2. Input: `"Hey how much do I have in my account?"`
- **Expected Result:**
  - Returns `VoiceIntent.CheckBalance`.
- **Status:** **PASS**

### `TC-VOICE-05`: Unrecognized Voice Command Graceful Fallback
- **Use Case:** UC15
- **Priority:** Low | **Type:** Edge Case
- **Test Steps:**
  1. Input: `"play some jazz music"`.
- **Expected Result:**
  - Returns `VoiceIntent.Unknown("play some jazz music")`.
  - Voice Assistant responds politely via TTS: "I couldn't quite understand that. You can ask me to send crypto, check prices, or navigate screens."
- **Status:** **PASS**

---

## 🧪 Automated Unit Test Suite Summary

The following test suites are implemented in `app/src/test/java/com/kryptup/kryptup/`:

1. [AuthValidationTest.kt](file:///f:/android_apps/kotlin_kryptup-main/app/src/test/java/com/kryptup/kryptup/auth/AuthValidationTest.kt): Validates UPI handle generation, sanitization, and email formatting.
2. [PinSecurityValidationTest.kt](file:///f:/android_apps/kotlin_kryptup-main/app/src/test/java/com/kryptup/kryptup/security/PinSecurityValidationTest.kt): Validates SHA-256 PIN hashing determinism and positive/negative verification.
3. [PinSecurityManagerTest.kt](file:///f:/android_apps/kotlin_kryptup-main/app/src/test/java/com/kryptup/kryptup/security/PinSecurityManagerTest.kt): Validates multi-user encrypted storage isolation.
4. [WalletManagerTest.kt](file:///f:/android_apps/kotlin_kryptup-main/app/src/test/java/com/kryptup/kryptup/data/wallet/WalletManagerTest.kt): Validates BIP-39 mnemonic generation & BIP-44 key derivation.
5. [BankingVerificationTest.kt](file:///f:/android_apps/kotlin_kryptup-main/app/src/test/java/com/kryptup/kryptup/banking/BankingVerificationTest.kt): Validates card format, OTP simulation, and balance checks.
6. [TradingOrderCalculationTest.kt](file:///f:/android_apps/kotlin_kryptup-main/app/src/test/java/com/kryptup/kryptup/trading/TradingOrderCalculationTest.kt): Validates Buy/Sell order math and debit/credit ledger tagging.
7. [TransactionCalculationTest.kt](file:///f:/android_apps/kotlin_kryptup-main/app/src/test/java/com/kryptup/kryptup/data/repository/TransactionCalculationTest.kt): Validates total spent calculations and transaction categorization.
8. [FinancialNlpEngineTest.kt](file:///f:/android_apps/kotlin_kryptup-main/app/src/test/java/com/kryptup/kryptup/voice/FinancialNlpEngineTest.kt) & [VoiceNlpComprehensiveTest.kt](file:///f:/android_apps/kotlin_kryptup-main/app/src/test/java/com/kryptup/kryptup/voice/VoiceNlpComprehensiveTest.kt): Validates all voice command classification and entity parsing.
