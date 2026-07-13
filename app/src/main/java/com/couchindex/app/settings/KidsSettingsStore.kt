package com.couchindex.app.settings

import android.content.Context
import com.couchindex.core.ViewerProfile
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class KidsSettings(
    val startInKidsMode: Boolean = false,
    val maximumAge: Int = DEFAULT_MAXIMUM_AGE,
) {
    companion object {
        const val DEFAULT_MAXIMUM_AGE = 11
        val SUPPORTED_AGES = listOf(7, 11, 15)
    }
}

class KidsSettingsStore(
    context: Context,
    private val pinHasher: ParentPinHasher = ParentPinHasher(),
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): KidsSettings = KidsSettings(
        startInKidsMode = preferences.getBoolean(KEY_START_IN_KIDS_MODE, false),
        maximumAge = preferences.getInt(KEY_MAXIMUM_AGE, KidsSettings.DEFAULT_MAXIMUM_AGE)
            .takeIf { it in KidsSettings.SUPPORTED_AGES }
            ?: KidsSettings.DEFAULT_MAXIMUM_AGE,
    )

    fun initialProfile(): ViewerProfile =
        if (load().startInKidsMode && hasPin()) ViewerProfile.Kids else ViewerProfile.Adult

    fun setStartInKidsMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_START_IN_KIDS_MODE, enabled).apply()
    }

    fun setMaximumAge(age: Int) {
        require(age in KidsSettings.SUPPORTED_AGES) { "unsupported Kids maximum age" }
        preferences.edit().putInt(KEY_MAXIMUM_AGE, age).apply()
    }

    fun hasPin(): Boolean =
        preferences.contains(KEY_PIN_SALT) && preferences.contains(KEY_PIN_HASH)

    fun setPin(pin: String) {
        ParentPinHasher.requireValidPin(pin)
        val salt = pinHasher.newSalt()
        val hash = pinHasher.hash(pin, salt)
        preferences.edit()
            .putString(KEY_PIN_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_PIN_HASH, Base64.getEncoder().encodeToString(hash))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (!ParentPinHasher.isValidPin(pin)) return false
        val salt = preferences.getString(KEY_PIN_SALT, null)?.decodeBase64() ?: return false
        val expectedHash = preferences.getString(KEY_PIN_HASH, null)?.decodeBase64() ?: return false
        return MessageDigest.isEqual(expectedHash, pinHasher.hash(pin, salt))
    }

    private fun String.decodeBase64(): ByteArray? =
        runCatching { Base64.getDecoder().decode(this) }.getOrNull()

    companion object {
        private const val PREFERENCES_NAME = "couchindex-kids-settings"
        private const val KEY_START_IN_KIDS_MODE = "start-in-kids-mode"
        private const val KEY_MAXIMUM_AGE = "maximum-age"
        private const val KEY_PIN_SALT = "parent-pin-salt"
        private const val KEY_PIN_HASH = "parent-pin-hash"
    }
}

class ParentPinHasher(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)

    fun hash(pin: String, salt: ByteArray): ByteArray {
        requireValidPin(pin)
        require(salt.isNotEmpty()) { "salt must not be empty" }
        return MessageDigest.getInstance("SHA-256").digest(salt + pin.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val SALT_BYTES = 16

        fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all(Char::isDigit)

        fun requireValidPin(pin: String) {
            require(isValidPin(pin)) { "parent PIN must contain exactly four digits" }
        }
    }
}
