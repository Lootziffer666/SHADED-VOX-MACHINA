package com.google.ar.core;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import kotlin.io.encoding.Base64;

/* JADX INFO: loaded from: classes.dex */
final class SessionCreateJniHelper {
    private static final byte[] a = {57, 53, 55, 116, Base64.padSymbol, 53, 53, Base64.padSymbol, 54, 63, 116, 59, 52, 62, 40, 53, 51, 62, 116, Base64.padSymbol, 54, 59, 41, 41, 63, 41, 116, 57, 53, 40, 63};
    private static final String b;
    private static final String[] c;
    private static final String[] d;

    static {
        byte[] bArr = new byte[31];
        for (int i = 0; i < 31; i++) {
            bArr[i] = (byte) (a[i] ^ 90);
        }
        b = new String(bArr);
        c = new String[]{"42E18C407031E802A349ACDAEBFBFC8CE93A693E5C1A8A12CE4613E57A233FBC", "03D6C1FD40D676294BC0BEB0D1A2E7AABC227C2044D176E2A35B3340033352AB"};
        d = new String[]{"AE95C5718B550CAD726DC268702DF2BACBC2AA292924782B4384E2B990022825", "103938EE4537E59E8EE792F654504FB8346FC6B346D0BBC4415FC339FCFC8EC1", "6379DDB41110A3F38DC9CD0855FFDB099184C749A6DDF64B7F67E4A3DA5674D7"};
    }

    private SessionCreateJniHelper() {
    }

    private static boolean a(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(b, 64);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            for (Signature signature : packageInfo.signatures) {
                messageDigest.update(signature.toByteArray());
                byte[] bArrDigest = messageDigest.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b2 : bArrDigest) {
                    sb.append(String.format("%02X", Byte.valueOf(b2)));
                }
                String string = sb.toString();
                String[] strArr = c;
                int length = strArr.length;
                for (int i = 0; i < 2; i++) {
                    if (strArr[i].equals(string)) {
                        return true;
                    }
                }
                if (!Build.TYPE.equals("user")) {
                    String[] strArr2 = d;
                    int length2 = strArr2.length;
                    for (int i2 = 0; i2 < 3; i2++) {
                        if (strArr2[i2].equals(string)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            Log.e("ARCore-SessionCreateJni", "Failed to check projected apk signature", e);
            return false;
        }
    }

    static boolean checkApkSignature(Context context) throws PackageManager.NameNotFoundException {
        if (useProjectedApk(context)) {
            return a(context);
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo("com.google.ar.core", 64);
            Signature signature = new Signature("308203c7308202afa003020102021500dc286b43b4ea12039958a00a6655eb84720e46c9300d06092a864886f70d01010b05003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964301e170d3137303830343136353333375a170d3437303830343136353333375a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820122300d06092a864886f70d01010105000382010f003082010a02820101008998646f47fc333db09644c303104ed183e904e351152aa66a603b77f63389d45d6fcffae3c94fadf1f28038e265d697fea347327f9081a7f0b9074d5b148db5bf357c611a77f87f844a15068818bdcd5b21d187e93fa2551676170eedce04a150c35ec0a791eef507fa9b406573c36f6f207764842e5677e35a281a422659e91e26eb4fecfb053b5c936d0976c37f8757adb57a37953da5844ea350695854d343a61ad341b63a1c425d22855af7ebfee018e1736cee98536be5b9947f288e2a26f99eb9f91b5de93fecc513019d2e90f12b38610d1f02eaa81deca4ce91c19cbce36d6c3025ce2432b3d178616beafaf437c08451bc469c6bc6f4517a714a5b0203010001a350304e300c0603551d13040530030101ff301d0603551d0e0416041419a864c0f2618c67c803a23da909bc70521f269b301f0603551d2304183016801419a864c0f2618c67c803a23da909bc70521f269b300d06092a864886f70d01010b050003820101005403fc56fdefc440376a0337815002b96a15bffc2fe42de6c58f52fae4d80652e3704455b885409eef81ffbb4c44dba104b6b8e24c9e2e0e7a04338ee73baa5b71bfb4488f8e04bef3d0eaf7d43aa42b03b278c33cc1f0dd3802571624baa161d851fab37db4bc92b9094b6885dff62b400ecd81f069d56a1be1db46d8198c50c9628cdb6e38686ef640fd386775f50376f957e24ea45ed1942968f20c82f189607fdb22f11cfdfd0760a77a60ceb3416cfb3f48f13f9f83f3834a01001750a7c78bc1fd81f0b53a7c41dcba9f5a0118259d083c32bb9ebb84d645d6f6b9c31923d8ab70e7f0a25940ecc9f4945144419f86e8c421d3b99774f4b8f3d09262e7");
            for (Signature signature2 : packageInfo.signatures) {
                if (signature2.equals(signature)) {
                    return true;
                }
                String charsString = signature.toCharsString();
                String charsString2 = signature2.toCharsString();
                StringBuilder sb = new StringBuilder(String.valueOf(charsString).length() + 38 + String.valueOf(charsString2).length() + 1);
                sb.append("Signature mismatch.  Expected '");
                sb.append(charsString);
                sb.append("' got '");
                sb.append(charsString2);
                sb.append("'");
                Log.w("ARCore-SessionCreateJni", sb.toString());
            }
            return context.getPackageManager().getApplicationInfo(context.getPackageName(), 128).metaData.getBoolean("com.google.ar.core.disable_security_check", false);
        } catch (PackageManager.NameNotFoundException unused) {
        }
    }

    static int getArCoreApkVersionCode(Context context) {
        if (useProjectedApk(context)) {
            return getMinApkVersion(context);
        }
        String str = useProjectedApk(context) ? b : "com.google.ar.core";
        try {
            return context.getPackageManager().getPackageInfo(str, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            String.valueOf(str);
            Log.e("ARCore-SessionCreateJni", "Could not load application package metadata for ".concat(String.valueOf(str)), e);
            return -1;
        }
    }

    static int getMinApkVersion(Context context) {
        try {
            Bundle bundle = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128).metaData;
            if (bundle.containsKey("com.google.ar.core.min_apk_version")) {
                return bundle.getInt("com.google.ar.core.min_apk_version");
            }
            throw new RuntimeException("Application manifest must contain meta-data com.google.ar.core.min_apk_version");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not load application package metadata", e);
        }
    }

    static boolean useProjectedApk(Context context) {
        if (!(context instanceof Activity)) {
            return false;
        }
        try {
            ActivityInfo activityInfo = context.getPackageManager().getActivityInfo(((Activity) context).getComponentName(), 0);
            Field declaredField = ActivityInfo.class.getDeclaredField("requiredDisplayCategory");
            declaredField.setAccessible(true);
            String str = (String) declaredField.get(activityInfo);
            return str != null && str.contains("xr_projected");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ARCore-SessionCreateJni", "Could not load activity info", e);
            return false;
        } catch (IllegalAccessException e2) {
            e = e2;
            Log.e("ARCore-SessionCreateJni", "Could not access requiredDisplayCategory field", e);
            return false;
        } catch (NoSuchFieldException e3) {
            e = e3;
            Log.e("ARCore-SessionCreateJni", "Could not access requiredDisplayCategory field", e);
            return false;
        }
    }
}
