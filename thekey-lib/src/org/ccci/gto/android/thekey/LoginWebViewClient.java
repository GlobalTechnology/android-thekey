package org.ccci.gto.android.thekey;

import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_CODE;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_ERROR;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_STATE;
import static org.ccci.gto.android.thekey.Constant.REDIRECT_URI;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public abstract class LoginWebViewClient extends WebViewClient {
    private final Context context;
    protected final TheKey thekey;
    private final Uri oauthUri;
    private final String state;

    public LoginWebViewClient(final Context context, final TheKey thekey) {
        this(context, thekey, null);
    }

    public LoginWebViewClient(final Context context, final TheKey thekey, final String state) {
        this.context = context;
        this.thekey = thekey;
        this.oauthUri = this.thekey.getAuthorizeUri().buildUpon().query("").build();
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
        // external link, launch default Android activity
        else {
            this.context.startActivity(new Intent(Intent.ACTION_VIEW, parsedUri));
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
        return REDIRECT_URI.getScheme().equals(uri.getScheme())
                && REDIRECT_URI.getPath().equals(uri.getPath())
                && (this.state == null ? uri.getQueryParameter(OAUTH_PARAM_STATE) == null : this.state.equals(uri
                        .getQueryParameter(OAUTH_PARAM_STATE)));
    }

    protected abstract void onAuthorizeSuccess(final Uri uri, final String code);

    protected abstract void onAuthorizeError(final Uri uri, final String errorCode);
}
