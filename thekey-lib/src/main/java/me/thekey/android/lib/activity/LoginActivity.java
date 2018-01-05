package me.thekey.android.lib.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebView;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

import me.thekey.android.TheKey;
import me.thekey.android.lib.CodeGrantAsyncTask;
import me.thekey.android.lib.LoginWebViewClient;
import me.thekey.android.lib.R;
import me.thekey.android.lib.util.DisplayUtil;

import static me.thekey.android.lib.activity.ActivityBuilder.EXTRA_ARGS;

public class LoginActivity extends Activity {
    public static final String EXTRA_GUID = LoginActivity.class.getName() + ".EXTRA_GUID";

    private Bundle mArgs;

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
        frame = findViewById(R.id.loginViewFrame);
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
        protected void onAuthorizeSuccess(@NonNull final Uri uri, @NonNull final String code,
                                          @Nullable final String state) {
            new ActivityCodeGrantAsyncTask(LoginActivity.this, mTheKey, code, state).execute();
        }

        @Override
        protected void onAuthorizeError(final Uri uri, final String errorCode) {
            LoginActivity.this.setResult(RESULT_CANCELED);
            LoginActivity.this.finish();
        }
    }

    private static class ActivityCodeGrantAsyncTask extends CodeGrantAsyncTask {
        private final WeakReference<LoginActivity> mActivity;

        ActivityCodeGrantAsyncTask(@NonNull final LoginActivity activity, @NonNull final TheKey theKey,
                                   @NonNull final String code, @Nullable final String state) {
            super(theKey, code, state);
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected void onPostExecute(final String guid) {
            super.onPostExecute(guid);

            final LoginActivity activity = mActivity.get();
            if (activity != null) {
                if (guid != null) {
                    final Intent response = new Intent();
                    response.putExtra(EXTRA_GUID, guid);
                    activity.setResult(RESULT_OK, response);
                } else {
                    activity.setResult(RESULT_CANCELED);
                }

                activity.finish();
            }
        }
    }
}
