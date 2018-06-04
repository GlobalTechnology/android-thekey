package me.thekey.android;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

@RestrictTo(LIBRARY)
final class NoOpTheKey implements TheKey {
    static final TheKey INSTANCE = new NoOpTheKey();

    @NonNull
    @Override
    public Uri getDefaultRedirectUri() {
        throw new UnsupportedOperationException("NoOpTheKey.getDefaultRedirectUri() is not implemented");
    }

    @NonNull
    @Override
    public LoginUriBuilder loginUriBuilder() {
        throw new UnsupportedOperationException("NoOpTheKey.loginUriBuilder() is not implemented");
    }

    @Nullable
    @Override
    public String processCodeGrant(@NonNull final String code, @NonNull final Uri redirectUri,
                                   @Nullable final String state) {
        return null;
    }

    @Nullable
    @Override
    public String processPasswordGrant(@NonNull final String username, @NonNull final String password) {
        return null;
    }

    @NonNull
    @Override
    public Collection<String> getSessions() {
        return Collections.emptySet();
    }

    @Nullable
    @Override
    public String getDefaultSessionGuid() {
        return null;
    }

    @Override
    public boolean isValidSession(@Nullable final String guid) {
        return false;
    }

    @NonNull
    @Override
    public Attributes getAttributes(@Nullable final String guid) {
        return NoOpAttributes.INSTANCE;
    }

    @Override
    public void setDefaultSession(@NonNull final String guid) { }

    @Nullable
    @Override
    public String getTicket(@NonNull final String guid, @NonNull final String service) {
        return null;
    }

    @Override
    public boolean loadAttributes(@Nullable final String guid) {
        return false;
    }

    @Override
    public void logout(@NonNull final String guid) { }

    @RestrictTo(LIBRARY)
    static class NoOpAttributes implements Attributes {
        static final NoOpAttributes INSTANCE = new NoOpAttributes();

        private final Date mEpoch = new Date(0);

        @Nullable
        @Override
        public String getUsername() {
            return null;
        }

        @Nullable
        @Override
        public String getGuid() {
            return null;
        }

        @NonNull
        @Override
        public Date getLoadedTime() {
            return mEpoch;
        }

        @Override
        public boolean areValid() {
            return false;
        }

        @Nullable
        @Override
        public String getEmail() {
            return null;
        }

        @Nullable
        @Override
        public String getFirstName() {
            return null;
        }

        @Nullable
        @Override
        public String getLastName() {
            return null;
        }
    }
}
