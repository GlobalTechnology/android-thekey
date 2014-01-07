package org.ccci.gto.android.thekey.dialog;

import me.thekey.android.lib.fragment.DialogFragment;

import org.ccci.gto.android.thekey.LoginWebViewClient;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

public class LoginDialogWebViewClient extends LoginWebViewClient {
    private final DialogFragment mDialog;

    public LoginDialogWebViewClient(final DialogFragment dialog, final Bundle args) {
        super(dialog.getActivity(), args);
        mDialog = dialog;
    }

    @Override
    protected void onAuthorizeSuccess(final Uri uri, final String code) {
        new LoginDialogCodeGrantAsyncTask(mDialog, mTheKey).execute(code);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onAuthorizeError(final Uri uri, final String errorCode) {
        final Activity activity = mDialog.getActivity();
        if (activity instanceof LoginDialogListener) {
            ((LoginDialogListener<DialogFragment>) activity).onLoginFailure(mDialog);
        }

        // close the dialog if it is still active (added to the activity)
        if (mDialog.isAdded()) {
            mDialog.dismiss();
        }
    }
}
