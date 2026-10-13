package com.voxelscan.android;

import android.content.Context;
import android.util.Base64;
import androidx.core.app.NotificationCompat;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class TestLicenseManager {
    private final SecureLicenseStore store;

    interface Callback {
        void complete(boolean z, String str);
    }

    TestLicenseManager(Context context) {
        this.store = new SecureLicenseStore(context);
    }

    boolean isConfigured() {
        return (BuildConfig.ACTIVATION_URL.isEmpty() || BuildConfig.ACTIVATION_PUBLIC_KEY.isEmpty()) ? false : true;
    }

    boolean hasValidStoredLicense() {
        String strLicense = this.store.license();
        if (strLicense == null) {
            return false;
        }
        try {
            verifyLicense(strLicense, this.store.installationId());
            return true;
        } catch (Exception unused) {
            this.store.clearLicense();
            return false;
        }
    }

    boolean hasUsableOfflineLicense() {
        if (!hasValidStoredLicense()) {
            return false;
        }
        long jLastSuccessfulCheck = this.store.lastSuccessfulCheck();
        return jLastSuccessfulCheck > 0 && System.currentTimeMillis() - jLastSuccessfulCheck <= 1209600000;
    }

    boolean shouldCheckOnline() {
        return System.currentTimeMillis() - this.store.lastSuccessfulCheck() >= 259200000;
    }

    void activate(final String str, final Callback callback) {
        new Thread(new Runnable() { // from class: com.voxelscan.android.TestLicenseManager$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$activate$0(str, callback);
            }
        }, "VoxelScan-activation").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$activate$0(String str, Callback callback) {
        try {
            if (!isConfigured()) {
                throw new IllegalStateException("This VoxelScan Test build has no activation configuration.");
            }
            String strInstallationId = this.store.installationId();
            String string = ActivationClient.activate(str.trim().toUpperCase(), strInstallationId).getString("license");
            verifyLicense(string, strInstallationId);
            this.store.saveLicense(string);
            callback.complete(true, "Activation successful.");
        } catch (ActivationClient.ActivationException e) {
            callback.complete(false, e.getMessage());
        } catch (SocketTimeoutException unused) {
            callback.complete(false, "Activation server timed out. Check the connection and try again.");
        } catch (IOException unused2) {
            callback.complete(false, "Unable to connect to the activation server.");
        } catch (Exception e2) {
            callback.complete(false, "Activation failed: " + e2.getMessage());
        }
    }

    void checkOnline(final Callback callback) {
        new Thread(new Runnable() { // from class: com.voxelscan.android.TestLicenseManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$checkOnline$1(callback);
            }
        }, "VoxelScan-license-check").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkOnline$1(Callback callback) {
        String str;
        try {
            String strLicense = this.store.license();
            if (strLicense == null) {
                throw new IllegalStateException("No local license is available.");
            }
            String strInstallationId = this.store.installationId();
            verifyLicense(strLicense, strInstallationId);
            String strOptString = ActivationClient.check(strLicense, strInstallationId).optString(NotificationCompat.CATEGORY_STATUS);
            if ("VALID".equals(strOptString)) {
                this.store.markSuccessfulCheck();
                callback.complete(true, "License verified.");
                return;
            }
            this.store.clearLicense();
            if ("REVOKED".equals(strOptString)) {
                str = "This VoxelScan license has been revoked.";
            } else {
                str = "This VoxelScan license has expired.";
            }
            callback.complete(false, str);
        } catch (ActivationClient.ActivationException e) {
            if ("invalid_license".equals(e.code)) {
                this.store.clearLicense();
            }
            callback.complete(false, e.getMessage());
        } catch (SocketTimeoutException unused) {
            callback.complete(false, "License server timed out.");
        } catch (IOException unused2) {
            callback.complete(false, "Unable to connect to the license server.");
        } catch (Exception e2) {
            callback.complete(false, "License check failed: " + e2.getMessage());
        }
    }

    private static void verifyLicense(String str, String str2) throws Exception {
        JSONObject jSONObject = new JSONObject(str);
        byte[] bArrDecode = Base64.decode(jSONObject.getString("payload"), 11);
        byte[] bArrDecode2 = Base64.decode(jSONObject.getString("signature"), 11);
        PublicKey publicKeyGeneratePublic = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.decode(BuildConfig.ACTIVATION_PUBLIC_KEY, 0)));
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(publicKeyGeneratePublic);
        signature.update(bArrDecode);
        if (!signature.verify(bArrDecode2)) {
            throw new SecurityException("Invalid license signature.");
        }
        JSONObject jSONObject2 = new JSONObject(new String(bArrDecode, StandardCharsets.UTF_8));
        if (!"VoxelScan".equals(jSONObject2.optString("app"))) {
            throw new SecurityException("License belongs to another application.");
        }
        if (jSONObject2.optInt("licenseVersion", 0) != 1) {
            throw new SecurityException("Unsupported license version.");
        }
        if (!str2.equals(jSONObject2.optString("installationId"))) {
            throw new SecurityException("License belongs to another installation.");
        }
        if (jSONObject2.has("expiresAt") && !jSONObject2.isNull("expiresAt") && jSONObject2.getLong("expiresAt") < System.currentTimeMillis() / 1000) {
            throw new SecurityException("License has expired.");
        }
    }
}
