package com.google.ar.core;

import android.animation.ValueAnimator;
import java.util.Objects;

/* JADX INFO: compiled from: InstallActivity.java */
/* JADX INFO: loaded from: classes.dex */
final class u implements ValueAnimator.AnimatorUpdateListener {
    final /* synthetic */ int a;
    final /* synthetic */ int b;
    final /* synthetic */ int c;
    final /* synthetic */ InstallActivity d;

    u(InstallActivity installActivity, int i, int i2, int i3) {
        this.a = i;
        this.b = i2;
        this.c = i3;
        Objects.requireNonNull(installActivity);
        this.d = installActivity;
    }

    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        float animatedFraction = 1.0f - valueAnimator.getAnimatedFraction();
        float animatedFraction2 = this.b * valueAnimator.getAnimatedFraction();
        InstallActivity installActivity = this.d;
        installActivity.getWindow().setLayout((int) ((this.a * animatedFraction) + animatedFraction2), (int) ((this.c * animatedFraction) + animatedFraction2));
        installActivity.getWindow().getDecorView().refreshDrawableState();
    }
}
