package me.thekey.android.lib.dialog;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.widget.FrameLayout;

import me.thekey.android.lib.Builder;
import me.thekey.android.lib.R;
import me.thekey.android.lib.fragment.FragmentBuilder;
import me.thekey.android.lib.util.DisplayUtil;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LoginDialogFragment extends DialogFragment implements me.thekey.android.lib.fragment.DialogFragment {
    // login WebView
    private FrameLayout frame = null;
    private WebView loginView = null;

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

        // create a loginView if it doesn't exist already
        if (this.loginView == null) {
            final Bundle args = getArguments();
            this.loginView = DisplayUtil.createLoginWebView(getActivity(), new LoginDialogWebViewClient(this, args));
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
