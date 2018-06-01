package me.thekey.android.view.dialog;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import me.thekey.android.core.CodeGrantAsyncTask;
import me.thekey.android.core.TheKeyImpl;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

@RestrictTo(LIBRARY)
public final class LoginDialogCodeGrantAsyncTask extends CodeGrantAsyncTask {
    private final DialogFragmentCompat mDialog;

    public LoginDialogCodeGrantAsyncTask(final DialogFragmentCompat dialog, @NonNull final TheKeyImpl thekey,
                                         @NonNull final String code, @Nullable final String state) {
        super(thekey, null, code, state);
        mDialog = dialog;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onPostExecute(final String guid) {
        super.onPostExecute(guid);

        final Activity activity = mDialog.getActivity();
        if (activity instanceof LoginDialogListener) {
            // trigger the correct callback
            if (guid != null) {
                ((LoginDialogListener<DialogFragmentCompat>) activity).onLoginSuccess(mDialog, guid);
            } else {
                ((LoginDialogListener<DialogFragmentCompat>) activity).onLoginFailure(mDialog);
            }
        }

        // close the mDialog if it is still active (added to the activity)
        if (mDialog.isAdded()) {
            mDialog.dismissAllowingStateLoss();
        }
    }
}
