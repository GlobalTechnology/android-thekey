package me.thekey.android.lib;

import static me.thekey.android.TheKey.INVALID_CLIENT_ID;
import static org.ccci.gto.android.thekey.Constant.CAS_SERVER;

public abstract class AbstractBuilder<T> implements Builder<T> {
    public AbstractBuilder() {
        this.casServer(CAS_SERVER.toString());
        this.clientId(INVALID_CLIENT_ID);
    }

    @Override
    public final Builder<T> casServer(final String server) {
        return this.setString(OPT_CAS_SERVER, server);
    }

    @Override
    public final Builder<T> clientId(final long id) {
        return this.setLong(OPT_CLIENT_ID, id);
    }

    protected abstract Builder<T> setLong(final String key, final long value);

    protected abstract Builder<T> setString(final String key, final String value);
}
