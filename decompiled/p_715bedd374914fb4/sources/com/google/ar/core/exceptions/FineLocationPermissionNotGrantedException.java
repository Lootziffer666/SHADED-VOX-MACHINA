package com.google.ar.core.exceptions;

/* JADX INFO: loaded from: classes.dex */
public class FineLocationPermissionNotGrantedException extends SecurityException {
    public FineLocationPermissionNotGrantedException() {
        this("");
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public FineLocationPermissionNotGrantedException(String str) {
        super("The Android permission ACCESS_FINE_LOCATION has not been granted prior to calling Session.configure() with Geospatial mode enabled (Config.GeospatialMode.ENABLED).".concat(String.valueOf(str)));
        String.valueOf(str);
    }
}
