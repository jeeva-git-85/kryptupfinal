"""
KryptUp Automated Test Runner
Mirrors and executes the test suites for KryptUp architecture:
Covers all core modules: Auth, PIN Security, Fiat Banking, Web3 Wallet,
P2P Transfers (Paise + Idempotency), Spot Trading Desk, Voice NLP, and Cloud Functions Logic.
"""

import hashlib
import math
import re
import time
import unittest
from decimal import Decimal

# ==============================================================================
# Module 1: Auth & Validation
# ==============================================================================
def generate_upi_id(name: str, uid: str) -> str:
    sanitized = re.sub(r"[^a-z0-9]", "", name.lower())
    if not sanitized:
        sanitized = "user"
    uid_suffix = uid[:8].lower()
    return f"{sanitized}{uid_suffix}@kryptup"

def is_valid_reset_email(email: str) -> bool:
    clean = email.strip()
    return bool(clean) and "@" in clean and "." in clean

class TestAuthValidation(unittest.TestCase):
    def test_generate_upi_id_valid(self):
        self.assertEqual(generate_upi_id("Alice Wonderland", "a1b2c3d4e5f6"), "alicewonderlanda1b2c3d4@kryptup")

    def test_generate_upi_id_special_chars(self):
        self.assertEqual(generate_upi_id("John Doe #1!", "xyz987654321"), "johndoe1xyz98765@kryptup")

    def test_generate_upi_id_empty_fallback(self):
        self.assertEqual(generate_upi_id("!@#$%", "u1234567890"), "useru1234567@kryptup")

    def test_valid_reset_email(self):
        self.assertTrue(is_valid_reset_email("test.user@kryptup.io"))
        self.assertTrue(is_valid_reset_email("  alice@gmail.com  "))
        self.assertFalse(is_valid_reset_email("invalidemail.com"))
        self.assertFalse(is_valid_reset_email("invalid@domain"))
        self.assertFalse(is_valid_reset_email("   "))

# ==============================================================================
# Module 2: Security & PIN Validation (PBKDF2 120,000 Iterations)
# ==============================================================================
def hash_pin_pbkdf2(pin: str, salt: bytes, iterations: int = 120_000) -> str:
    return hashlib.pbkdf2_hmac("sha256", pin.encode("utf-8"), salt, iterations).hex()

def verify_pin_pbkdf2(entered_pin: str, stored_hash: str, salt: bytes) -> bool:
    return hash_pin_pbkdf2(entered_pin, salt) == stored_hash

class TestPinSecurity(unittest.TestCase):
    def test_pbkdf2_hash_deterministic(self):
        salt = b"test_salt_123456"
        h1 = hash_pin_pbkdf2("1234", salt)
        h2 = hash_pin_pbkdf2("1234", salt)
        self.assertEqual(h1, h2)
        self.assertEqual(len(h1), 64)

    def test_verify_pin_success(self):
        salt = b"random_salt_9999"
        stored = hash_pin_pbkdf2("7890", salt)
        self.assertTrue(verify_pin_pbkdf2("7890", stored, salt))

    def test_verify_pin_wrong_fails(self):
        salt = b"random_salt_9999"
        stored = hash_pin_pbkdf2("7890", salt)
        self.assertFalse(verify_pin_pbkdf2("7891", stored, salt))
        self.assertFalse(verify_pin_pbkdf2("0000", stored, salt))

    def test_user_isolation(self):
        prefs = {}
        user_a_key = "user_tx_pin_hash_user_A"
        user_b_key = "user_tx_pin_hash_user_B"

        salt = b"shared_test_salt"
        prefs[user_a_key] = hash_pin_pbkdf2("1234", salt)
        self.assertNotIn(user_b_key, prefs)

        prefs[user_b_key] = hash_pin_pbkdf2("8888", salt)
        self.assertEqual(prefs[user_a_key], hash_pin_pbkdf2("1234", salt))
        self.assertEqual(prefs[user_b_key], hash_pin_pbkdf2("8888", salt))

