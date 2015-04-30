package me.thekey.android.lib;

import static me.thekey.android.lib.Constant.ARG_SELF_SERVICE;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_CODE;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_ERROR;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_STATE;
import static me.thekey.android.lib.Constant.REDIRECT_URI;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public abstract class LoginWebViewClient extends WebViewClient {
    private final Context mContext;
    private final Bundle mArgs;
    @NonNull
    protected final TheKeyImpl mTheKey;
    private final String state;

    /* various Uris used internally */
    private final Uri oauthUri;
    private final Uri selfServiceUri;

    protected LoginWebViewClient(final Context context, final Bundle args) {
        this(context, args, null);
    }

    private LoginWebViewClient(final Context context, final Bundle args, final String state) {
        mContext = context;
        mArgs = args;
        mTheKey = TheKeyImpl.getInstance(context, args);
        this.oauthUri = mTheKey.getAuthorizeUri().buildUpon().query("").build();
        this.selfServiceUri = mTheKey.getCasUri("service", "selfservice");
        this.state = state;
    }

    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String uri) {
        final Uri parsedUri = Uri.parse(uri);
        // response redirect
        if (this.isRedirectUri(parsedUri)) {
            final String code = parsedUri.getQueryParameter(OAUTH_PARAM_CODE);
            if (code != null) {
                this.onAuthorizeSuccess(parsedUri, code);
            } else {
                this.onAuthorizeError(parsedUri, parsedUri.getQueryParameter(OAUTH_PARAM_ERROR));
            }
            return true;
        }
        // CAS OAuth url
        else if (this.isOAuthUri(parsedUri)) {
            return false;
        }
        // CAS self service
        else if (mArgs.getBoolean(ARG_SELF_SERVICE, false) && this.isSelfServiceUri(parsedUri)) {
            return false;
        }
        // external link, launch default Android activity
        else {
            mContext.startActivity(new Intent(Intent.ACTION_VIEW, parsedUri));
            return true;
        }
    }

    @Override
    public void onPageFinished(final WebView webView, final String uri) {
        super.onPageFinished(webView, uri);

        // find the login progress view
        ViewParent parent = webView;
        View rootView = webView;
        while ((parent = parent.getParent()) != null) {
            if (parent instanceof View) {
                rootView = (View) parent;
            }
        }
        final View progress = rootView.findViewById(R.id.loginProgress);

        // toggle visibility based on redirects received
        final Uri parsedUri = Uri.parse(uri);
        if (this.isOAuthUri(parsedUri)) {
            if (progress != null) {
                // hide progress bar
                progress.setVisibility(View.GONE);
            }

            // show WebView
            webView.setVisibility(View.VISIBLE);
        } else if (this.isRedirectUri(parsedUri)) {
            if (progress != null) {
                // hide WebView
                webView.setVisibility(View.GONE);

                // show progress bar
                progress.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        this.onAuthorizeError(Uri.parse(failingUrl), null);
    }

    private boolean isOAuthUri(final Uri uri) {
        return uri.getScheme().equals(this.oauthUri.getScheme())
                && uri.getSchemeSpecificPart().startsWith(this.oauthUri.getSchemeSpecificPart());
    }

    private boolean isRedirectUri(final Uri uri) {
        return REDIRECT_URI.getScheme().equals(uri.getScheme()) && REDIRECT_URI.getPath().equals(uri.getPath()) &&
                (this.state == null ? uri.getQueryParameter(OAUTH_PARAM_STATE) == null :
                        this.state.equals(uri.getQueryParameter(
                                OAUTH_PARAM_STATE)));
    }

    private boolean isSelfServiceUri(final Uri uri) {
        return uri.getScheme().equals(this.selfServiceUri.getScheme())
                && uri.getSchemeSpecificPart().startsWith(this.selfServiceUri.getSchemeSpecificPart());
    }

    protected abstract void onAuthorizeSuccess(final Uri uri, final String code);

    protected abstract void onAuthorizeError(final Uri uri, final String errorCode);
}
