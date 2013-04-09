package org.ccci.gto.android.thekey;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.FrameLayout;

public class LoginActivity extends Activity {
    public final static String EXTRA_CASSERVER = "org.ccci.gto.android.thekey.CAS_SERVER";
    public final static String EXTRA_CLIENTID = "org.ccci.gto.android.thekey.CLIENT_ID";
    public final static String EXTRA_RESPONSE_GUID = "org.ccci.gto.android.thekey.response.GUID";

    private TheKey thekey;

    // login WebView
    private FrameLayout frame = null;
    private WebView loginView = null;

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

        // init the Login WebView
        this.attachLoginView();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        this.detachLoginView();

        super.onConfigurationChanged(newConfig);

        // reload the view
        setContentView(R.layout.thekey_login);

        // attach the loginView
        this.attachLoginView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void attachLoginView() {
        this.detachLoginView();

        // create a loginView if it doesn't exist already
        if (this.loginView == null) {
            this.loginView = new WebView(this);
            this.loginView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            this.loginView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
            this.loginView.setScrollbarFadingEnabled(true);
            this.loginView.getSettings().setJavaScriptEnabled(true);
            this.loginView.getSettings().setLoadsImagesAutomatically(true);
            this.loginView.setWebViewClient(new ActivityLoginWebViewClient(this, this.thekey));

            this.loginView.loadUrl(this.thekey.getAuthorizeUri().toString());
        }
        
        // attach the login view to the current frame
        this.frame = (FrameLayout) findViewById(R.id.loginViewFrame);
        this.frame.addView(this.loginView);
    }

    private void detachLoginView() {
        // remove the login view from any existing frame
        if (this.frame != null) {
            this.frame.removeView(this.loginView);
            this.frame = null;
        }
    }

    private class ActivityLoginWebViewClient extends LoginWebViewClient {
        public ActivityLoginWebViewClient(final Context context, final TheKey thekey) {
            super(context, thekey);
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
