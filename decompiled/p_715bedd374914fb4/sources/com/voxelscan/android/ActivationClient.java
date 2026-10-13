package com.voxelscan.android;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
final class ActivationClient {
    ActivationClient() {
    }

    static JSONObject activate(String str, String str2) throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("token", str);
        jSONObject.put("installationId", str2);
        jSONObject.put("appVersion", BuildConfig.VERSION_NAME);
        return post("/activate", jSONObject);
    }

    static JSONObject check(String str, String str2) throws Exception {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("license", str);
        jSONObject.put("installationId", str2);
        jSONObject.put("appVersion", BuildConfig.VERSION_NAME);
        return post("/license/check", jSONObject);
    }

    private static JSONObject post(String str, JSONObject jSONObject) throws Exception {
        InputStream errorStream;
        if (BuildConfig.ACTIVATION_URL.isEmpty()) {
            throw new IllegalStateException("Activation server is not configured in this build.");
        }
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(BuildConfig.ACTIVATION_URL.replaceAll("/+$", "") + str).openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setConnectTimeout(10000);
        httpURLConnection.setReadTimeout(15000);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = jSONObject.toString().getBytes(StandardCharsets.UTF_8);
        httpURLConnection.setFixedLengthStreamingMode(bytes.length);
        try {
            OutputStream outputStream = httpURLConnection.getOutputStream();
            try {
                outputStream.write(bytes);
                if (outputStream != null) {
                    outputStream.close();
                }
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    errorStream = httpURLConnection.getInputStream();
                } else {
                    errorStream = httpURLConnection.getErrorStream();
                }
                JSONObject jSONObject2 = new JSONObject(read(errorStream));
                if (responseCode < 200 || responseCode >= 300) {
                    throw new ActivationException(jSONObject2.optString("code", "server_error"), jSONObject2.optString("error", "Activation server returned " + responseCode + "."));
                }
                httpURLConnection.disconnect();
                return jSONObject2;
            } catch (Throwable th) {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (Throwable th3) {
            httpURLConnection.disconnect();
            throw th3;
        }
    }

    /* JADX WARN: Code duplicated, block: B:34:0x0039 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    private static String read(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "{}";
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                byte[] bArr = new byte[4096];
                while (true) {
                    int i = inputStream.read(bArr);
                    if (i < 0) {
                        break;
                    }
                    byteArrayOutputStream.write(bArr, 0, i);
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable th) {
                            th.addSuppressed(th);
                        }
                    }
                    throw th;
                }
                String string = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                byteArrayOutputStream.close();
                if (inputStream != null) {
                    inputStream.close();
                }
                return string;
            } catch (Throwable th2) {
                try {
                    byteArrayOutputStream.close();
                } catch (Throwable th3) {
                    th2.addSuppressed(th3);
                }
                throw th2;
            }
        } catch (Throwable th4) {
            if (inputStream != null) {
                inputStream.close();
            }
            throw th4;
        }
    }

    static final class ActivationException extends Exception {
        final String code;

        ActivationException(String str, String str2) {
            super(str2);
            this.code = str;
        }
    }
}
