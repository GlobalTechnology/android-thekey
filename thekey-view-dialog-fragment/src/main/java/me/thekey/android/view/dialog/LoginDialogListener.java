package me.thekey.android.view.dialog;

public interface LoginDialogListener<T extends DialogFragmentCompat> {
    void onLoginSuccess(T dialog, String guid);

    void onLoginFailure(T dialog);
}
