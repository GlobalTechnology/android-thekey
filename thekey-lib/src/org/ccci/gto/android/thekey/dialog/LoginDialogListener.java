package org.ccci.gto.android.thekey.dialog;

public interface LoginDialogListener<T extends DialogFragment> {
    void onLoginSuccess(T dialog, String guid);

    void onLoginFailure(T dialog);
}
