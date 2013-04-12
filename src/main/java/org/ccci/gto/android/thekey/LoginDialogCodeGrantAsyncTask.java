package org.ccci.gto.android.thekey;

import android.app.Activity;

public final class LoginDialogCodeGrantAsyncTask extends CodeGrantAsyncTask {
    final DialogFragment dialog;

    public LoginDialogCodeGrantAsyncTask(final DialogFragment dialog, final TheKey thekey) {
        super(thekey);
        this.dialog = dialog;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onPostExecute(final Boolean result) {
        super.onPostExecute(result);

        final Activity activity = this.dialog.getActivity();
        if (activity instanceof LoginDialogListener) {
            // trigger the correct callback
            if (result.booleanValue()) {
                ((LoginDialogListener<DialogFragment>) activity).onLoginSuccess(dialog, this.thekey.getGuid());
            } else {
                ((LoginDialogListener<DialogFragment>) activity).onLoginFailure(dialog);
            }
        }

        // close the dialog
        dialog.dismiss();
    }
}