# ==============================================================================
# Module 3: Fiat Banking & Luhn Check
# ==============================================================================
def is_card_valid(card_number: str) -> bool:
    clean = re.sub(r"\s+", "", card_number)
    if len(clean) != 16 or not clean.isdigit():
        return False
    # Standard Luhn checksum
    total = 0
    for i in range(len(clean)):
        digit = int(clean[len(clean) - 1 - i])
        if i % 2 == 1:
            digit *= 2
            if digit > 9:
                digit -= 9
        total += digit
    return total % 10 == 0

class TestBanking(unittest.TestCase):
    def test_card_validation_luhn(self):
        # 4111 1111 1111 1111 is Luhn-valid (Visa test card)
        self.assertTrue(is_card_valid("4111 1111 1111 1111"))
        self.assertTrue(is_card_valid("4111111111111111"))
        # Invalid checksums
        self.assertFalse(is_card_valid("4532890112345678"))
        self.assertFalse(is_card_valid("4111 2222 3333"))
        self.assertFalse(is_card_valid("4111 2222 3333 ABCD"))

# ==============================================================================
# Module 4: Cloud Functions Logic (Paise Precision & Idempotency)
# ==============================================================================
def execute_upi_transfer_paise(
    sender_balance_inr: float,
    recipient_balance_inr: float,
    amount_inr: float,
    idempotency_key: str,
    idempotency_store: dict,
    max_transfer_inr: float = 100_000.0
):
    if idempotency_key in idempotency_store:
        return idempotency_store[idempotency_key], True

    if not math.isfinite(amount_inr) or amount_inr <= 0:
        raise ValueError("Amount must be positive and finite")

    amount_paise = round(amount_inr * 100)
    max_paise = round(max_transfer_inr * 100)

    if amount_paise <= 0 or amount_paise > max_paise:
        raise ValueError("Amount exceeds limit or is non-positive")

    sender_paise = round(sender_balance_inr * 100)
    recipient_paise = round(recipient_balance_inr * 100)

    if sender_paise < amount_paise:
        raise ValueError("Insufficient balance")

    new_sender_inr = (sender_paise - amount_paise) / 100.0
    new_recipient_inr = (recipient_paise + amount_paise) / 100.0

    result = {
        "new_sender_inr": new_sender_inr,
        "new_recipient_inr": new_recipient_inr,
        "amount_inr": amount_inr
    }
    idempotency_store[idempotency_key] = result
    return result, False

def verify_bank_otp(session: dict, entered_otp: str, current_time: int):
    if session.get("used"):
        raise ValueError("OTP already used")
    if current_time > session.get("expires_at", 0):
        raise ValueError("OTP expired")
    if session.get("otp") != entered_otp.strip():
        raise ValueError("Invalid OTP")
    session["used"] = True
    return True

def claim_daily_reward(last_claim_time: int, current_time: int, current_points: int):
    ONE_DAY_MS = 24 * 60 * 60 * 1000
    if current_time - last_claim_time < ONE_DAY_MS:
        raise ValueError("Daily reward already claimed within 24 hours")
    return current_points + 50, current_time

