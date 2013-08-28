package org.ccci.gto.android.thekey.dialog;

import org.ccci.gto.android.thekey.DisplayUtil;
import org.ccci.gto.android.thekey.R;
import org.ccci.gto.android.thekey.TheKeyImpl;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.FrameLayout;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LoginDialogFragment extends android.app.DialogFragment implements DialogFragment {
    public final static String ARG_CASSERVER = "org.ccci.gto.android.thekey.CAS_SERVER";
    public final static String ARG_CLIENTID = "org.ccci.gto.android.thekey.CLIENT_ID";

    private TheKeyImpl thekey;

    // login WebView
    private FrameLayout frame = null;
    private WebView loginView = null;

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
            this.thekey = new TheKeyImpl(this.getActivity(), clientId, casServer);
        } else {
            this.thekey = new TheKeyImpl(this.getActivity(), clientId);
        }
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

        // build dialog
        final FrameLayout frame = (FrameLayout) LayoutInflater.from(this.getActivity()).inflate(R.layout.thekey_login,
                null);
        this.attachLoginView(frame);
        builder.setView(frame);

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        // Work around bug:
        // http://code.google.com/p/android/issues/detail?id=17423
        final Dialog dialog = this.getDialog();
        if ((dialog != null) && this.getRetainInstance())
            dialog.setDismissMessage(null);

        super.onDestroyView();
    }

    private void attachLoginView(final FrameLayout frame) {
        this.detachLoginView();

        // create a loginView if it doesn't exist already
        if (this.loginView == null) {
            this.loginView = DisplayUtil.createLoginWebView(this.getActivity(), this.thekey,
                    new LoginDialogWebViewClient(this, this.thekey));
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

    public interface Listener extends LoginDialogListener<LoginDialogFragment> {
    }
}
