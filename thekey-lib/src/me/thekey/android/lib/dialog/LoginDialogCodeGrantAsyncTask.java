package me.thekey.android.lib.dialog;

import me.thekey.android.lib.fragment.DialogFragment;

import me.thekey.android.lib.CodeGrantAsyncTask;
import me.thekey.android.lib.TheKeyImpl;

import android.app.Activity;

public final class LoginDialogCodeGrantAsyncTask extends CodeGrantAsyncTask {
    final DialogFragment dialog;

    public LoginDialogCodeGrantAsyncTask(final DialogFragment dialog, final TheKeyImpl thekey) {
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

        // close the dialog if it is still active (added to the activity)
        if (this.dialog.isAdded()) {
            this.dialog.dismiss();
        }
    }
}
