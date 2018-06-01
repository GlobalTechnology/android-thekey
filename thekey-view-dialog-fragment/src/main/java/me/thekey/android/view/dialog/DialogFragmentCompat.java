package me.thekey.android.view.dialog;

import android.app.Activity;
import android.support.annotation.RestrictTo;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

@RestrictTo(LIBRARY_GROUP)
public interface DialogFragmentCompat {
    void dismissAllowingStateLoss();

    Activity getActivity();

    boolean isAdded();
}
