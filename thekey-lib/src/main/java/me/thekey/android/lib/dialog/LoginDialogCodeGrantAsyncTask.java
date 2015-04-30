package me.thekey.android.lib.dialog;

import android.app.Activity;
import android.support.annotation.NonNull;

import me.thekey.android.lib.CodeGrantAsyncTask;
import me.thekey.android.lib.TheKeyImpl;
import me.thekey.android.lib.fragment.DialogFragment;

public final class LoginDialogCodeGrantAsyncTask extends CodeGrantAsyncTask {
    final DialogFragment dialog;

    public LoginDialogCodeGrantAsyncTask(final DialogFragment dialog, @NonNull final TheKeyImpl thekey) {
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
            if (result) {
                ((LoginDialogListener<DialogFragment>) activity).onLoginSuccess(dialog, mTheKey.getGuid());
            } else {
                ((LoginDialogListener<DialogFragment>) activity).onLoginFailure(dialog);
            }
        }

        // close the dialog if it is still active (added to the activity)
        if (this.dialog.isAdded()) {
            this.dialog.dismissAllowingStateLoss();
        }
    }
}
