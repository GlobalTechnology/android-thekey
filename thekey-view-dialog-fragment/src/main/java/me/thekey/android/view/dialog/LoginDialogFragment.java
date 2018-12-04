package me.thekey.android.view.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import me.thekey.android.TheKey;
import me.thekey.android.core.CodeGrantAsyncTask;
import me.thekey.android.view.Builder;
import me.thekey.android.view.LoginWebViewClient;
import me.thekey.android.view.fragment.FragmentBuilder;
import me.thekey.android.view.util.DisplayUtil;
import timber.log.Timber;

public class LoginDialogFragment extends DialogFragment {
    private static final String TAG = "LoginDialogFragment";

    public interface Listener {
        void onLoginSuccess(LoginDialogFragment dialog, String guid);

        void onLoginFailure(LoginDialogFragment dialog);
    }

    // login WebView
    @Nullable
    private FrameLayout mFrame = null;
    @Nullable
    private WebView mLoginView = null;
    @Nullable
    private LoginWebViewClient mLoginWebViewClient = null;

    public static Builder<LoginDialogFragment> builder() {
        return new FragmentBuilder<>(LoginDialogFragment.class);
    }

    // region Lifecycle Events

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        updateLoginWebViewClient();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // build dialog
        final FrameLayout frame =
                (FrameLayout) LayoutInflater.from(requireContext()).inflate(R.layout.thekey_login, null);
        attachLoginView(frame);
        builder.setView(frame);

        // handle back button presses to navigate back in the WebView if possible
        builder.setOnKeyListener((dialog, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP &&
                    keyCode == KeyEvent.KEYCODE_BACK &&
                    event.isTracking() && !event.isCanceled()) {
                return DisplayUtil.navigateBackIfPossible(mLoginView);
            }
            return false;
        });

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        // HACK: Work around bug
        // HACK: http://code.google.com/p/android/issues/detail?id=17423
        // HACK: https://issuetracker.google.com/issues/36929400
        final Dialog dialog = this.getDialog();
        if ((dialog != null) && this.getRetainInstance()) {
            dialog.setDismissMessage(null);
        }

        super.onDestroyView();
    }

    // endregion Lifecycle Events

    @UiThread
    private void attachLoginView(@NonNull final FrameLayout frame) {
        detachLoginView();

        // create the LoginWebViewClient if we don't have one yet
        if (mLoginWebViewClient == null) {
            mLoginWebViewClient = new LoginDialogWebViewClient(getArguments());
            updateLoginWebViewClient();
        }

        // create a Login WebView if one doesn't exist already
        if (mLoginView == null) {
            mLoginView = DisplayUtil.createLoginWebView(frame.getContext(), mLoginWebViewClient, getArguments());
        }

        // attach the login view to the current mFrame
        mFrame = frame;
        mFrame.addView(mLoginView);
    }

    private void updateLoginWebViewClient() {
        if (mLoginWebViewClient != null) {
            mLoginWebViewClient.setActivity(getActivity());
        }
    }

    @UiThread
    private void detachLoginView() {
        // remove the login view from any existing mFrame
        if (mFrame != null) {
            try {
                mFrame.removeView(mLoginView);
            } catch (final IllegalArgumentException e) {
                // XXX: KEYAND-12 IllegalArgumentException: Receiver not registered: android.webkit.WebViewClassic
                Timber.tag(TAG)
                        .e(e, "error removing Login WebView, let's just reset the login view");
                mLoginView = null;
                mLoginWebViewClient = null;
            }
            mFrame = null;
        }
    }

    @Override
    public void dismissAllowingStateLoss() {
        try {
            super.dismissAllowingStateLoss();
        } catch (final IllegalStateException suppressed) {
            // HACK: work around state loss exception if the dialog was added to the back stack
            Timber.tag(TAG)
                    .d(suppressed, "Error dismissing the LoginDialogFragment, probably because of a Back Stack.");
            getFragmentManager().beginTransaction()
                    .remove(this)
                    .commitAllowingStateLoss();
        }
    }

    class LoginDialogWebViewClient extends LoginWebViewClient {
        LoginDialogWebViewClient(@Nullable final Bundle args) {
            super(requireContext(), args);
        }

        @Override
        protected void onAuthorizeSuccess(@NonNull final Uri uri, @NonNull final String code,
                                          @Nullable final String state) {
            new LoginDialogCodeGrantAsyncTask(LoginDialogFragment.this, mTheKey, uri).execute();
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void onAuthorizeError(final Uri uri, final String errorCode) {
            final Activity activity = getActivity();
            if (activity instanceof Listener) {
                ((Listener) activity).onLoginFailure(LoginDialogFragment.this);
            }

            // close the dialog if it is still active (added to the activity)
            if (isAdded()) {
                dismissAllowingStateLoss();
            }
        }
    }

    static final class LoginDialogCodeGrantAsyncTask extends CodeGrantAsyncTask {
        private final WeakReference<LoginDialogFragment> mDialog;

        LoginDialogCodeGrantAsyncTask(@NonNull final LoginDialogFragment dialog, @NonNull final TheKey thekey,
                                      @NonNull final Uri dataUri) {
            super(thekey, dataUri);
            mDialog = new WeakReference<>(dialog);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(final String guid) {
            super.onPostExecute(guid);

            final LoginDialogFragment dialog = mDialog.get();
            if (dialog != null) {
                final Activity activity = dialog.getActivity();
                if (activity instanceof Listener) {
                    // trigger the correct callback
                    if (guid != null) {
                        ((Listener) activity).onLoginSuccess(dialog, guid);
                    } else {
                        ((Listener) activity).onLoginFailure(dialog);
                    }
                }

                // close the dialog if it is still active (added to the activity)
                if (dialog.isAdded()) {
                    dialog.dismissAllowingStateLoss();
                }
            }
        }
    }
}
