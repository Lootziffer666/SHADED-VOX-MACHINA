package com.voxelscan.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/* JADX INFO: loaded from: classes.dex */
public class MainActivity extends Activity {
    private static final int CAMERA_PERMISSION = 42;
    private static final int OPEN_JSON_REQUEST = 77;
    private static final long QUICK_INFO_DURATION_MS = 550;
    private boolean appUiStarted;
    private Button autoAxisButton;
    private Button axisButton;
    private Button blocksButton;
    private LinearLayout controls;
    private TextView crosshair;
    private Button datasetButton;
    private Button depthButton;
    private Runnable dismissQuickInfo;
    private Button floorButton;
    private boolean installRequested;
    private TestLicenseManager licenseManager;
    private Button modeButton;
    private Button paletteButton;
    private int paletteIndex;
    private Button pauseButton;
    private Toast quickInfoToast;
    private VoxelScanRenderer renderer;
    private Button reviewButton;
    private Button scanButton;
    private Session session;
    private Button sizeButton;
    private TextView status;
    private Button uploadButton;
    private Button wallButton;
    private final float[] voxelSizes = {0.05f, 0.1f, 0.2f};
    private final float[] voxelMaxDepths = {5.0f, 5.0f, 5.0f};
    private final float[] depthOptions = {2.0f, 5.0f, 8.0f, 15.0f, 20.0f};
    private final int[] paletteSizes = {0, 8, 16, 32, 64};
    private int voxelSizeIndex = 0;
    private int depthIndex = 1;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        TestLicenseManager testLicenseManager = new TestLicenseManager(this);
        this.licenseManager = testLicenseManager;
        if (!testLicenseManager.isConfigured()) {
            buildActivationUi("This VoxelScan Test build has no activation configuration.");
            return;
        }
        if (this.licenseManager.hasUsableOfflineLicense()) {
            startApplicationUi();
            checkLicenseWhenDue();
        } else if (this.licenseManager.hasValidStoredLicense()) {
            buildLicenseVerificationUi("Connect to verify this VoxelScan license.");
        } else {
            buildActivationUi("");
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.appUiStarted && this.renderer != null && ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") == 0) {
            tryResumeArCore();
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        VoxelScanRenderer voxelScanRenderer = this.renderer;
        if (voxelScanRenderer != null) {
            voxelScanRenderer.onPause();
        }
        Session session = this.session;
        if (session != null) {
            session.pause();
        }
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        Runnable runnable = this.dismissQuickInfo;
        if (runnable != null) {
            this.uiHandler.removeCallbacks(runnable);
        }
        Toast toast = this.quickInfoToast;
        if (toast != null) {
            toast.cancel();
        }
        Session session = this.session;
        if (session != null) {
            session.close();
        }
        super.onDestroy();
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == 42 && iArr.length > 0 && iArr[0] == 0) {
            if (this.appUiStarted) {
                tryResumeArCore();
            }
        } else {
            TextView textView = this.status;
            if (textView != null) {
                textView.setText("Camera permission denied. VoxelScan needs the rear camera for ARCore depth.");
            }
        }
    }

    private void startApplicationUi() {
        if (this.appUiStarted) {
            return;
        }
        this.appUiStarted = true;
        buildUi();
        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != 0) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 42);
        } else {
            tryResumeArCore();
        }
    }

    private void buildActivationUi(String str) {
        this.appUiStarted = false;
        LinearLayout linearLayoutActivationRoot = activationRoot();
        TextView textViewActivationText = activationText("VOXELSCAN", 30.0f, -1);
        textViewActivationText.setTypeface(Typeface.DEFAULT, 1);
        linearLayoutActivationRoot.addView(textViewActivationText);
        TextView textViewActivationText2 = activationText("Private Test Build", 16.0f, -10102607);
        textViewActivationText2.setPadding(0, dp(4), 0, dp(28));
        linearLayoutActivationRoot.addView(textViewActivationText2);
        final EditText editText = new EditText(this);
        editText.setHint("VS-XXXX-XXXX-XXXX-XXXX");
        editText.setSingleLine(true);
        editText.setTextColor(-1);
        editText.setHintTextColor(-9469563);
        editText.setTextSize(17.0f);
        editText.setAllCaps(true);
        editText.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-15656677);
        gradientDrawable.setCornerRadius(dp(6));
        gradientDrawable.setStroke(dp(1), -12364197);
        editText.setBackground(gradientDrawable);
        linearLayoutActivationRoot.addView(editText, new LinearLayout.LayoutParams(-1, dp(52)));
        final TextView textViewActivationText3 = activationText(str, 14.0f, -4141616);
        textViewActivationText3.setPadding(0, dp(14), 0, dp(8));
        linearLayoutActivationRoot.addView(textViewActivationText3);
        final Button button = new Button(this);
        button.setText("ACTIVATE");
        styleButton(button, -15304101);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(58));
        layoutParams.topMargin = dp(12);
        linearLayoutActivationRoot.addView(button, layoutParams);
        button.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildActivationUi$2(editText, textViewActivationText3, button, view);
            }
        });
        setContentView(centerActivationCard(linearLayoutActivationRoot));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildActivationUi$2(EditText editText, final TextView textView, final Button button, View view) {
        String strTrim = editText.getText().toString().trim();
        if (strTrim.isEmpty()) {
            textView.setText("Enter the activation code from the download page.");
            return;
        }
        button.setEnabled(false);
        button.setText("ACTIVATING...");
        textView.setText("Contacting the activation server...");
        this.licenseManager.activate(strTrim, new TestLicenseManager.Callback() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda38
            @Override // com.voxelscan.android.TestLicenseManager.Callback
            public final void complete(boolean z, String str) {
                this.f$0.lambda$buildActivationUi$1(button, textView, z, str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildActivationUi$1(final Button button, final TextView textView, final boolean z, final String str) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$buildActivationUi$0(button, textView, str, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildActivationUi$0(Button button, TextView textView, String str, boolean z) {
        button.setEnabled(true);
        button.setText("ACTIVATE");
        textView.setText(str);
        if (z) {
            startApplicationUi();
        }
    }

    private void buildLicenseVerificationUi(String str) {
        this.appUiStarted = false;
        LinearLayout linearLayoutActivationRoot = activationRoot();
        TextView textViewActivationText = activationText("VOXELSCAN", 30.0f, -1);
        textViewActivationText.setTypeface(Typeface.DEFAULT, 1);
        linearLayoutActivationRoot.addView(textViewActivationText);
        linearLayoutActivationRoot.addView(activationText("License verification", 16.0f, -10102607));
        final TextView textViewActivationText2 = activationText(str, 14.0f, -4141616);
        textViewActivationText2.setPadding(0, dp(24), 0, dp(12));
        linearLayoutActivationRoot.addView(textViewActivationText2);
        final Button button = new Button(this);
        button.setText("VERIFY LICENSE");
        styleButton(button, -15304101);
        linearLayoutActivationRoot.addView(button, new LinearLayout.LayoutParams(-1, dp(58)));
        button.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda45
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildLicenseVerificationUi$5(button, textViewActivationText2, view);
            }
        });
        setContentView(centerActivationCard(linearLayoutActivationRoot));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildLicenseVerificationUi$5(final Button button, final TextView textView, View view) {
        button.setEnabled(false);
        button.setText("VERIFYING...");
        this.licenseManager.checkOnline(new TestLicenseManager.Callback() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda44
            @Override // com.voxelscan.android.TestLicenseManager.Callback
            public final void complete(boolean z, String str) {
                this.f$0.lambda$buildLicenseVerificationUi$4(button, textView, z, str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildLicenseVerificationUi$4(final Button button, final TextView textView, final boolean z, final String str) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$buildLicenseVerificationUi$3(button, textView, str, z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildLicenseVerificationUi$3(Button button, TextView textView, String str, boolean z) {
        button.setEnabled(true);
        button.setText("VERIFY LICENSE");
        textView.setText(str);
        if (z) {
            startApplicationUi();
        } else {
            if (this.licenseManager.hasValidStoredLicense()) {
                return;
            }
            buildActivationUi(str);
        }
    }

    private void checkLicenseWhenDue() {
        TestLicenseManager testLicenseManager = this.licenseManager;
        if (testLicenseManager == null || !testLicenseManager.shouldCheckOnline()) {
            return;
        }
        this.licenseManager.checkOnline(new TestLicenseManager.Callback() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda15
            @Override // com.voxelscan.android.TestLicenseManager.Callback
            public final void complete(boolean z, String str) {
                this.f$0.lambda$checkLicenseWhenDue$7(z, str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkLicenseWhenDue$7(final boolean z, final String str) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda39
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$checkLicenseWhenDue$6(z, str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkLicenseWhenDue$6(boolean z, String str) {
        if (z || this.licenseManager.hasUsableOfflineLicense()) {
            return;
        }
        VoxelScanRenderer voxelScanRenderer = this.renderer;
        if (voxelScanRenderer != null) {
            voxelScanRenderer.onPause();
        }
        Session session = this.session;
        if (session != null) {
            session.pause();
            this.session.close();
            this.session = null;
        }
        this.renderer = null;
        buildActivationUi(str);
    }

    private LinearLayout activationRoot() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(26), dp(28), dp(26), dp(28));
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-14998232);
        gradientDrawable.setCornerRadius(dp(8));
        gradientDrawable.setStroke(dp(1), -13351349);
        linearLayout.setBackground(gradientDrawable);
        return linearLayout;
    }

    private View centerActivationCard(View view) {
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackgroundColor(-15722727);
        frameLayout.addView(view, new FrameLayout.LayoutParams(Math.min(dp(440), getResources().getDisplayMetrics().widthPixels - dp(32)), -2, 17));
        return frameLayout;
    }

    private TextView activationText(String str, float f, int i) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(f);
        textView.setTextColor(i);
        return textView;
    }

    private void buildUi() {
        FrameLayout frameLayout = new FrameLayout(this);
        VoxelScanRenderer voxelScanRenderer = new VoxelScanRenderer(this);
        this.renderer = voxelScanRenderer;
        frameLayout.addView(voxelScanRenderer.surfaceView, new FrameLayout.LayoutParams(-1, -1));
        TextView textView = new TextView(this);
        this.status = textView;
        textView.setTextColor(-1182988);
        this.status.setTextSize(13.0f);
        this.status.setPadding(18, 16, 18, 16);
        this.status.setBackgroundColor(-1442379252);
        frameLayout.addView(this.status, new FrameLayout.LayoutParams(-1, -2, 48));
        LinearLayout linearLayout = new LinearLayout(this);
        this.controls = linearLayout;
        linearLayout.setOrientation(1);
        this.controls.setGravity(17);
        this.controls.setPadding(12, 12, 12, 24);
        this.controls.setBackgroundColor(-871953908);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(0);
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(0);
        Button button = new Button(this);
        this.scanButton = button;
        button.setText("START");
        this.scanButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$8(view);
            }
        });
        linearLayout2.addView(this.scanButton, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button button2 = new Button(this);
        this.modeButton = button2;
        button2.setText("INTERIOR");
        this.modeButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda27
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$9(view);
            }
        });
        Button button3 = new Button(this);
        button3.setText("NEW");
        button3.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda28
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$10(view);
            }
        });
        linearLayout2.addView(button3, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button button4 = new Button(this);
        this.sizeButton = button4;
        button4.setText("SIZE 5CM");
        this.sizeButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda29
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$11(view);
            }
        });
        LinearLayout linearLayout5 = new LinearLayout(this);
        linearLayout5.setOrientation(0);
        linearLayout5.addView(this.sizeButton, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button button5 = new Button(this);
        this.depthButton = button5;
        button5.setText("DEPTH 5M");
        this.depthButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda30
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$12(view);
            }
        });
        Button button6 = new Button(this);
        this.reviewButton = button6;
        button6.setText("REVIEW");
        this.reviewButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda31
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$13(view);
            }
        });
        linearLayout3.addView(this.reviewButton, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button button7 = new Button(this);
        this.axisButton = button7;
        button7.setText("AXIS");
        this.axisButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda32
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$14(view);
            }
        });
        linearLayout5.addView(this.axisButton, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button button8 = new Button(this);
        this.autoAxisButton = button8;
        button8.setText("AUTO AXIS");
        this.autoAxisButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda34
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$15(view);
            }
        });
        linearLayout3.addView(this.autoAxisButton, new LinearLayout.LayoutParams(0, -2, 1.0f));
        new LinearLayout(this).setOrientation(0);
        Button button9 = new Button(this);
        this.floorButton = button9;
        button9.setText("FLOOR");
        this.floorButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda35
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$16(view);
            }
        });
        Button button10 = new Button(this);
        this.wallButton = button10;
        button10.setText("WALL");
        this.wallButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda36
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$17(view);
            }
        });
        Button button11 = new Button(this);
        this.blocksButton = button11;
        button11.setText("VIEW: VOXELS");
        this.blocksButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda18
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$18(view);
            }
        });
        linearLayout4.addView(this.blocksButton, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button button12 = new Button(this);
        this.paletteButton = button12;
        button12.setText("PAL: RAW");
        this.paletteButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda19
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$19(view);
            }
        });
        Button button13 = new Button(this);
        button13.setText("CLEAN");
        button13.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$22(view);
            }
        });
        linearLayout4.addView(button13, new LinearLayout.LayoutParams(0, -2, 1.0f));
        LinearLayout linearLayout6 = new LinearLayout(this);
        linearLayout6.setOrientation(0);
        Button button14 = new Button(this);
        button14.setText("FILES");
        button14.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$23(view);
            }
        });
        linearLayout6.addView(button14, new LinearLayout.LayoutParams(0, -2, 1.0f));
        Button button15 = new Button(this);
        this.datasetButton = button15;
        button15.setText("DATASET");
        this.datasetButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda23
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$24(view);
            }
        });
        styleButton(this.scanButton, -15304101);
        styleButton(this.modeButton, -13541498);
        styleButton(button3, -5882038);
        styleButton(this.sizeButton, -12232092);
        styleButton(this.depthButton, -10984305);
        styleButton(this.axisButton, -7706840);
        styleButton(this.reviewButton, -14124147);
        styleButton(this.autoAxisButton, -13275268);
        styleButton(this.floorButton, -13076387);
        styleButton(this.wallButton, -6791113);
        styleButton(this.blocksButton, -13537418);
        styleButton(this.paletteButton, -10070646);
        styleButton(button13, -8236990);
        styleButton(button14, -11773599);
        styleButton(this.datasetButton, -6602904);
        Button button16 = this.uploadButton;
        if (button16 != null) {
            styleButton(button16, -10984305);
        }
        this.controls.addView(linearLayout2, new LinearLayout.LayoutParams(-1, -2));
        this.controls.addView(linearLayout5, new LinearLayout.LayoutParams(-1, -2));
        this.controls.addView(linearLayout3, new LinearLayout.LayoutParams(-1, -2));
        this.controls.addView(linearLayout4, new LinearLayout.LayoutParams(-1, -2));
        this.controls.addView(linearLayout6, new LinearLayout.LayoutParams(-1, -2));
        frameLayout.addView(this.controls, new FrameLayout.LayoutParams(-1, -2, 80));
        TextView textView2 = new TextView(this);
        this.crosshair = textView2;
        textView2.setText("+");
        this.crosshair.setTextColor(-1);
        this.crosshair.setTextSize(28.0f);
        this.crosshair.setGravity(17);
        this.crosshair.setShadowLayer(4.0f, 0.0f, 0.0f, ViewCompat.MEASURED_STATE_MASK);
        frameLayout.addView(this.crosshair, new FrameLayout.LayoutParams(72, 72, 17));
        Button button17 = new Button(this);
        this.pauseButton = button17;
        button17.setText("PAUSE");
        this.pauseButton.setAlpha(0.82f);
        this.pauseButton.setVisibility(8);
        this.pauseButton.setOnClickListener(new View.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda24
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$26(view);
            }
        });
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2, 8388693);
        layoutParams.setMargins(16, 16, 20, 24);
        frameLayout.addView(this.pauseButton, layoutParams);
        styleButton(this.pauseButton, -5882038);
        setContentView(frameLayout);
        this.renderer.setStatsListener(new VoxelScanRenderer.StatsListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda25
            @Override // com.voxelscan.android.VoxelScanRenderer.StatsListener
            public final void onStats(String str) {
                this.f$0.lambda$buildUi$28(str);
            }
        });
        this.renderer.setTrackingIssueListener(new VoxelScanRenderer.TrackingIssueListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda26
            @Override // com.voxelscan.android.VoxelScanRenderer.TrackingIssueListener
            public final void onTrackingIssue(String str) {
                this.f$0.lambda$buildUi$30(str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$8(View view) {
        toggleScan();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$9(View view) {
        toggleScanMode();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$10(View view) {
        confirmNewScan();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$11(View view) {
        cycleVoxelSize();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$12(View view) {
        cycleDepth();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$13(View view) {
        toggleReview();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$14(View view) {
        setAxis();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$15(View view) {
        setAutoAxis();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$16(View view) {
        snapFloor();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$17(View view) {
        snapWall();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$18(View view) {
        toggleBlocks();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$19(View view) {
        cycleReviewPalette();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$22(View view) {
        this.renderer.cleanWorld(new VoxelScanRenderer.CleanCallback() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda48
            @Override // com.voxelscan.android.VoxelScanRenderer.CleanCallback
            public final void onCleaned(int i) {
                this.f$0.lambda$buildUi$21(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$20(int i) {
        showQuickInfo("Removed " + i + " noisy voxels");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$21(final int i) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda41
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$buildUi$20(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$23(View view) {
        showFilesMenu();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$24(View view) {
        toggleDatasetRecording();
    }

    private /* synthetic */ void lambda$buildUi$25(View view) {
        uploadLastDataset();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$26(View view) {
        pauseScan();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$27(String str) {
        this.status.setText(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$28(final String str) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$buildUi$27(str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$30(final String str) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$buildUi$29(str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$29(String str) {
        this.scanButton.setText(this.renderer.isDatasetRecording() ? "CAPTURE" : "START");
        this.reviewButton.setText("REVIEW");
        setScanningChrome(false);
        showQuickInfo(str);
    }

    private void tryResumeArCore() {
        try {
            if (ArCoreApk.getInstance().requestInstall(this, !this.installRequested) == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                this.installRequested = true;
                return;
            }
            if (this.session == null) {
                Session session = new Session(this);
                this.session = session;
                Config config = session.getConfig();
                if (!this.session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY) && !this.session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    this.status.setText("This phone supports ARCore, but not ARCore Depth API. Use a Depth-supported Android model.");
                    return;
                }
                if (this.session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)) {
                    config.setDepthMode(Config.DepthMode.RAW_DEPTH_ONLY);
                } else {
                    config.setDepthMode(Config.DepthMode.AUTOMATIC);
                }
                configureArSession(config);
                this.renderer.setSession(this.session);
            }
            this.session.resume();
            this.renderer.onResume();
            this.status.setText("Ready. Move the phone slowly; ARCore depth improves with motion.");
        } catch (CameraNotAvailableException | UnavailableException | SecurityException e) {
            this.status.setText("ARCore unavailable: " + e.getMessage());
        }
    }

    private void toggleScan() {
        boolean z = this.renderer.toggleScanning();
        this.scanButton.setText(this.renderer.isDatasetRecording() ? "CAPTURE" : "START");
        this.reviewButton.setText("REVIEW");
        setScanningChrome(z);
    }

    private void pauseScan() {
        if (this.renderer.isScanning()) {
            this.renderer.toggleScanning();
        }
        this.scanButton.setText(this.renderer.isDatasetRecording() ? "CAPTURE" : "START");
        setScanningChrome(false);
    }

    private void confirmNewScan() {
        if (this.renderer.isDatasetRecording() || this.renderer.isDatasetFinalizing()) {
            showQuickInfo("Stop and save DATASET before NEW.");
        } else if (!this.renderer.hasReconstructionData()) {
            showQuickInfo("Scene is already empty.");
        } else {
            new AlertDialog.Builder(this).setTitle("New scan?").setMessage("This clears the current voxel scene. Export it first if you want to keep it.").setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton("NEW", new DialogInterface.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda46
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    this.f$0.lambda$confirmNewScan$33(dialogInterface, i);
                }
            }).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmNewScan$32() {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda42
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$confirmNewScan$31();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmNewScan$33(DialogInterface dialogInterface, int i) {
        this.renderer.resetWorld(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda47
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$confirmNewScan$32();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$confirmNewScan$31() {
        this.reviewButton.setText("REVIEW");
        this.scanButton.setText("START");
        this.paletteIndex = 0;
        this.paletteButton.setText("PAL: RAW");
        setScanningChrome(false);
        showQuickInfo("New empty scene ready.");
    }

    private void toggleReview() {
        if (this.renderer.isDatasetRecording() || this.renderer.isDatasetFinalizing()) {
            showQuickInfo("Stop and save DATASET before REVIEW.");
            return;
        }
        if (this.renderer.isReviewMode()) {
            boolean zExitReview = this.renderer.exitReview(true);
            this.reviewButton.setText("REVIEW");
            this.scanButton.setText("START");
            setScanningChrome(zExitReview);
            return;
        }
        this.renderer.enterReview();
        this.reviewButton.setText("RESUME");
        this.scanButton.setText("START");
        setScanningChrome(false);
        this.crosshair.setVisibility(4);
    }

    private void setScanningChrome(boolean z) {
        this.controls.setVisibility(z ? 8 : 0);
        this.status.setVisibility(0);
        this.pauseButton.setVisibility(z ? 0 : 8);
        this.crosshair.setVisibility(this.renderer.isReviewMode() ? 4 : 0);
    }

    private void cycleVoxelSize() {
        int i = this.voxelSizeIndex + 1;
        float[] fArr = this.voxelSizes;
        int length = i % fArr.length;
        float f = fArr[length];
        if (this.renderer.setVoxelSize(f)) {
            this.voxelSizeIndex = length;
            float f2 = f * 100.0f;
            this.sizeButton.setText("SIZE " + Math.round(f2) + "CM");
            float f3 = this.voxelMaxDepths[this.voxelSizeIndex];
            if (this.renderer.maxScanDepth() > f3) {
                this.renderer.setMaxScanDepth(f3);
                this.depthIndex = indexOfDepth(f3);
                updateDepthLabel();
            }
            showQuickInfo("Voxel size: " + Math.round(f2) + " cm");
            return;
        }
        Toast.makeText(this, "Voxel size can only change before scanning.", 1).show();
    }

    private void cycleDepth() {
        float f;
        float f2 = this.voxelMaxDepths[this.voxelSizeIndex];
        int length = this.depthIndex;
        do {
            float[] fArr = this.depthOptions;
            length = (length + 1) % fArr.length;
            f = fArr[length];
        } while (f > f2);
        this.depthIndex = length;
        this.renderer.setMaxScanDepth(f);
        updateDepthLabel();
        showQuickInfo("Scan depth: " + Math.round(this.depthOptions[this.depthIndex]) + " m (max for this voxel: " + Math.round(f2) + " m)");
    }

    private int indexOfDepth(float f) {
        int i = 0;
        while (true) {
            float[] fArr = this.depthOptions;
            if (i >= fArr.length) {
                return 0;
            }
            if (fArr[i] == f) {
                return i;
            }
            i++;
        }
    }

    private void updateDepthLabel() {
        this.depthButton.setText("DEPTH " + Math.round(this.renderer.maxScanDepth()) + "M");
    }

    private void toggleScanMode() {
        this.modeButton.setText(this.renderer.toggleScanMode().name());
        Session session = this.session;
        if (session != null) {
            try {
                configureArSession(session.getConfig());
            } catch (Exception e) {
                Toast.makeText(this, "Mode changed, but AR config update failed: " + e.getMessage(), 1).show();
            }
        }
    }

    private void configureArSession(Config config) {
        config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
        if (this.renderer.scanMode() == VoxelScanRenderer.ScanMode.EXTERIOR && this.session.isSemanticModeSupported(Config.SemanticMode.ENABLED)) {
            config.setSemanticMode(Config.SemanticMode.ENABLED);
        } else {
            config.setSemanticMode(Config.SemanticMode.DISABLED);
        }
        this.session.configure(config);
    }

    private void setAxis() {
        if (this.renderer.setGridAxisFromCurrentCamera()) {
            showQuickInfo("Grid Z axis set: " + Math.round(this.renderer.axisYawDegrees()) + " deg");
        } else if (this.renderer.voxelCount() > 0) {
            Toast.makeText(this, "Set AXIS before scanning. Existing voxels keep their current grid.", 1).show();
        } else {
            Toast.makeText(this, "Move phone until ARCore tracking is stable, then set AXIS.", 1).show();
        }
    }

    private void setAutoAxis() {
        if (this.renderer.setGridAxisFromBestWall()) {
            showQuickInfo("Grid axis aligned to detected wall.");
        } else if (this.renderer.voxelCount() > 0) {
            Toast.makeText(this, "Use AUTO AXIS before scanning. Existing voxels keep their grid.", 1).show();
        } else {
            Toast.makeText(this, "No stable wall plane yet. Point at a textured wall and move slowly.", 1).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$snapFloor$35(final boolean z, final int i) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda43
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$snapFloor$34(z, i);
            }
        });
    }

    private void snapFloor() {
        this.renderer.markSurfaceFromCrosshair(VoxelScanRenderer.SurfaceType.FLOOR, new VoxelScanRenderer.SnapCallback() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda13
            @Override // com.voxelscan.android.VoxelScanRenderer.SnapCallback
            public final void onSnapped(boolean z, int i) {
                this.f$0.lambda$snapFloor$35(z, i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$snapFloor$34(boolean z, int i) {
        String str;
        if (z) {
            str = "Manual floor set. Adjusted " + i + " voxels.";
        } else {
            str = "No geometry under the crosshair. Move slowly and try again.";
        }
        showQuickInfo(str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$snapWall$37(final boolean z, final int i) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$snapWall$36(z, i);
            }
        });
    }

    private void snapWall() {
        this.renderer.markSurfaceFromCrosshair(VoxelScanRenderer.SurfaceType.WALL, new VoxelScanRenderer.SnapCallback() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda37
            @Override // com.voxelscan.android.VoxelScanRenderer.SnapCallback
            public final void onSnapped(boolean z, int i) {
                this.f$0.lambda$snapWall$37(z, i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$snapWall$36(boolean z, int i) {
        String str;
        if (z) {
            str = "Manual wall set. Adjusted " + i + " voxels.";
        } else {
            str = "No geometry under the crosshair. Move slowly and try again.";
        }
        showQuickInfo(str);
    }

    private void toggleBlocks() {
        this.blocksButton.setText(this.renderer.toggleBlockMode() ? "VIEW: VOXELS" : "VIEW: DOTS");
    }

    private void cycleReviewPalette() {
        if (!this.renderer.isReviewMode()) {
            showQuickInfo("Open REVIEW to preview a reduced color palette.");
            return;
        }
        if (this.renderer.voxelCount() == 0) {
            showQuickInfo("Scan or open a scene before creating a palette.");
            return;
        }
        int i = this.paletteIndex + 1;
        int[] iArr = this.paletteSizes;
        final int length = i % iArr.length;
        final int i2 = iArr[length];
        this.paletteButton.setEnabled(false);
        this.renderer.setReviewPaletteSize(i2, new VoxelScanRenderer.PaletteCallback() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda14
            @Override // com.voxelscan.android.VoxelScanRenderer.PaletteCallback
            public final void onPaletteReady(int i3, String str) {
                this.f$0.lambda$cycleReviewPalette$39(length, i2, i3, str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$cycleReviewPalette$39(final int i, final int i2, final int i3, final String str) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$cycleReviewPalette$38(str, i, i2, i3);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$cycleReviewPalette$38(String str, int i, int i2, int i3) {
        String str2;
        this.paletteButton.setEnabled(true);
        if (str != null) {
            Toast.makeText(this, "Palette failed: " + str, 1).show();
            return;
        }
        this.paletteIndex = i;
        this.paletteButton.setText(i2 == 0 ? "PAL: RAW" : "PAL: " + i2);
        if (i2 == 0) {
            str2 = "Original scan colors restored.";
        } else {
            str2 = "Review reduced to " + i3 + " colors.";
        }
        showQuickInfo(str2);
    }

    private void showFilesMenu() {
        new AlertDialog.Builder(this).setTitle("VoxelScan files").setItems(new String[]{"EXPORT VOX"}, new DialogInterface.OnClickListener() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda10
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.lambda$showFilesMenu$40(dialogInterface, i);
            }
        }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showFilesMenu$40(DialogInterface dialogInterface, int i) {
        exportVox();
    }

    private /* synthetic */ void lambda$showFilesMenu$41(DialogInterface dialogInterface, int i) {
        if (i == 0) {
            openJsonPicker();
            return;
        }
        if (i == 1) {
            exportJson();
        } else if (i == 2) {
            exportVox();
        } else {
            shareLastDataset();
        }
    }

    private void toggleDatasetRecording() {
        if (this.renderer.isDatasetFinalizing()) {
            showQuickInfo("Dataset is still being saved.");
            return;
        }
        if (this.renderer.isDatasetRecording()) {
            this.datasetButton.setEnabled(false);
            this.datasetButton.setText("SAVING...");
            if (this.renderer.stopDatasetRecording(new DatasetRecorder.StopCallback() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda4
                @Override // com.voxelscan.android.DatasetRecorder.StopCallback
                public final void onStopped(File file, int i, int i2, String str) {
                    this.f$0.lambda$toggleDatasetRecording$43(file, i, i2, str);
                }
            })) {
                return;
            }
            this.datasetButton.setEnabled(true);
            this.datasetButton.setText("STOP DATA");
            return;
        }
        if (this.renderer.isReviewMode()) {
            showQuickInfo("Exit REVIEW before recording a dataset.");
            return;
        }
        if (this.renderer.startDatasetRecording()) {
            this.datasetButton.setText("STOP DATA");
            this.scanButton.setText("CAPTURE");
            setDatasetControlsLocked(true);
            showQuickInfo("Dataset armed. Press CAPTURE and move slowly.");
            return;
        }
        Toast.makeText(this, "Could not start dataset recording.", 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$toggleDatasetRecording$43(final File file, final int i, final int i2, final String str) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$toggleDatasetRecording$42(str, file, i, i2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$toggleDatasetRecording$42(String str, File file, int i, int i2) {
        this.datasetButton.setEnabled(true);
        this.datasetButton.setText("DATASET");
        this.scanButton.setText("START");
        setDatasetControlsLocked(false);
        if (str != null || file == null) {
            Toast.makeText(this, "Dataset save failed: " + str, 1).show();
        } else if (i <= 0) {
            Toast.makeText(this, "No frames recorded. Press DATASET, then START, and scan until the REC counter increases.", 1).show();
        } else {
            showQuickInfo("Saved " + i + " frames" + (i2 > 0 ? ", dropped " + i2 : ""));
            shareDataset(file);
        }
    }

    private void setDatasetControlsLocked(boolean z) {
        this.modeButton.setEnabled(!z);
        this.sizeButton.setEnabled(!z);
        this.depthButton.setEnabled(!z);
        this.axisButton.setEnabled(!z);
        this.autoAxisButton.setEnabled(!z);
    }

    private void shareLastDataset() {
        File fileLastDatasetFile = this.renderer.lastDatasetFile();
        if (fileLastDatasetFile == null) {
            showQuickInfo("No saved dataset yet.");
        } else {
            shareDataset(fileLastDatasetFile);
        }
    }

    private void shareDataset(File file) {
        Uri uriForFile = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("application/zip");
        intent.putExtra("android.intent.extra.STREAM", uriForFile);
        intent.addFlags(1);
        startActivity(Intent.createChooser(intent, "Share VoxelScan dataset"));
    }

    private void uploadLastDataset() {
        if (this.renderer.isDatasetRecording() || this.renderer.isDatasetFinalizing()) {
            showQuickInfo("Stop and save DATASET before upload.");
            return;
        }
        File fileLastDatasetFile = this.renderer.lastDatasetFile();
        if (fileLastDatasetFile == null) {
            showQuickInfo("No saved dataset yet.");
        } else {
            uploadDataset(fileLastDatasetFile);
        }
    }

    private void uploadDataset(File file) {
        this.uploadButton.setEnabled(false);
        this.uploadButton.setText("UPLOADING...");
        DatasetUploader.upload(file, new DatasetUploader.Callback() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda1
            @Override // com.voxelscan.android.DatasetUploader.Callback
            public final void onComplete(boolean z, String str) {
                this.f$0.lambda$uploadDataset$45(z, str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$uploadDataset$45(final boolean z, final String str) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$uploadDataset$44(z, str);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$uploadDataset$44(boolean z, String str) {
        this.uploadButton.setEnabled(true);
        this.uploadButton.setText("UPLOAD");
        if (z) {
            showQuickInfo(str);
        } else {
            Toast.makeText(this, "Upload failed; file kept for retry: " + str, 1).show();
        }
    }

    private void openJsonPicker() {
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("application/json");
        startActivityForResult(intent, OPEN_JSON_REQUEST);
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i != OPEN_JSON_REQUEST || i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        final Uri data = intent.getData();
        showQuickInfo("Loading scan...");
        new Thread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onActivityResult$46(data);
            }
        }, "VoxelScan-JSON-loader").start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: loadJsonUri, reason: merged with bridge method [inline-methods] */
    public void lambda$onActivityResult$46(Uri uri) {
        try {
            InputStream inputStreamOpenInputStream = getContentResolver().openInputStream(uri);
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    if (inputStreamOpenInputStream == null) {
                        throw new IllegalStateException("Cannot open selected file.");
                    }
                    byte[] bArr = new byte[16384];
                    while (true) {
                        int i = inputStreamOpenInputStream.read(bArr);
                        if (i < 0) {
                            break;
                        } else {
                            byteArrayOutputStream.write(bArr, 0, i);
                        }
                    }
                    this.renderer.replaceWorldFromJson(byteArrayOutputStream.toString(StandardCharsets.UTF_8.name()), new VoxelScanRenderer.LoadCallback() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda22
                        @Override // com.voxelscan.android.VoxelScanRenderer.LoadCallback
                        public final void onLoaded(int i2, String str) {
                            this.f$0.lambda$loadJsonUri$48(i2, str);
                        }
                    });
                    byteArrayOutputStream.close();
                    if (inputStreamOpenInputStream != null) {
                        inputStreamOpenInputStream.close();
                    }
                } catch (Throwable th) {
                    try {
                        byteArrayOutputStream.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                if (inputStreamOpenInputStream != null) {
                    try {
                        inputStreamOpenInputStream.close();
                    } catch (Throwable th4) {
                        th3.addSuppressed(th4);
                    }
                }
                throw th3;
            }
        } catch (Exception e) {
            runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda33
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.lambda$loadJsonUri$49(e);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadJsonUri$48(final int i, final String str) {
        runOnUiThread(new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$loadJsonUri$47(str, i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadJsonUri$47(String str, int i) {
        if (str != null) {
            Toast.makeText(this, "Open failed: " + str, 1).show();
        } else {
            syncControlsAfterImport();
            showQuickInfo("Loaded " + i + " voxels.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadJsonUri$49(Exception exc) {
        Toast.makeText(this, "Open failed: " + exc.getMessage(), 1).show();
    }

    private void syncControlsAfterImport() {
        float fVoxelSize = this.renderer.voxelSize();
        float f = Float.MAX_VALUE;
        int i = 0;
        int i2 = 0;
        while (true) {
            float[] fArr = this.voxelSizes;
            if (i < fArr.length) {
                float fAbs = Math.abs(fArr[i] - fVoxelSize);
                if (fAbs < f) {
                    i2 = i;
                    f = fAbs;
                }
                i++;
            } else {
                this.voxelSizeIndex = i2;
                this.sizeButton.setText("SIZE " + Math.round(fVoxelSize * 100.0f) + "CM");
                float f2 = this.voxelMaxDepths[this.voxelSizeIndex];
                VoxelScanRenderer voxelScanRenderer = this.renderer;
                voxelScanRenderer.setMaxScanDepth(Math.min(voxelScanRenderer.maxScanDepth(), f2));
                this.depthIndex = indexOfDepth(this.renderer.maxScanDepth());
                updateDepthLabel();
                this.reviewButton.setText("RESUME");
                this.scanButton.setText("START");
                setScanningChrome(false);
                this.crosshair.setVisibility(4);
                return;
            }
        }
    }

    private void styleButton(Button button, int i) {
        button.setTextColor(-1);
        button.setTextSize(13.0f);
        button.setTypeface(Typeface.DEFAULT, 1);
        button.setGravity(17);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(48));
        button.setPadding(dp(8), dp(7), dp(8), dp(9));
        button.setElevation(dp(5));
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, raisedButtonBackground(darken(i, 0.78f), true));
        stateListDrawable.addState(new int[]{-16842910}, raisedButtonBackground(-13354434, false));
        stateListDrawable.addState(new int[0], raisedButtonBackground(i, false));
        button.setBackground(stateListDrawable);
        if (button.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) button.getLayoutParams();
            layoutParams.setMargins(dp(3), dp(3), dp(3), dp(4));
            button.setLayoutParams(layoutParams);
        }
    }

    private void showQuickInfo(String str) {
        Runnable runnable = this.dismissQuickInfo;
        if (runnable != null) {
            this.uiHandler.removeCallbacks(runnable);
        }
        Toast toast = this.quickInfoToast;
        if (toast != null) {
            toast.cancel();
        }
        final Toast toastMakeText = Toast.makeText(this, str, 0);
        this.quickInfoToast = toastMakeText;
        toastMakeText.show();
        Runnable runnable2 = new Runnable() { // from class: com.voxelscan.android.MainActivity$$ExternalSyntheticLambda40
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$showQuickInfo$50(toastMakeText);
            }
        };
        this.dismissQuickInfo = runnable2;
        this.uiHandler.postDelayed(runnable2, QUICK_INFO_DURATION_MS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showQuickInfo$50(Toast toast) {
        if (this.quickInfoToast == toast) {
            toast.cancel();
            this.quickInfoToast = null;
        }
    }

    private LayerDrawable raisedButtonBackground(int i, boolean z) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(-15722983);
        gradientDrawable.setCornerRadius(dp(7));
        GradientDrawable gradientDrawable2 = new GradientDrawable();
        gradientDrawable2.setColor(i);
        gradientDrawable2.setCornerRadius(dp(7));
        gradientDrawable2.setStroke(dp(1), lighten(i, 0.22f));
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{gradientDrawable, gradientDrawable2});
        layerDrawable.setLayerInset(1, 0, z ? dp(3) : 0, 0, z ? dp(1) : dp(4));
        return layerDrawable;
    }

    private int dp(int i) {
        return Math.round(i * getResources().getDisplayMetrics().density);
    }

    private static int darken(int i, float f) {
        return Color.rgb(Math.round(Color.red(i) * f), Math.round(Color.green(i) * f), Math.round(Color.blue(i) * f));
    }

    private static int lighten(int i, float f) {
        return Color.rgb(Math.round(Color.red(i) + ((255 - Color.red(i)) * f)), Math.round(Color.green(i) + ((255 - Color.green(i)) * f)), Math.round(Color.blue(i) + ((255 - Color.blue(i)) * f)));
    }

    private void exportJson() {
        File fileWriteJson;
        try {
            synchronized (this.renderer.world) {
                fileWriteJson = this.renderer.world.writeJson(new File(getCacheDir(), "voxelscan.voxels.json"), this.renderer.reviewPaletteSize());
            }
            Uri uriForFile = FileProvider.getUriForFile(this, getPackageName() + ".files", fileWriteJson);
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("application/json");
            intent.putExtra("android.intent.extra.STREAM", uriForFile);
            intent.addFlags(1);
            startActivity(Intent.createChooser(intent, "Export VoxelScan JSON"));
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), 1).show();
        }
    }

    private void exportVox() {
        File fileWrite;
        try {
            synchronized (this.renderer.world) {
                fileWrite = MagicaVoxExporter.write(this.renderer.world, new File(getCacheDir(), "voxelscan.vox"), this.renderer.reviewPaletteSize() > 0 ? this.renderer.reviewPaletteSize() : 255);
            }
            Uri uriForFile = FileProvider.getUriForFile(this, getPackageName() + ".files", fileWrite);
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("application/octet-stream");
            intent.putExtra("android.intent.extra.STREAM", uriForFile);
            intent.addFlags(1);
            startActivity(Intent.createChooser(intent, "Export MagicaVoxel VOX"));
        } catch (Exception e) {
            Toast.makeText(this, "VOX export failed: " + e.getMessage(), 1).show();
        }
    }
}
