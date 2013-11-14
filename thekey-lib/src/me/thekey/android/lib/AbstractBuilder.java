package me.thekey.android.lib;

import static me.thekey.android.TheKey.INVALID_CLIENT_ID;
import static org.ccci.gto.android.thekey.Constant.CAS_SERVER;
import static org.ccci.gto.android.thekey.Constant.ARG_CAS_SERVER;
import static org.ccci.gto.android.thekey.Constant.ARG_CLIENT_ID;
import android.os.Bundle;

public abstract class AbstractBuilder<T> implements Builder<T> {
    protected final Bundle mArgs = new Bundle();

    public AbstractBuilder() {
        this.casServer(CAS_SERVER.toString());
        this.clientId(INVALID_CLIENT_ID);
    }

    @Override
    public final Builder<T> casServer(final String server) {
        mArgs.putString(ARG_CAS_SERVER, server);
        return this;
    }

    @Override
    public final Builder<T> clientId(final long id) {
        mArgs.putLong(ARG_CLIENT_ID, id);
        return this;
    }

    @Override
    public T build() {
        throw new UnsupportedOperationException("Cannot call build() on this Builder");
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException("Cannot call start() on this Builder");
    }
}
