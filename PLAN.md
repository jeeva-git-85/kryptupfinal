# KryptUp Development Plan

This plan outlines the phased implementation of the KryptUp Unified Fintech Super-App.

---

## 🛠 Phase 1: Foundation & Infrastructure
- [x] **Dependency Injection:** Set up Hilt for dependency management.
- [x] **Navigation:** Implement Compose Navigation with a centralized router.
- [x] **Firebase Integration:** Configure Firebase Auth, Firestore, and Realtime Database.
- [x] **Local Storage:** Set up Room for caching and EncryptedSharedPreferences for sensitive data.
- [x] **Networking:** Configure Retrofit with OkHttp for CoinGecko API integration.

## 🔐 Phase 2: Authentication & User Onboarding
- [x] **Auth Screens:** Build Login, Registration, and Password Reset UIs.
- [x] **Google Sign-In:** Implement Firebase Google Authentication.
- [x] **Profile Management:** Create User Profile screen and Firestore data persistence.
- [x] **Security:** Implement initial BiometricPrompt check for app entry.

## 🏦 Phase 3: Banking & Crypto Wallet Core
- [x] **Bank Linking:** Build ATM card input (with auto-formatting) and simulated OTP verification.
- [x] **Wallet Engine:** Integrate Web3j for HD Wallet creation and seed phrase import.
- [x] **Blockchain Connectivity:** Connect to Ethereum Sepolia Testnet.
- [x] **Token Management:** Implement balance tracking for ETH and ERC-20 tokens (USDT, USDC, etc.).

## 💸 Phase 4: P2P Transactions & QR System
- [x] **QR Generation:** Generate unique QR codes for user identification.
- [x] **QR Scanner:** Implement CameraX and ML Kit Barcode Scanning with background processing (Optimized).
- [x] **Transfer Workflow:** Amount entry -> Biometric verification -> Blockchain transaction signing.
- [x] **Status Tracking:** Real-time transaction status updates and receipt generation.

## 📈 Phase 5: Trading Platform
- [x] **Market Data:** Fetch real-time prices from CoinGecko.
- [x] **Charts:** Integrate MPAndroidChart for historical price visualization.
- [x] **Order System:** Build UI and logic for Market, Limit, Stop-loss, and Take-profit orders.
- [x] **Portfolio:** Implement P&L tracking and asset distribution visualization.

## 🎙 Phase 6: AI Assistant & Communication
- [x] **Voice Engine:** Implement SpeechRecognizer with Push-to-Talk (Privacy First).
- [x] **NLP Logic:** Rule-based intent classification and entity extraction for financial commands.
- [x] **Text-To-Speech:** Implement TTS for assistant feedback.
- [x] **Chat Hub:** Build Real-time chat using Firebase Realtime DB with auto-receipt sharing.

## 🎁 Phase 7: Rewards & Final Polish
- [x] **Gamification:** Implement the reward points logic for transactions and daily logins.
- [x] **UI/UX Refinement:** Ensure Material 3 consistency and smooth animations.
- [x] **Testing:** Unit tests for repositories and UI tests for critical flows.
- [x] **Security Audit:** Review Keystore usage and data encryption.
