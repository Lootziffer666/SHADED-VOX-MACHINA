package com.google.ar.core;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

/* JADX INFO: compiled from: SetupContentResolver.java */
/* JADX INFO: loaded from: classes.dex */
final class ak {
    static PendingIntent a(Context context) throws UnavailableUserDeclinedInstallationException, UnavailableDeviceNotCompatibleException {
        try {
            Bundle bundleCall = context.getContentResolver().call(new Uri.Builder().scheme("content").authority("com.google.ar.core.services.arcorecontentprovider").path("").build(), "getSetupIntent", context.getPackageName(), (Bundle) null);
            if (bundleCall != null) {
                PendingIntent pendingIntent = (PendingIntent) bundleCall.getParcelable("intent");
                if (pendingIntent != null) {
                    return pendingIntent;
                }
                String string = bundleCall.getString("exceptionType", "");
                if (!string.isEmpty()) {
                    if (string.equals(UnavailableDeviceNotCompatibleException.class.getName())) {
                        throw new UnavailableDeviceNotCompatibleException();
                    }
                    if (string.equals(UnavailableUserDeclinedInstallationException.class.getName())) {
                        throw new UnavailableUserDeclinedInstallationException();
                    }
                    Class<? extends U> clsAsSubclass = Class.forName(string).asSubclass(RuntimeException.class);
                    String string2 = bundleCall.getString("exceptionText", null);
                    if (string2 != null) {
                        throw ((RuntimeException) clsAsSubclass.getConstructor(String.class).newInstance(string2));
                    }
                    throw ((RuntimeException) clsAsSubclass.getConstructor(new Class[0]).newInstance(new Object[0]));
                }
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.i("ARCore-SetupContentReso", "Post-install failed", e);
            return null;
        }
    }
}
