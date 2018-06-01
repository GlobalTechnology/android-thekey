package me.thekey.android.view.dialog;

import android.app.Activity;
import android.support.annotation.RestrictTo;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

@RestrictTo(LIBRARY)
public interface DialogFragmentCompat {
    void dismissAllowingStateLoss();

    Activity getActivity();

    boolean isAdded();
}
