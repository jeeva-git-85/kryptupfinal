# KryptUp - Unified Fintech Super-App

![Status](https://img.shields.io/badge/Status-In%20Development-yellow)
![Platform](https://img.shields.io/badge/Platform-Android-green)
![Language](https://img.shields.io/badge/Language-Kotlin-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

> A unified fintech "Super-App" that bridges traditional fiat currency and cryptocurrency systems, enhanced with AI-powered voice automation, biometric security, and integrated communication.

---

## 📖 Abstract

KryptUp is an Android-based fintech application designed to unify the fragmented financial ecosystem by providing a single platform for banking, cryptocurrency management, trading, communication, and AI-assisted financial tasks.

The rapid growth of digital finance has created a fragmented ecosystem where users must juggle multiple applications for banking, cryptocurrency management, trading, and peer-to-peer transfers. Traditional banking apps lack crypto support, while crypto platforms often ignore fiat integration and user-friendly automation. **KryptUp** addresses this gap by seamlessly bridging traditional fiat currency and cryptocurrency systems in one application.

The app allows users to securely log in using Google or email/password authentication, link bank accounts (simulated with OTP verification), create and manage a non-custodial crypto wallet on the Ethereum Sepolia testnet, and execute peer-to-peer transactions by scanning QR codes followed by facial biometric verification. A built-in trading module provides real-time price data and a simulated order book for spot trading with market, limit, stop-loss, and take-profit orders. An AI-powered voice assistant, activated by the wake word **"Hey Krypt"**, enables hands-free operations such as sending crypto, placing trades, analyzing spending patterns, and providing EMI reminders. Users can chat with each other in real-time, with transaction receipts automatically reflected in the conversation. A reward points system incentivizes user engagement for transactions, trading activity, and daily logins.

The project demonstrates the practical application of modern technologies including Kotlin, Jetpack Compose, Firebase, Web3j blockchain integration, CameraX, ML Kit, BiometricPrompt, and voice recognition, showcasing full-stack mobile development skills.

---

## 🎯 Project Objectives

1. **Unified Financial Experience** – Provide a single app for both fiat and crypto management
2. **Secure P2P Transfers** – Enable fast, secure transfers using QR codes and facial biometric verification
3. **Integrated Trading** – Offer a simulated cryptocurrency exchange with real-time prices
4. **AI-Powered Automation** – Develop a voice assistant with wake word detection for hands-free financial tasks
5. **Social Transactions** – Allow chat with automatic transaction receipts
6. **User Engagement** – Implement a reward points system

---

## ✨ Features

### 🔐 Authentication
- Google Sign-In integration
- Email/Password registration and login
- Secure session management
- User profile management

### 🏦 Bank Account Linking
- ATM card details input with auto-formatting
- OTP verification (simulated)
- Masked card storage
- Secure data handling

### 💼 Crypto Wallet
- Create new HD wallet
- Import wallet via seed phrase
- Support for multiple tokens (ETH, USDT, USDC, DAI, LINK)
- Balance tracking
- Send/Receive functionality
- Private key secured in Android Keystore

### 📷 QR Code System
- Generate unique QR for each user
- Scan QR codes using camera
- Recipient information lookup
- Fast peer-to-peer identification

### 🔒 Transaction with Biometrics
- Amount entry with currency selection
- Facial/fingerprint verification via BiometricPrompt
- Blockchain transaction signing
- Real-time status updates
- Transaction receipt generation

### 📊 Trading Platform
- Real-time cryptocurrency prices from CoinGecko
- Interactive price charts
- Order book visualization
- Market and limit orders
- Stop-loss and take-profit options
- Portfolio tracking with P&L

### 🎙️ AI Voice Assistant
- Activation: Tap-to-Talk via floating microphone button
- Voice greeting: "Hey! What can I help you with today?"
- Intent classification: Two-tier NLP engine (rule-based local routing + Gemini AI fallback)
- Entity extraction: Token amounts, recipient names, currencies
- Supported commands:
    - "Send 0.5 ETH to Alice"
    - "What's the price of ETH?"
    - "Check my balance" / "Show my balance"
    - "Go to vault" / "Open trading screen" / "Show messages"
- Text-to-speech responses via Android TextToSpeech

### 💬 Communication Hub
- One-to-one real-time chat via Firebase Realtime Database
- Instant contact lookup and notification tracking
- In-conversation payment coordination

### 🎁 Reward System
- Earn reward points for activities
- 24-hour daily login streak reward claim
- Points balance tracking with real-time Firestore sync

---

## 🛠️ Technology Stack

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.1.10 | Primary programming language |
| Jetpack Compose | 2024.12.01 (BOM) | Modern declarative UI framework |
| Material 3 | 1.3 (BOM) | Design system |
| Navigation Compose | 2.8.5 | Screen navigation |
| MPAndroidChart | 3.1.0 | Price charts |

### Architecture
| Technology | Purpose |
|------------|---------|
| MVVM + Clean Architecture | Code organization |
| Hilt | Dependency injection |
| Kotlin Coroutines + Flow | Asynchronous operations |
| Repository Pattern | Data abstraction |

### Backend
| Technology | Purpose |
|------------|---------|
| Firebase Authentication | User authentication |
| Cloud Firestore | Primary database |
| Firebase Realtime Database | Chat functionality |

### Blockchain
| Technology | Purpose |
|------------|---------|
| Web3j | Ethereum interaction |
| Ethereum Sepolia Testnet | Testing network |
| ERC-20 | Token standard |
| Android Keystore | Private key security |

### Hardware Integration
| Technology | Purpose |
|------------|---------|
| CameraX | Camera preview |
| ML Kit Barcode Scanning | QR detection |
| BiometricPrompt | Facial/fingerprint verification |
| SpeechRecognizer | Voice input |
| TextToSpeech | Voice output |

### Networking
| Technology | Purpose |
|------------|---------|
| Retrofit | REST API client |
| OkHttp | HTTP client |
| Gson | JSON parsing |
| CoinGecko API | Cryptocurrency prices |

### Local Storage
| Technology | Purpose |
|------------|---------|
| Room Database | Offline cache |
| EncryptedSharedPreferences | Secure storage |

---

## 🏗️ System Architecture
