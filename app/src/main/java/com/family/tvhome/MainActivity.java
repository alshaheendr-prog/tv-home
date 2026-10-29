package com.family.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The home screen: a full-screen WebView showing assets/index.html, with a small bridge to launch apps. */
public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0B0D14);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setContentView(web);
        web.requestFocus();
        web.loadUrl("file:///android_asset/index.html");
        hideBars();
    }

    private void hideBars() {
        web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBars();
        web.onResume();
        web.evaluateJavascript("window.onHomeResume&&onHomeResume()", null);
    }

    @Override
    protected void onPause() {
        web.onPause(); // stops the animation while another app is open
        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        // Back on the home screen closes an open panel instead of leaving.
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP) web.evaluateJavascript("window.onBack&&onBack()", null);
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    private class Bridge {
        @JavascriptInterface
        public boolean launch(String pkg) {
            PackageManager pm = getPackageManager();
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = pm.getLaunchIntentForPackage(pkg);
            if (i == null) return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(i); return true; } catch (ActivityNotFoundException ex) { return false; }
        }

        @JavascriptInterface
        public boolean isInstalled(String pkg) {
            try { getPackageManager().getPackageInfo(pkg, 0); return true; } catch (Exception ex) { return false; }
        }

        @JavascriptInterface
        public void openSettings() {
            Intent i = new Intent(Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(i); } catch (Exception ignored) { }
        }

        /** Every launchable app as JSON: [{pkg, label, icon (PNG data URL)}], sorted by name. */
        @JavascriptInterface
        public String listApps() {
            PackageManager pm = getPackageManager();
            JSONArray out = new JSONArray();
            Set<String> seen = new HashSet<>();
            seen.add(getPackageName());
            String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
            for (String cat : cats) {
                Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
                List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
                for (ResolveInfo r : list) {
                    String pkg = r.activityInfo.packageName;
                    if (!seen.add(pkg)) continue;
                    try {
                        JSONObject o = new JSONObject();
                        o.put("pkg", pkg);
                        o.put("label", r.loadLabel(pm).toString());
                        o.put("icon", toDataUrl(r.loadIcon(pm)));
                        out.put(o);
                    } catch (Exception ignored) { }
                }
            }
            return out.toString();
        }
    }

    private static String toDataUrl(Drawable d) {
        Bitmap b = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, 128, 128);
        d.draw(c);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return "data:image/png;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
    }
}
