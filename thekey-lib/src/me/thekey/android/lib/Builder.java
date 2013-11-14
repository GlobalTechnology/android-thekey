package me.thekey.android.lib;

public interface Builder<T> {
    // settings that can be set
    public static final String OPT_CAS_SERVER = "cas_server";
    public static final String OPT_CLIENT_ID = "client_id";

    Builder<T> casServer(String server);

    Builder<T> clientId(long id);

    T build();

    void start();
}
