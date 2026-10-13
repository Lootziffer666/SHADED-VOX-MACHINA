package com.voxelscan.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/* JADX INFO: loaded from: classes.dex */
final class SecureLicenseStore {
    private static final String INSTALLATION = "installation";
    private static final String KEY_ALIAS = "voxelscan-test-license-v1";
    private static final String LAST_CHECK = "last_check";
    private static final String LICENSE = "license";
    private static final String PREFS = "voxelscan_secure_license";
    private final SharedPreferences preferences;

    SecureLicenseStore(Context context) {
        this.preferences = context.getSharedPreferences(PREFS, 0);
    }

    synchronized String installationId() throws Exception {
        String str = read(INSTALLATION);
        if (str != null) {
            UUID.fromString(str);
            return str;
        }
        String string = UUID.randomUUID().toString();
        write(INSTALLATION, string);
        return string;
    }

    synchronized String license() {
        try {
        } catch (Exception unused) {
            clearLicense();
            return null;
        }
        return read(LICENSE);
    }

    synchronized long lastSuccessfulCheck() {
        long j;
        j = 0;
        try {
            String str = read(LAST_CHECK);
            if (str != null) {
                j = Long.parseLong(str);
            }
        } catch (Exception unused) {
            return 0L;
        }
        return j;
    }

    synchronized void saveLicense(String str) throws Exception {
        write(LICENSE, str);
        write(LAST_CHECK, Long.toString(System.currentTimeMillis()));
    }

    synchronized void markSuccessfulCheck() throws Exception {
        write(LAST_CHECK, Long.toString(System.currentTimeMillis()));
    }

    synchronized void clearLicense() {
        this.preferences.edit().remove(LICENSE).remove(LAST_CHECK).apply();
    }

    private void write(String str, String str2) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(1, key());
        this.preferences.edit().putString(str, Base64.encodeToString(cipher.getIV(), 2) + "." + Base64.encodeToString(cipher.doFinal(str2.getBytes(StandardCharsets.UTF_8)), 2)).commit();
    }

    private String read(String str) throws Exception {
        String string = this.preferences.getString(str, null);
        if (string == null) {
            return null;
        }
        String[] strArrSplit = string.split("\\.", 2);
        if (strArrSplit.length != 2) {
            throw new IllegalStateException("Malformed secure value");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(2, key(), new GCMParameterSpec(128, Base64.decode(strArrSplit[0], 2)));
        return new String(cipher.doFinal(Base64.decode(strArrSplit[1], 2)), StandardCharsets.UTF_8);
    }

    private SecretKey key() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS, 3).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());
            return keyGenerator.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }
}
