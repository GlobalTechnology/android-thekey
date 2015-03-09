package me.thekey.android.lib.fragment;

import android.app.Activity;

public interface DialogFragment {
    void dismissAllowingStateLoss();

    Activity getActivity();

    boolean isAdded();
}
