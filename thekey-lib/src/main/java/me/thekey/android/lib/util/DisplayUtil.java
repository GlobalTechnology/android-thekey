package me.thekey.android.lib.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import me.thekey.android.lib.LoginWebViewClient;
import me.thekey.android.lib.TheKeyImpl;

public final class DisplayUtil {
    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static WebView createLoginWebView(final Context context, final LoginWebViewClient client) {
        final TheKeyImpl thekey = TheKeyImpl.getInstance(context);

        final WebView webView = new WebView(context);
        webView.setVisibility(View.GONE);
        webView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);

        final WebSettings settings = webView.getSettings();

        // security related settings
        settings.setSavePassword(false);
        settings.setAllowFileAccess(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            settings.setAllowContentAccess(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                settings.setAllowFileAccessFromFileURLs(false);
                settings.setAllowUniversalAccessFromFileURLs(false);
            }
        }

        // display related settings
        settings.setJavaScriptEnabled(true);
        settings.setLoadsImagesAutomatically(true);

        // clear SSO cookies if needed
        final String authorizeUrl = thekey.getAuthorizeUri().toString();
        final CookieManager cookieManager = CookieManager.getInstance();
        final String cookies = cookieManager.getCookie(authorizeUrl);
        if (cookies != null && cookies.length() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.removeAllCookies(null);
            } else {
                cookieManager.removeAllCookie();
            }
        }

        webView.setWebViewClient(client);
        webView.loadUrl(authorizeUrl);

        // apply any hacks to work around various bugs in previous versions of android
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.FROYO:
            case Build.VERSION_CODES.GINGERBREAD:
            case Build.VERSION_CODES.GINGERBREAD_MR1:
                webView.setOnTouchListener(new SDK8TouchListener());
                break;
        }

        return webView;
    }

    /**
     * This listener is only used on SDK levels 8-10
     * <p/>
     * This will work around a focus bug affecting WebViews
     * <p/>
     * see http://stackoverflow.com/questions/3460915
     */
    static final class SDK8TouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(final View v, final MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_UP:
                    if (!v.hasFocus()) {
                        v.requestFocus();
                    }
            }
            return false;
        }
    }
}
