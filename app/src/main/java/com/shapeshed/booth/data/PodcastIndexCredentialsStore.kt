package com.shapeshed.booth.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class PodcastIndexCredentials(val apiKey: String, val apiSecret: String)

/** Small Keystore-backed store for optional Podcast Index credentials. */
class PodcastIndexCredentialsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val _credentials = MutableStateFlow(readCredentials())
    val credentials: StateFlow<PodcastIndexCredentials?> = _credentials.asStateFlow()

    fun setCredentials(apiKey: String, apiSecret: String) {
        val value = PodcastIndexCredentials(apiKey.trim(), apiSecret.trim())
        if (value.apiKey.isBlank() || value.apiSecret.isBlank()) {
            clear()
            return
        }
        val encrypted = encrypt("${value.apiKey}\u0000${value.apiSecret}")
        preferences.edit {
            putString(KEY_CREDENTIALS, Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }
        _credentials.value = value
    }

    fun clear() {
        preferences.edit { remove(KEY_CREDENTIALS) }
        _credentials.value = null
    }

    private fun readCredentials(): PodcastIndexCredentials? = runCatching {
        val encoded = preferences.getString(KEY_CREDENTIALS, null) ?: return null
        val parts = decrypt(Base64.decode(encoded, Base64.NO_WRAP)).split('\u0000', limit = 2)
        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            PodcastIndexCredentials(parts[0], parts[1])
        } else null
    }.getOrNull()

    private fun encrypt(value: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decrypt(value: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, value.copyOf(IV_BYTES)))
        return cipher.doFinal(value.copyOfRange(IV_BYTES, value.size)).toString(StandardCharsets.UTF_8)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEYSTORE).apply {
            init(android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    private companion object {
        const val PREFERENCES = "podcast_index_credentials"
        const val KEY_CREDENTIALS = "encrypted_credentials"
        const val KEY_ALIAS = "booth_podcast_index_credentials"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
