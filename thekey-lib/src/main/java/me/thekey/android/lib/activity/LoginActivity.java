package me.thekey.android.lib.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.webkit.WebView;
import android.widget.FrameLayout;

import me.thekey.android.lib.CodeGrantAsyncTask;
import me.thekey.android.lib.LoginWebViewClient;
import me.thekey.android.lib.R;
import me.thekey.android.lib.TheKeyImpl;
import me.thekey.android.lib.util.DisplayUtil;

import static me.thekey.android.lib.activity.ActivityBuilder.EXTRA_ARGS;

public class LoginActivity extends Activity {
    public static final String EXTRA_GUID = LoginActivity.class.getName() + ".EXTRA_GUID";

    private Bundle mArgs;
    @NonNull
    private /* final */ TheKeyImpl mTheKey;

    // login WebView
    private FrameLayout frame = null;
    private WebView loginView = null;

    public static ActivityBuilder builder(final Context context) {
        return new ActivityBuilder(context, LoginActivity.class);
    }

    /* BEGIN lifecycle */

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thekey_login);

        // get the arguments necessary for creation of TheKey object
        mArgs = getIntent().getBundleExtra(EXTRA_ARGS);
        if (mArgs == null) {
            finish();
            return;
        }

        // get TheKey object
        mTheKey = TheKeyImpl.getInstance(this);

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

    /* END lifecycle */

    private void attachLoginView() {
        this.detachLoginView();

        // create a loginView if it doesn't exist already
        if (this.loginView == null) {
            this.loginView = DisplayUtil.createLoginWebView(this, new ActivityLoginWebViewClient());
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
        ActivityLoginWebViewClient() {
            super(LoginActivity.this, mArgs);
        }

        @Override
        protected void onAuthorizeSuccess(final Uri uri, final String code) {
            new ActivityCodeGrantAsyncTask(mTheKey).execute(code);
        }

        @Override
        protected void onAuthorizeError(final Uri uri, final String errorCode) {
            LoginActivity.this.setResult(RESULT_CANCELED);
            LoginActivity.this.finish();
        }
    }

    private class ActivityCodeGrantAsyncTask extends CodeGrantAsyncTask {
        ActivityCodeGrantAsyncTask(@NonNull final TheKeyImpl theKey) {
            super(theKey);
        }

        @Override
        protected void onPostExecute(final String guid) {
            super.onPostExecute(guid);

            if (guid != null) {
                final Intent response = new Intent();
                response.putExtra(EXTRA_GUID, guid);
                LoginActivity.this.setResult(RESULT_OK, response);
            } else {
                LoginActivity.this.setResult(RESULT_CANCELED);
            }

            LoginActivity.this.finish();
        }
    }
}
