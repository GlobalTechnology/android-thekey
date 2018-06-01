package me.thekey.android.lib.support.v4.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.FrameLayout;

import me.thekey.android.lib.R;
import me.thekey.android.lib.dialog.LoginDialogListener;
import me.thekey.android.lib.dialog.LoginDialogWebViewClient;
import me.thekey.android.lib.support.v4.fragment.FragmentBuilder;
import me.thekey.android.lib.util.DisplayUtil;
import me.thekey.android.view.Builder;
import timber.log.Timber;

public class LoginDialogFragment extends DialogFragment implements me.thekey.android.lib.fragment.DialogFragment {
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());

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

    public interface Listener extends LoginDialogListener<LoginDialogFragment> {
    }
}
