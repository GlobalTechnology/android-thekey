package me.thekey.android.view.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.FrameLayout;

import me.thekey.android.core.CodeGrantAsyncTask;
import me.thekey.android.core.TheKeyImpl;
import me.thekey.android.view.Builder;
import me.thekey.android.view.LoginWebViewClient;
import me.thekey.android.view.fragment.FragmentBuilder;
import me.thekey.android.view.util.DisplayUtil;
import timber.log.Timber;

public class LoginDialogFragment extends DialogFragment {
    public interface Listener {
        void onLoginSuccess(DialogFragment dialog, String guid);

        void onLoginFailure(DialogFragment dialog);
    }

    // login WebView
    private FrameLayout frame = null;
    private WebView mLoginView = null;

    public static Builder<LoginDialogFragment> builder() {
        return new FragmentBuilder<>(LoginDialogFragment.class);
    }

    /* BEGIN lifecycle */

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
                (FrameLayout) LayoutInflater.from(this.getActivity()).inflate(R.layout.thekey_login, null);
        this.attachLoginView(frame);
        builder.setView(frame);

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        // Work around bug:
        // http://code.google.com/p/android/issues/detail?id=17423
        final Dialog dialog = this.getDialog();
        if ((dialog != null) && this.getRetainInstance()) {
            dialog.setDismissMessage(null);
        }

        super.onDestroyView();
    }

    /* END lifecycle */

    private void attachLoginView(final FrameLayout frame) {
        this.detachLoginView();

        // create a Login WebView if one doesn't exist already
        if (mLoginView == null) {
            final Bundle args = getArguments();
            mLoginView = DisplayUtil.createLoginWebView(getActivity(), new LoginDialogWebViewClient(this, args));
        }

        // attach the login view to the current frame
        this.frame = frame;
        this.frame.addView(mLoginView);
    }

    private void detachLoginView() {
        // remove the login view from any existing frame
        if (this.frame != null) {
            try {
                this.frame.removeView(mLoginView);
            } catch (final IllegalArgumentException e) {
                // XXX: KEYAND-12 IllegalArgumentException: Receiver not registered: android.webkit.WebViewClassic
                Timber.e(e, "error removing Login WebView, let's just reset the login view");
                mLoginView = null;
            }
            this.frame = null;
        }
    }

    class LoginDialogWebViewClient extends LoginWebViewClient {
        LoginDialogWebViewClient(final DialogFragment dialog, final Bundle args) {
            super(dialog.getActivity(), args);
        }

        @Override
        protected void onAuthorizeSuccess(@NonNull final Uri uri, @NonNull final String code,
                                          @Nullable final String state) {
            new LoginDialogCodeGrantAsyncTask(LoginDialogFragment.this, mTheKey, code, state).execute();
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
        private final DialogFragment mDialog;

        LoginDialogCodeGrantAsyncTask(final DialogFragment dialog, @NonNull final TheKeyImpl thekey,
                                      @NonNull final String code, @Nullable final String state) {
            super(thekey, null, code, state);
            mDialog = dialog;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(final String guid) {
            super.onPostExecute(guid);

            final Activity activity = mDialog.getActivity();
            if (activity instanceof Listener) {
                // trigger the correct callback
                if (guid != null) {
                    ((Listener) activity).onLoginSuccess(mDialog, guid);
                } else {
                    ((Listener) activity).onLoginFailure(mDialog);
                }
            }

            // close the mDialog if it is still active (added to the activity)
            if (mDialog.isAdded()) {
                mDialog.dismissAllowingStateLoss();
            }
        }
    }
}
