package org.ccci.gto.android.thekey;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebSettings;
import android.webkit.WebView;

public final class DisplayUtil {
    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public final static WebView createLoginWebView(final Context context, final TheKey thekey,
            final LoginWebViewClient webViewClient) {
        final WebView webView = new WebView(context);
        webView.setVisibility(View.GONE);
        webView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);

        final WebSettings settings = webView.getSettings();
        settings.setSavePassword(false);
        settings.setJavaScriptEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setAllowFileAccess(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            settings.setAllowContentAccess(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                settings.setAllowFileAccessFromFileURLs(false);
                settings.setAllowUniversalAccessFromFileURLs(false);
            }
        }

        webView.setWebViewClient(webViewClient);
        webView.loadUrl(thekey.getAuthorizeUri().toString());
        return webView;
    }
}
