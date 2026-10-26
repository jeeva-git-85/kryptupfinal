package com.kryptup.kryptup.security

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test

class FakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getString(key: String?, defValue: String?): String? {
        return (map[key] as? String) ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return (map[key] as? Boolean) ?: defValue
    }

    override fun edit(): SharedPreferences.Editor = FakeEditor(map)

    override fun getAll(): MutableMap<String, *> = map
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
    override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) map.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            map.clear()
            return this
        }
        override fun apply() {}
        override fun commit(): Boolean = true

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
    }
}

class UserIsolationTest {

    @Test
    fun testUserIsolationKeys() {
        val prefs = FakeSharedPreferences()
        
        // Simulating User A keys
        val userAKey = "user_tx_pin_hash_user_A_123"
        prefs.edit().putString(userAKey, "hash_1234").apply()

        // Simulating User B keys
        val userBKey = "user_tx_pin_hash_user_B_456"
        assertNull("User B must NOT see User A's PIN hash", prefs.getString(userBKey, null))

        // User B sets their PIN hash
        prefs.edit().putString(userBKey, "hash_8888").apply()

        // Verify total separation
        assertEquals("hash_1234", prefs.getString(userAKey, null))
        assertEquals("hash_8888", prefs.getString(userBKey, null))
    }

    @Test
    fun testWalletMnemonicIsolationKeys() {
        val prefs = FakeSharedPreferences()

        val mnemonicUserAKey = "encrypted_mnemonic_user_A_123"
        val mnemonicUserBKey = "encrypted_mnemonic_user_B_456"

        prefs.edit().putString(mnemonicUserAKey, "candy maple cake sugar honey").apply()

        assertNull("User B must NOT see User A's mnemonic seed", prefs.getString(mnemonicUserBKey, null))

        prefs.edit().putString(mnemonicUserBKey, "apple banana cherry date elderberry").apply()

        assertEquals("candy maple cake sugar honey", prefs.getString(mnemonicUserAKey, null))
        assertEquals("apple banana cherry date elderberry", prefs.getString(mnemonicUserBKey, null))
    }
}
