package org.ccci.gto.android.thekey;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;

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

    @SuppressLint("SetJavaScriptEnabled")
    private void buildWebView() {
        // build oauth authorize url
        final String state = "asdf";
        final Uri authUri = this.thekey.getAuthorizeUri(state);

        // set WebView to navigate to the OAuth authorize url
        final WebView loginView = (WebView) findViewById(R.id.login_web_view);
        loginView.getSettings().setJavaScriptEnabled(true);
        loginView.setWebViewClient(new ActivityLoginWebViewClient(this, this.thekey, state));
        loginView.loadUrl(authUri.toString());
    }

    private class ActivityLoginWebViewClient extends LoginWebViewClient {
        public ActivityLoginWebViewClient(final Context context, final TheKey thekey, final String state) {
            super(context, thekey, state);
        }

        @Override
        protected CodeGrantAsyncTask getCodeGrantAsyncTask() {
            return new ActivityCodeGrantAsyncTask(this.thekey);
        }
    }

    private class ActivityCodeGrantAsyncTask extends CodeGrantAsyncTask {
        public ActivityCodeGrantAsyncTask(final TheKey thekey) {
            super(thekey);
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
