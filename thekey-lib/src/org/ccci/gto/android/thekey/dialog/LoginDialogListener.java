package org.ccci.gto.android.thekey.dialog;

import me.thekey.android.lib.fragment.DialogFragment;

public interface LoginDialogListener<T extends DialogFragment> {
    void onLoginSuccess(T dialog, String guid);

    void onLoginFailure(T dialog);
}
