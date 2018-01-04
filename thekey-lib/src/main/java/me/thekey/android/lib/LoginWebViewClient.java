package me.thekey.android.lib;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import me.thekey.android.core.TheKeyImpl;

import static me.thekey.android.core.Constants.ARG_SELF_SERVICE;
import static me.thekey.android.core.Constants.OAUTH_PARAM_CODE;
import static me.thekey.android.core.Constants.OAUTH_PARAM_ERROR;
import static me.thekey.android.core.Constants.OAUTH_PARAM_STATE;

public abstract class LoginWebViewClient extends WebViewClient {
    private final Context mContext;
    private final Bundle mArgs;
    @NonNull
    protected final TheKeyImpl mTheKey;
    private final String mState;

    /* various Uris used internally */
    private final Uri mOauthUri;
    private final Uri mSelfServiceUri;
    private final Uri mRedirectUri;

    protected LoginWebViewClient(final Context context, final Bundle args) {
        this(context, args, null);
    }

    private LoginWebViewClient(final Context context, final Bundle args, final String state) {
        mContext = context;
        mArgs = args;
        mTheKey = TheKeyImpl.getInstance(context);
        mOauthUri = mTheKey.getAuthorizeUri().buildUpon().query("").build();
        mSelfServiceUri = mTheKey.getCasUri("service", "selfservice");
        mRedirectUri = mTheKey.getRedirectUri();
        mState = state;
    }

    @Override
    @SuppressWarnings("checkstyle:RightCurly")
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
        return isBaseUriEqual(mOauthUri, uri);
    }

    private boolean isRedirectUri(final Uri uri) {
        return isBaseUriEqual(mRedirectUri, uri) && TextUtils.equals(mState, uri.getQueryParameter(OAUTH_PARAM_STATE));
    }

    private boolean isSelfServiceUri(final Uri uri) {
        return isBaseUriEqual(mSelfServiceUri, uri);
    }

    private boolean isBaseUriEqual(@NonNull final Uri baseUri, @NonNull final Uri uri) {
        return TextUtils.equals(uri.getScheme(), baseUri.getScheme()) &&
                TextUtils.equals(uri.getAuthority(), baseUri.getAuthority()) &&
                TextUtils.equals(uri.getPath(), baseUri.getPath());
    }

    protected abstract void onAuthorizeSuccess(Uri uri, String code);

    protected abstract void onAuthorizeError(Uri uri, String errorCode);
}
