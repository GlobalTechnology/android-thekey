package org.ccci.gto.android.thekey;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.FrameLayout;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LoginDialogFragment extends DialogFragment {
    public final static String ARG_CASSERVER = "org.ccci.gto.android.thekey.CAS_SERVER";
    public final static String ARG_CLIENTID = "org.ccci.gto.android.thekey.CLIENT_ID";

    private TheKey thekey;

    // login WebView
    private FrameLayout frame = null;
    private WebView loginView = null;

    public LoginDialogFragment() {
    }

    public static final LoginDialogFragment newInstance(final long clientId) {
        return LoginDialogFragment.newInstance(clientId, null);
    }

    public static final LoginDialogFragment newInstance(final long clientId, final String casServer) {
        final LoginDialogFragment fragment = new LoginDialogFragment();

        // handle arguments
        final Bundle args = new Bundle();
        args.putLong(ARG_CLIENTID, clientId);
        args.putString(ARG_CASSERVER, casServer);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setRetainInstance(true);

        // load arguments
        final long clientId = getArguments().getLong(ARG_CLIENTID, -1);
        final String casServer = getArguments().getString(ARG_CASSERVER);
        if (casServer != null) {
            this.thekey = new TheKey(getActivity(), clientId, casServer);
        } else {
            this.thekey = new TheKey(getActivity(), clientId);
        }
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // build dialog
        final FrameLayout frame = (FrameLayout) LayoutInflater.from(getActivity()).inflate(R.layout.thekey_login, null);
        this.attachLoginView(frame);
        builder.setView(frame);

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        // Work around bug:
        // http://code.google.com/p/android/issues/detail?id=17423
        final Dialog dialog = getDialog();
        if ((dialog != null) && getRetainInstance())
            dialog.setDismissMessage(null);

        super.onDestroyView();
    }

    private void attachLoginView(final FrameLayout frame) {
        this.detachLoginView();

        // create a loginView if it doesn't exist already
        if (this.loginView == null) {
            this.loginView = DisplayUtil.createLoginWebView(getActivity(), this.thekey, new DialogLoginWebViewClient(
                    this.getActivity(), this.thekey));
        }

        // attach the login view to the current frame
        this.frame = frame;
        this.frame.addView(this.loginView);
    }

    private void detachLoginView() {
        // remove the login view from any existing frame
        if (this.frame != null) {
            this.frame.removeView(this.loginView);
            this.frame = null;
        }
    }

    public interface TheKeyAuthorizeDialogListener {
        public void onAuthorizeSuccess(final LoginDialogFragment dialog, final String guid);

        public void onAuthorizeFailure(final LoginDialogFragment dialog);
    }

    private class DialogLoginWebViewClient extends LoginWebViewClient {
        public DialogLoginWebViewClient(final Context context, final TheKey thekey) {
            super(context, thekey);
        }

        @Override
        protected void onAuthorizeSuccess(final Uri uri, final String code) {
            new DialogCodeGrantAsyncTask(this.thekey).execute(code);
        }

        @Override
        protected void onAuthorizeError(final Uri uri, final String errorCode) {
            final LoginDialogFragment dialog = LoginDialogFragment.this;

            final Activity activity = dialog.getActivity();
            if (activity instanceof TheKeyAuthorizeDialogListener) {
                ((TheKeyAuthorizeDialogListener) activity).onAuthorizeFailure(dialog);
            }

            dialog.dismiss();
        }
    }

    private class DialogCodeGrantAsyncTask extends CodeGrantAsyncTask {
        public DialogCodeGrantAsyncTask(final TheKey thekey) {
            super(thekey);
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            super.onPostExecute(result);
            final LoginDialogFragment dialog = LoginDialogFragment.this;

            final Activity activity = dialog.getActivity();
            if (activity instanceof TheKeyAuthorizeDialogListener) {
                // trigger the correct callback
                if (result.booleanValue()) {
                    ((TheKeyAuthorizeDialogListener) activity).onAuthorizeSuccess(dialog, this.thekey.getGuid());
                } else {
                    ((TheKeyAuthorizeDialogListener) activity).onAuthorizeFailure(dialog);
                }
            }

            // close the dialog
            dialog.dismiss();
        }
    }
}
