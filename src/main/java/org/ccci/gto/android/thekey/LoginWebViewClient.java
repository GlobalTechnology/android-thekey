package org.ccci.gto.android.thekey;

import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_CODE;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_ERROR;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_STATE;
import static org.ccci.gto.android.thekey.Constant.REDIRECT_URI;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
