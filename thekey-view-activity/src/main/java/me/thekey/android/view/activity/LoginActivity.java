package me.thekey.android.view.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.webkit.WebView;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

import me.thekey.android.TheKey;
import me.thekey.android.core.CodeGrantAsyncTask;
import me.thekey.android.view.Builder;
import me.thekey.android.view.LoginWebViewClient;
import me.thekey.android.view.util.DisplayUtil;
import timber.log.Timber;

import static me.thekey.android.view.activity.ActivityBuilder.EXTRA_ARGS;

public class LoginActivity extends Activity {
    public static final String EXTRA_GUID = LoginActivity.class.getName() + ".EXTRA_GUID";

    @NonNull
    /*final*/ Bundle mArgs;

    // login WebView
    private FrameLayout frame = null;
    @Nullable
    private WebView mLoginView;

    public static Builder<Activity> builder(final Context context) {
        return new ActivityBuilder(context, LoginActivity.class);
    }

    /* BEGIN lifecycle */

    @Override
    @SuppressLint("BinaryOperationInTimber")
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thekey_login);

        // get the arguments necessary for creation of TheKey object
        mArgs = getIntent().getBundleExtra(EXTRA_ARGS);
        if (mArgs == null) {
            Timber.tag("LoginActivity")
                    .e("Error creating LoginActivity, make sure to use LoginActivity.builder(Context) " +
                               "to create the activity.");
            finish();
            return;
        }

        // init the Login WebView
        this.attachLoginView();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        // reload the view (but preserve the login view)
        detachLoginView();
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.thekey_login);
        attachLoginView();
    }

    @Override
    public void onBackPressed() {
        if (DisplayUtil.navigateBackIfPossible(mLoginView)) {
            return;
        }
        super.onBackPressed();
    }

    /* END lifecycle */

    private void attachLoginView() {
        this.detachLoginView();

        // create a mLoginView if it doesn't exist already
        if (mLoginView == null) {
            mLoginView = DisplayUtil.createLoginWebView(this, new ActivityLoginWebViewClient(), mArgs);
        }

        // attach the login view to the current frame
        frame = findViewById(R.id.loginViewFrame);
        this.frame.addView(mLoginView);
    }

    private void detachLoginView() {
        // remove the login view from any existing frame
        if (this.frame != null) {
            this.frame.removeView(mLoginView);
            this.frame = null;
        }
    }

    private class ActivityLoginWebViewClient extends LoginWebViewClient {
        ActivityLoginWebViewClient() {
            super(LoginActivity.this, mArgs);
            setActivity(LoginActivity.this);
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
            super(theKey, null, code, state);
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