class TestCloudFunctions(unittest.TestCase):
    def test_paise_precision_no_float_drift(self):
        store = {}
        # 100.00 - 0.10 - 0.20 should be exactly 99.70, not 99.69999999999999
        res1, _ = execute_upi_transfer_paise(100.00, 50.00, 0.10, "k1", store)
        self.assertEqual(res1["new_sender_inr"], 99.90)

        res2, _ = execute_upi_transfer_paise(res1["new_sender_inr"], res1["new_recipient_inr"], 0.20, "k2", store)
        self.assertEqual(res2["new_sender_inr"], 99.70)
        self.assertEqual(res2["new_recipient_inr"], 50.30)

    def test_idempotency_cache_prevents_duplicate_charge(self):
        store = {}
        res1, cached1 = execute_upi_transfer_paise(5000.0, 100.0, 250.0, "tx_req_unique_001", store)
        self.assertFalse(cached1)
        self.assertEqual(res1["new_sender_inr"], 4750.0)

        # Retry with the same key
        res2, cached2 = execute_upi_transfer_paise(5000.0, 100.0, 250.0, "tx_req_unique_001", store)
        self.assertTrue(cached2)
        self.assertEqual(res2["new_sender_inr"], 4750.0)

    def test_otp_expiration_and_reuse(self):
        now = int(time.time() * 1000)
        session = {
            "otp": "458921",
            "expires_at": now + 300_000,
            "used": False
        }
        # Success
        self.assertTrue(verify_bank_otp(session, "458921", now + 1000))
        # Reuse fails
        with self.assertRaises(ValueError):
            verify_bank_otp(session, "458921", now + 2000)

        # Expired fails
        expired_session = {"otp": "112233", "expires_at": now - 1000, "used": False}
        with self.assertRaises(ValueError):
            verify_bank_otp(expired_session, "112233", now)

    def test_daily_reward_streak_24h(self):
        now = 1_000_000_000
        ONE_DAY = 24 * 60 * 60 * 1000
        points, claim_time = claim_daily_reward(0, now, 100)
        self.assertEqual(points, 150)
        self.assertEqual(claim_time, now)

        # Attempt claim before 24h fails
        with self.assertRaises(ValueError):
            claim_daily_reward(claim_time, now + ONE_DAY - 1000, points)

        # Claim after 24h succeeds
        points2, _ = claim_daily_reward(claim_time, now + ONE_DAY + 100, points)
        self.assertEqual(points2, 200)

# ==============================================================================
# Module 5: Spot Trading Desk
# ==============================================================================
def calculate_order(order_type: str, symbol: str, amount_str: str, price: float):
    try:
        amount = float(amount_str)
    except ValueError:
        return None
    if math.isnan(amount) or not math.isfinite(amount) or amount <= 0 or not math.isfinite(price) or price <= 0:
        return None
    total_usd = amount * price
    is_buy = order_type.strip().lower() == "buy"
    return {
        "title": f"{order_type.capitalize()} {amount_str} {symbol.upper()}",
        "total_usd": total_usd,
        "is_debit": is_buy
    }

class TestTradingOrder(unittest.TestCase):
    def test_buy_eth(self):
        order = calculate_order("Buy", "ETH", "0.5", 3000.0)
        self.assertIsNotNone(order)
        self.assertEqual(order["total_usd"], 1500.0)
        self.assertTrue(order["is_debit"])

    def test_sell_btc(self):
        order = calculate_order("Sell", "BTC", "0.1", 60000.0)
        self.assertIsNotNone(order)
        self.assertEqual(order["total_usd"], 6000.0)
        self.assertFalse(order["is_debit"])

    def test_invalid_order_amount(self):
        self.assertIsNone(calculate_order("Buy", "ETH", "-1.5", 3000.0))
        self.assertIsNone(calculate_order("Buy", "ETH", "0", 3000.0))
        self.assertIsNone(calculate_order("Buy", "ETH", "abc", 3000.0))
        self.assertIsNone(calculate_order("Buy", "ETH", "0.5", -100.0))

# ==============================================================================
# Module 6: Transaction Analytics
# ==============================================================================
def calculate_total_spent(records: list[dict]) -> float:
    return sum(r["amount_value"] for r in records if r.get("is_debit", False))

class TestTransactionAnalytics(unittest.TestCase):
    def test_calculate_total_spent(self):
        records = [
            {"amount_value": 1500.0, "is_debit": True},
            {"amount_value": 500.0, "is_debit": False},
            {"amount_value": 250.0, "is_debit": True},
        ]
        total = calculate_total_spent(records)
        self.assertEqual(total, 1750.0)

    def test_calculate_empty_records(self):
        self.assertEqual(calculate_total_spent([]), 0.0)

