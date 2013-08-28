package org.ccci.gto.android.thekey.dialog;

import org.ccci.gto.android.thekey.LoginWebViewClient;
import org.ccci.gto.android.thekey.TheKeyImpl;

import android.app.Activity;
import android.net.Uri;

public class LoginDialogWebViewClient extends LoginWebViewClient {
    final DialogFragment dialog;

    public LoginDialogWebViewClient(final DialogFragment dialog, final TheKeyImpl thekey) {
        super(dialog.getActivity(), thekey);
        this.dialog = dialog;
    }

    @Override
    protected void onAuthorizeSuccess(final Uri uri, final String code) {
        new LoginDialogCodeGrantAsyncTask(this.dialog, this.thekey).execute(code);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onAuthorizeError(final Uri uri, final String errorCode) {
        final Activity activity = this.dialog.getActivity();
        if (activity instanceof LoginDialogListener) {
            ((LoginDialogListener<DialogFragment>) activity).onLoginFailure(dialog);
        }

        dialog.dismiss();
    }
}
