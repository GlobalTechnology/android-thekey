package me.thekey.android.view.dialog;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.DialogFragment;

import me.thekey.android.core.CodeGrantAsyncTask;
import me.thekey.android.core.TheKeyImpl;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

@RestrictTo(LIBRARY)
public final class LoginDialogCodeGrantAsyncTask extends CodeGrantAsyncTask {
    private final DialogFragment mDialog;

    public LoginDialogCodeGrantAsyncTask(final DialogFragment dialog, @NonNull final TheKeyImpl thekey,
                                         @NonNull final String code, @Nullable final String state) {
        super(thekey, null, code, state);
        mDialog = dialog;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onPostExecute(final String guid) {
        super.onPostExecute(guid);

        final Activity activity = mDialog.getActivity();
        if (activity instanceof LoginDialogFragment.Listener) {
            // trigger the correct callback
            if (guid != null) {
                ((LoginDialogFragment.Listener) activity).onLoginSuccess(mDialog, guid);
            } else {
                ((LoginDialogFragment.Listener) activity).onLoginFailure(mDialog);
            }
        }

        // close the mDialog if it is still active (added to the activity)
        if (mDialog.isAdded()) {
            mDialog.dismissAllowingStateLoss();
        }
    }
}
