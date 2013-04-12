package org.ccci.gto.android.thekey;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;

public final class DisplayUtil {
    @SuppressLint("SetJavaScriptEnabled")
    public final static WebView createLoginWebView(final Context context, final TheKey thekey,
            final LoginWebViewClient webViewClient) {
        final WebView webView = new WebView(context);
        webView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.setWebViewClient(webViewClient);
        webView.loadUrl(thekey.getAuthorizeUri().toString());
        return webView;
    }
}
