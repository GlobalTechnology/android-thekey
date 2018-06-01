package me.thekey.android.lib.support.v4.fragment;

import android.support.v4.app.Fragment;

import me.thekey.android.view.AbstractBuilder;

public final class FragmentBuilder<T extends Fragment> extends AbstractBuilder<T> {
    private final Class<T> clazz;

    public FragmentBuilder(final Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T build() {
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
