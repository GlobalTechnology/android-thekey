package me.thekey.android.lib.fragment;

import android.app.Activity;

public interface DialogFragment {
    void dismiss();

    Activity getActivity();

    boolean isAdded();
}
