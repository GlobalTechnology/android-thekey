package org.ccci.gto.android.thekey;

import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_CLIENT_ID;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_REDIRECT_URI;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_STATE;

import org.ccci.gto.android.thekey.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LoginActivity extends Activity {
    public final static String EXTRA_CASSERVER = "org.ccci.gto.android.thekey.CAS_SERVER";
    public final static String EXTRA_CLIENTID = "org.ccci.gto.android.thekey.CLIENT_ID";
    public final static String EXTRA_RESPONSE_GUID = "org.ccci.gto.android.thekey.response.GUID";

    private TheKey thekey;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thekey_login);

        // create TheKey object
        final Intent intent = getIntent();
        final long clientId = intent.getLongExtra(EXTRA_CLIENTID, -1);
        if (intent.hasExtra(EXTRA_CASSERVER)) {
            this.thekey = new TheKey(this, clientId, intent.getStringExtra(EXTRA_CASSERVER));
        } else {
            this.thekey = new TheKey(this, clientId);
        }

        // build the Login WebView
        this.buildWebView();
    }

    private void buildWebView() {
        final Uri redirectUri = Uri.parse("thekey:/oauth/mobile/android");
        final String state = "asdf";

        // build oauth authorize url
        final Uri authUri = this.thekey.getCasUri("oauth", "authorize").buildUpon()
                .appendQueryParameter(OAUTH_PARAM_CLIENT_ID, this.thekey.getClientId().toString())
                .appendQueryParameter(OAUTH_PARAM_REDIRECT_URI, redirectUri.toString())
                .appendQueryParameter(OAUTH_PARAM_STATE, state).build();

        // set WebView to navigate to the OAuth authorize url
        final WebView loginView = (WebView) findViewById(R.id.login_web_view);
        loginView.getSettings().setJavaScriptEnabled(true);
        loginView.setWebViewClient(new TheKeyWebViewClient(redirectUri, state));
        loginView.loadUrl(authUri.toString());
    }

    private class TheKeyWebViewClient extends WebViewClient {
        private final Uri oauthUri;
        private final Uri redirectUri;
        private final String state;

        TheKeyWebViewClient(final Uri redirectUri, final String state) {
            this.oauthUri = LoginActivity.this.thekey.getCasUri("oauth").buildUpon().query("").build();
            this.redirectUri = redirectUri;
            this.state = state;
        }

        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String uri) {
            final Uri parsedUri = Uri.parse(uri);
            // response redirect
            if (this.isRedirectUri(parsedUri)) {
                final String code = Uri.parse(uri).getQueryParameter("code");
                if(code != null) {
                    new ExchangeCodeAsyncTask(code, this.redirectUri).execute();
                }
                return true;
            }
            // CAS OAuth url
            else if (this.isOAuthUri(parsedUri)) {
                return false;
            }
            // external link, launch default Android activity
            else {
                startActivity(new Intent(Intent.ACTION_VIEW, parsedUri));
                return true;
            }
        }

        private boolean isOAuthUri(final Uri uri) {
            return uri.getScheme().equals(this.oauthUri.getScheme())
                    && uri.getSchemeSpecificPart().startsWith(this.oauthUri.getSchemeSpecificPart());
        }

        private boolean isRedirectUri(final Uri uri) {
            return this.redirectUri.getScheme().equals(uri.getScheme())
                    && this.redirectUri.getPath().equals(uri.getPath())
                    && this.state.equals(uri.getQueryParameter(OAUTH_PARAM_STATE));
        }
    }

    private class ExchangeCodeAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private final String code;
        private final Uri redirectUri;

        public ExchangeCodeAsyncTask(final String code, final Uri redirectUri) {
            this.code = code;
            this.redirectUri = redirectUri;
        }

        @Override
        protected Boolean doInBackground(final Void... args) {
            return LoginActivity.this.thekey.processCodeGrant(this.code, this.redirectUri);
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            super.onPostExecute(result);

            if (result.booleanValue()) {
                final Intent response = new Intent();
                response.putExtra(EXTRA_RESPONSE_GUID, LoginActivity.this.thekey.getGuid());
                LoginActivity.this.setResult(RESULT_OK, response);
            } else {
                LoginActivity.this.setResult(RESULT_CANCELED);
            }

            LoginActivity.this.finish();
        }
    }
}
