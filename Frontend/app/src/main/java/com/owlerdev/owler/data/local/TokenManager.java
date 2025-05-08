// app/data/local/TokenManager.java
package com.owlerdev.owler.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.google.android.gms.auth.GoogleAuthUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import timber.log.Timber;

/**
 * Securely manages storage and retrieval of access and refresh tokens
 * using Android Keystore for encryption keys.
 */
public class TokenManager {
    private static final String ANDROID_KEY_STORE      = "AndroidKeyStore";
    private static final String KEY_ALIAS              = "TOKEN_KEY_ALIAS";
    private static final String PREFS_NAME             = "secure_token_prefs";
    private static final String KEY_ACCESS_CIPHER      = "enc_access_token";
    private static final String KEY_ACCESS_IV          = "iv_access_token";
    private static final String KEY_REFRESH_CIPHER     = "enc_refresh_token";
    private static final String KEY_REFRESH_IV         = "iv_refresh_token";
    private static final String AES_MODE               = "AES/GCM/NoPadding";
    private static final int    GCM_TAG_LENGTH         = 128;

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            ensureKeyExists();
        } catch (Exception e) {
            Timber.e(e, "TokenManager: Failed to initialize Keystore key");
        }
    }

    /** Save both access and refresh tokens (encrypted). */
    public void saveTokens(String accessToken, String refreshToken) {
        try {
            ensureKeyExists();
            Timber.d("TokenManager: Saving access token: %s", accessToken);
            Timber.d("TokenManager: Saving refresh token: %s", refreshToken);
            // Encrypt both tokens
            EncryptionResult accessRes  = encrypt(accessToken);
            EncryptionResult refreshRes = encrypt(refreshToken);

            // Use a single editor
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_ACCESS_CIPHER, accessRes.cipherText);
            editor.putString(KEY_ACCESS_IV, accessRes.iv);
            editor.putString(KEY_REFRESH_CIPHER, refreshRes.cipherText);
            editor.putString(KEY_REFRESH_IV, refreshRes.iv);
            boolean success = editor.commit();
            Timber.d("TokenManager: Tokens saved securely, success=%b", success);
        } catch (Exception e) {
            Timber.e(e, "TokenManager: Error encrypting tokens");
        }
    }

    /** Retrieve the decrypted access token, or null if none. */
    public String getAccessToken() {
        try {
            ensureKeyExists();
        } catch (Exception e) {
            Timber.e(e, "TokenManager: Failed to ensure Keystore key in getAccessToken");
            return null;
        }
        String cipherText = prefs.getString(KEY_ACCESS_CIPHER, null);
        String ivBase64   = prefs.getString(KEY_ACCESS_IV, null);
        Timber.d("TokenManager: Getting access token, cipherText=%s, iv=%s", 
                cipherText != null ? "present" : "null", 
                ivBase64 != null ? "present" : "null");
        
        if (cipherText == null || ivBase64 == null) {
            Timber.d("TokenManager: No access token found in prefs");
            return null;
        }
        try {
            String token = decrypt(cipherText, ivBase64);
            Timber.d("TokenManager: Retrieved access token: %s", token);
            return token;
        } catch (Exception e) {
            Timber.e(e, "TokenManager: Error decrypting access token");
            return null;
        }
    }

    /** Retrieve the decrypted refresh token, or null if none. */
    public String getRefreshToken() {
        try {
            ensureKeyExists();
        } catch (Exception e) {
            Timber.e(e, "TokenManager: Failed to ensure Keystore key in getRefreshToken");
            return null;
        }
        String cipherText = prefs.getString(KEY_REFRESH_CIPHER, null);
        String ivBase64   = prefs.getString(KEY_REFRESH_IV, null);
        if (cipherText == null || ivBase64 == null) return null;
        try {
            return decrypt(cipherText, ivBase64);
        } catch (Exception e) {
            Timber.e(e, "TokenManager: Error decrypting refresh token");
            return null;
        }
    }

    /** Clears both tokens from storage. */
    public void clearTokens() {
        prefs.edit()
                .remove(KEY_ACCESS_CIPHER)
                .remove(KEY_ACCESS_IV)
                .remove(KEY_REFRESH_CIPHER)
                .remove(KEY_REFRESH_IV)
                .apply();
        Timber.d("TokenManager: Tokens cleared");
    }

    // --- INTERNAL HELPERS ---

    /** Ensure the AES key for encryption/decryption exists in the Keystore. */
    private void ensureKeyExists() throws GeneralSecurityException, IOException {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
        ks.load(null);
        if (!ks.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            keyGenerator.generateKey();
            Timber.d("TokenManager: Keystore key generated");
        }
    }

    /** Encrypts plaintext and returns base64‑encoded cipher text and IV. */
    private EncryptionResult encrypt(String plain) throws Exception {
        int attempts = 0;
        while (attempts < 2) {
            try {
                KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
                ks.load(null);
                SecretKey key = ((KeyStore.SecretKeyEntry)
                        ks.getEntry(KEY_ALIAS, null))
                        .getSecretKey();

                Cipher cipher = Cipher.getInstance(AES_MODE);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                byte[] iv         = cipher.getIV();
                byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

                return new EncryptionResult(
                        Base64.encodeToString(cipherText, Base64.NO_WRAP),
                        Base64.encodeToString(iv, Base64.NO_WRAP)
                );
            } catch (java.security.KeyStoreException e) {
                if (e.getMessage() != null && e.getMessage().contains("Uninitialized keystore") && attempts == 0) {
                    Timber.w("TokenManager: Keystore uninitialized, retrying after delay");
                    Thread.sleep(100); // 100ms delay
                    attempts++;
                    continue;
                } else {
                    throw e;
                }
            }
        }
        throw new java.security.KeyStoreException("Uninitialized keystore after retry");
    }

    /** Decrypts base64‑encoded cipher text and IV back into plaintext. */
    private String decrypt(String cipherTextBase64, String ivBase64) throws Exception {
        int attempts = 0;
        while (attempts < 2) {
            try {
                KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
                ks.load(null);
                SecretKey key = ((KeyStore.SecretKeyEntry)
                        ks.getEntry(KEY_ALIAS, null))
                        .getSecretKey();

                Cipher cipher = Cipher.getInstance(AES_MODE);
                byte[] iv         = Base64.decode(ivBase64, Base64.NO_WRAP);
                GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, key, spec);

                byte[] decodedCipher = Base64.decode(cipherTextBase64, Base64.NO_WRAP);
                byte[] plainBytes    = cipher.doFinal(decodedCipher);
                return new String(plainBytes, StandardCharsets.UTF_8);
            } catch (java.security.KeyStoreException e) {
                if (e.getMessage() != null && e.getMessage().contains("Uninitialized keystore") && attempts == 0) {
                    Timber.w("TokenManager: Keystore uninitialized during decrypt, retrying after delay");
                    Thread.sleep(100); // 100ms delay
                    attempts++;
                    continue;
                } else {
                    throw e;
                }
            }
        }
        throw new java.security.KeyStoreException("Uninitialized keystore after retry");
    }

    /** Simple struct for holding encryption outputs. */
    private static class EncryptionResult {
        final String cipherText;
        final String iv;
        EncryptionResult(String cipherText, String iv) {
            this.cipherText = cipherText;
            this.iv = iv;
        }
    }
}
