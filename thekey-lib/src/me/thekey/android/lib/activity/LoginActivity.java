package me.thekey.android.lib.activity;

import static me.thekey.android.lib.activity.ActivityBuilder.EXTRA_ARGS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.FrameLayout;

import org.ccci.gto.android.thekey.CodeGrantAsyncTask;
import org.ccci.gto.android.thekey.LoginWebViewClient;
import org.ccci.gto.android.thekey.R;
import org.ccci.gto.android.thekey.TheKeyImpl;

import me.thekey.android.lib.util.DisplayUtil;

public class LoginActivity extends Activity {
    public final static String EXTRA_GUID = LoginActivity.class.getName() + ".EXTRA_GUID";

    private Bundle mArgs;
    private TheKeyImpl mTheKey;

    // login WebView
    private FrameLayout frame = null;
    private WebView loginView = null;

    public static ActivityBuilder builder(final Context context) {
        return new ActivityBuilder(context, LoginActivity.class);
    }

    /** BEGIN lifecycle */

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thekey_login);

        // create TheKey object
        final Intent intent = getIntent();
        mArgs = intent.getBundleExtra(EXTRA_ARGS);
        mTheKey = TheKeyImpl.getInstance(this, mArgs);

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

    /** END lifecycle */

    private void attachLoginView() {
        this.detachLoginView();

        // create a loginView if it doesn't exist already
        if (this.loginView == null) {
            this.loginView = DisplayUtil.createLoginWebView(this, new ActivityLoginWebViewClient(), mArgs);
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
        public ActivityLoginWebViewClient() {
            super(LoginActivity.this, mArgs);
        }

        @Override
        protected void onAuthorizeSuccess(final Uri uri, final String code) {
            new ActivityCodeGrantAsyncTask().execute(code);
        }

        @Override
        protected void onAuthorizeError(final Uri uri, final String errorCode) {
            LoginActivity.this.setResult(RESULT_CANCELED);
            LoginActivity.this.finish();
        }
    }

    private class ActivityCodeGrantAsyncTask extends CodeGrantAsyncTask {
        public ActivityCodeGrantAsyncTask() {
            super(mTheKey);
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            super.onPostExecute(result);

            if (result) {
                final Intent response = new Intent();
                response.putExtra(EXTRA_GUID, mTheKey.getGuid());
                LoginActivity.this.setResult(RESULT_OK, response);
            } else {
                LoginActivity.this.setResult(RESULT_CANCELED);
            }

            LoginActivity.this.finish();
        }
    }
}