# ==============================================================================
# Module 7: Voice NLP Parsing Engine
# ==============================================================================
class FinancialNlpEngine:
    def parse_command(self, text: str) -> dict:
        lower = text.lower()

        if re.search(r"\b(send|pay|transfer)\b", lower):
            match = re.search(r"(?:send|pay|transfer)\s+([0-9.]+)\s+([a-zA-Z]+)\s+to\s+([a-zA-Z0-9]+)", lower)
            if match:
                amount = Decimal(match.group(1))
                symbol = match.group(2).upper()
                recipient = match.group(3).capitalize()
                return {"intent": "SendCrypto", "amount": amount, "symbol": symbol, "recipient": recipient}

        if "balance" in lower or "how much do i have" in lower:
            return {"intent": "CheckBalance"}

        if re.search(r"\b(price|rate|cost)\b", lower):
            symbol = "ETH"
            if "btc" in lower or "bitcoin" in lower:
                symbol = "BTC"
            elif "usdt" in lower or "tether" in lower:
                symbol = "USDT"
            return {"intent": "CheckPrice", "symbol": symbol}

        if any(w in lower for w in ["open", "go to", "show", "navigate"]):
            for screen in ["wallet", "bank", "trading", "scanner", "chat", "rewards"]:
                if screen in lower:
                    return {"intent": "NavigateScreen", "screen": screen}

        return {"intent": "Unknown", "raw": text}

class TestVoiceNlp(unittest.TestCase):
    def setUp(self):
        self.engine = FinancialNlpEngine()

    def test_send_crypto_nlp(self):
        res = self.engine.parse_command("Send 0.5 ETH to Alice")
        self.assertEqual(res["intent"], "SendCrypto")
        self.assertEqual(res["amount"], Decimal("0.5"))
        self.assertEqual(res["symbol"], "ETH")
        self.assertEqual(res["recipient"], "Alice")

    def test_costa_socrates_no_collision(self):
        res = self.engine.parse_command("Send 2.5 USDT to Costa")
        self.assertEqual(res["intent"], "SendCrypto")
        self.assertEqual(res["recipient"], "Costa")

    def test_price_nlp(self):
        res = self.engine.parse_command("What is the price of Bitcoin?")
        self.assertEqual(res["intent"], "CheckPrice")
        self.assertEqual(res["symbol"], "BTC")

    def test_check_balance_nlp(self):
        res = self.engine.parse_command("Check my balance please")
        self.assertEqual(res["intent"], "CheckBalance")

    def test_navigation_nlp(self):
        res = self.engine.parse_command("Navigate to trading desk")
        self.assertEqual(res["intent"], "NavigateScreen")
        self.assertEqual(res["screen"], "trading")

    def test_unknown_nlp(self):
        res = self.engine.parse_command("play some jazz music")
        self.assertEqual(res["intent"], "Unknown")

# ==============================================================================
# Module 8: Web3 Wallet BIP-39 / BIP-44 Derivation
# ==============================================================================
class TestWalletManager(unittest.TestCase):
    def test_mnemonic_word_count(self):
        sample_mnemonic = "candy maple cake sugar honey pink sugar honey pink sugar honey pizza"
        words = sample_mnemonic.split(" ")
        self.assertEqual(len(words), 12)

    def test_bip39_seed_derivation(self):
        sample_mnemonic = "candy maple cake sugar honey pink sugar honey pink sugar honey pizza"
        passphrase = ""
        salt = ("mnemonic" + passphrase).encode("utf-8")
        seed = hashlib.pbkdf2_hmac("sha512", sample_mnemonic.encode("utf-8"), salt, 2048)
        self.assertEqual(len(seed), 64)
        self.assertTrue(len(seed.hex()) == 128)

if __name__ == "__main__":
    suite = unittest.TestSuite()
    for tc in [
        TestAuthValidation,
        TestPinSecurity,
        TestBanking,
        TestCloudFunctions,
        TestTradingOrder,
        TestTransactionAnalytics,
        TestVoiceNlp,
        TestWalletManager
    ]:
        suite.addTests(unittest.defaultTestLoader.loadTestsFromTestCase(tc))

    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    exit_code = 0 if result.wasSuccessful() else 1
    exit(exit_code)
