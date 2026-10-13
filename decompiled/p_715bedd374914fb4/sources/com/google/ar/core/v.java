package com.google.ar.core;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import java.util.Objects;

/* JADX INFO: compiled from: InstallActivity.java */
/* JADX INFO: loaded from: classes.dex */
final class v extends AnimatorListenerAdapter {
    final /* synthetic */ InstallActivity a;

    v(InstallActivity installActivity) {
        Objects.requireNonNull(installActivity);
        this.a = installActivity;
    }

    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
    public final void onAnimationEnd(Animator animator) {
        this.a.a();
    }
}
