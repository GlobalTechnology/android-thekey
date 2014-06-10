package me.thekey.android.lib.fragment;

import me.thekey.android.lib.AbstractBuilder;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public final class FragmentBuilder<T extends Fragment> extends AbstractBuilder<T> {
    private final Class<T> clazz;

    public FragmentBuilder(final Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public final T build() {
        try {
            final T fragment = clazz.newInstance();
            fragment.setArguments(mArgs);
            return fragment;
        } catch (final InstantiationException e) {
            // propagate exception as a RuntimeException
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e) {
            // propagate exception as a RuntimeException
            throw new RuntimeException(e);
        }
    }
}