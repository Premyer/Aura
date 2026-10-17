package com.premyer.aura.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Private app storage, encrypted with a non-exportable Android Keystore key. No APK keys. */
class ProviderStore(context: Context) {
    private val preferences = context.getSharedPreferences("provider_v1", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alias = "aura.provider.v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    fun load(provider: Provider): ProviderConfig {
        val encoded = preferences.getString(provider.name, null) ?: return ProviderConfig(provider = provider)
        val parts = encoded.split(":")
        require(parts.size == 2) { "Gespeicherte Anbieterdaten sind beschädigt." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        }
        return gson.fromJson(
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8),
            ProviderConfig::class.java
        )
    }

    fun selected(): Provider = preferences.getString("selected", null)?.let {
        Provider.values().firstOrNull { provider -> provider.name == it }
    } ?: Provider.ANTHROPIC

    fun save(config: ProviderConfig) {
        require(config.validate() == null) { config.validate().orEmpty() }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(gson.toJson(config).toByteArray(Charsets.UTF_8))
        val value = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString(config.provider.name, value)
            .putString("selected", config.provider.name).commit()) { "Speichern fehlgeschlagen." }
    }

    fun delete(provider: Provider) {
        check(preferences.edit().remove(provider.name).commit()) { "Löschen fehlgeschlagen." }
    }
}
