package com.google.ar.core;

import android.app.Activity;
import android.content.Context;
import com.google.ar.core.exceptions.FatalException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.util.function.Consumer;

/* JADX INFO: loaded from: classes.dex */
public class ArCoreApk {

    /* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
    /* JADX WARN: Unknown enum class pattern. Please report as an issue! */
    public static class Availability {
        final int nativeCode;
        public static final Availability UNKNOWN_ERROR = new a();
        public static final Availability UNKNOWN_CHECKING = new b();
        public static final Availability UNKNOWN_TIMED_OUT = new c();
        public static final Availability UNSUPPORTED_DEVICE_NOT_CAPABLE = new d();
        public static final Availability SUPPORTED_NOT_INSTALLED = new e();
        public static final Availability SUPPORTED_APK_TOO_OLD = new f();
        public static final Availability SUPPORTED_INSTALLED = new g();
        private static final /* synthetic */ Availability[] $VALUES = $values();

        private static /* synthetic */ Availability[] $values() {
            return new Availability[]{UNKNOWN_ERROR, UNKNOWN_CHECKING, UNKNOWN_TIMED_OUT, UNSUPPORTED_DEVICE_NOT_CAPABLE, SUPPORTED_NOT_INSTALLED, SUPPORTED_APK_TOO_OLD, SUPPORTED_INSTALLED};
        }

        private Availability(String str, int i, int i2) {
            super(str, i);
            this.nativeCode = i2;
        }

        /* synthetic */ Availability(String str, int i, int i2, byte[] bArr) {
            this(str, i, i2);
        }

        static Availability forNumber(int i) {
            for (Availability availability : values()) {
                if (availability.nativeCode == i) {
                    return availability;
                }
            }
            throw new FatalException(q.b((byte) 48, i, "Unexpected value for native Availability, value="));
        }

        public static Availability valueOf(String str) {
            return (Availability) Enum.valueOf(Availability.class, str);
        }

        public static Availability[] values() {
            return (Availability[]) $VALUES.clone();
        }

        public boolean isSupported() {
            return false;
        }

        public boolean isTransient() {
            return false;
        }

        public boolean isUnknown() {
            return false;
        }

        public boolean isUnsupported() {
            return false;
        }
    }

    public enum InstallBehavior {
        REQUIRED(0),
        OPTIONAL(1);

        final int nativeCode;

        InstallBehavior(int i) {
            this.nativeCode = i;
        }

        static InstallBehavior forNumber(int i) {
            for (InstallBehavior installBehavior : values()) {
                if (installBehavior.nativeCode == i) {
                    return installBehavior;
                }
            }
            throw new FatalException(q.b((byte) 51, i, "Unexpected value for native InstallBehavior, value="));
        }
    }

    public enum InstallStatus {
        INSTALLED(0),
        INSTALL_REQUESTED(1);

        final int nativeCode;

        InstallStatus(int i) {
            this.nativeCode = i;
        }

        static InstallStatus forNumber(int i) {
            for (InstallStatus installStatus : values()) {
                if (installStatus.nativeCode == i) {
                    return installStatus;
                }
            }
            throw new FatalException(q.b((byte) 49, i, "Unexpected value for native InstallStatus, value="));
        }
    }

    public enum UserMessageType {
        APPLICATION(0),
        FEATURE(1),
        USER_ALREADY_INFORMED(2);

        final int nativeCode;

        UserMessageType(int i) {
            this.nativeCode = i;
        }

        static UserMessageType forNumber(int i) {
            for (UserMessageType userMessageType : values()) {
                if (userMessageType.nativeCode == i) {
                    return userMessageType;
                }
            }
            throw new FatalException(q.b((byte) 51, i, "Unexpected value for native UserMessageType, value="));
        }
    }

    protected ArCoreApk() {
    }

    public static ArCoreApk getInstance() {
        return l.a();
    }

    public Availability checkAvailability(Context context) {
        throw new UnsupportedOperationException("Stub");
    }

    public void checkAvailabilityAsync(Context context, Consumer<Availability> consumer) {
        throw new UnsupportedOperationException("Stub");
    }

    public InstallStatus requestInstall(Activity activity, boolean z) throws UnavailableUserDeclinedInstallationException, UnavailableDeviceNotCompatibleException {
        throw new UnsupportedOperationException("Stub");
    }

    public InstallStatus requestInstall(Activity activity, boolean z, InstallBehavior installBehavior, UserMessageType userMessageType) throws UnavailableUserDeclinedInstallationException, UnavailableDeviceNotCompatibleException {
        throw new UnsupportedOperationException("Stub");
    }
}
