package me.thekey.android.view.dialog;

import android.support.annotation.RestrictTo;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

@RestrictTo(LIBRARY)
public interface LoginDialogListener<T extends DialogFragmentCompat> {
    void onLoginSuccess(T dialog, String guid);

    void onLoginFailure(T dialog);
}
