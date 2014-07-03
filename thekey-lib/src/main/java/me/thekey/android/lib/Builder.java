package me.thekey.android.lib;

public interface Builder<T> {
    Builder<T> casServer(String server);

    Builder<T> clientId(long id);

    Builder<T> selfService(boolean enable);

    T build();

    void start();
}
