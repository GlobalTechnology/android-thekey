package me.thekey.android.view.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import me.thekey.android.TheKey;
import me.thekey.android.core.ArgumentUtils;
import me.thekey.android.view.LoginWebViewClient;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public final class DisplayUtil {
    @SuppressLint("SetJavaScriptEnabled")
    public static WebView createLoginWebView(@NonNull final Context context, @NonNull final LoginWebViewClient client,
                                             @Nullable final Bundle args) {
        final TheKey thekey = TheKey.getInstance(context);

        final WebView webView = new WebView(context);
        webView.setVisibility(View.GONE);
        webView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);

        final WebSettings settings = webView.getSettings();

        // security related settings
        settings.setSavePassword(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
        }

        // display related settings
        settings.setJavaScriptEnabled(true);
        settings.setLoadsImagesAutomatically(true);

        // clear SSO cookies if needed
        final String authorizeUrl = thekey.loginUriBuilder()
                .redirectUri(ArgumentUtils.getRedirectUri(args, null))
                .signup(ArgumentUtils.isSignup(args))
                .build().toString();
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

        return webView;
    }

    public static boolean navigateBackIfPossible(@Nullable final WebView webView) {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }
}
