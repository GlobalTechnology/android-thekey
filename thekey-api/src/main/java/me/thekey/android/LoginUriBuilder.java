package me.thekey.android;

import android.net.Uri;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class LoginUriBuilder {
    private static final Pattern PATTERN_SCOPE = Pattern.compile("^[a-z]+$", Pattern.CASE_INSENSITIVE);

    protected static final String PARAM_ACTION = "action";
    protected static final String ACTION_SIGNUP = "signup";

    protected boolean mSignup = false;
    @Nullable
    protected Uri mRedirectUri = null;
    protected final Set<String> mScope = new HashSet<>();
    @Nullable
    protected String mState = null;

    /**
     * Should this login URI deeplink to account creation.
     *
     * @param state true to deeplink to account creation form, false to show normal login form.
     * @return the login URI builder.
     */
    @NonNull
    @AnyThread
    public LoginUriBuilder signup(final boolean state) {
        mSignup = state;
        return this;
    }

    /**
     * Set an explicit redirect_uri for this login URI.
     *
     * @param redirectUri The redirect_uri that a user will be returned to after authenticating.
     * @return the login URI builder.
     */
    @NonNull
    @AnyThread
    public LoginUriBuilder redirectUri(@Nullable final Uri redirectUri) {
        mRedirectUri = redirectUri;
        return this;
    }

    /**
     * Set an explicit scope for this login URI.
     *
     * @param scope a list of OAuth scopes to request with this authorize URI.
     * @return the login URI builder.
     */
    @NonNull
    @AnyThread
    public LoginUriBuilder scope(@NonNull final String... scope) {
        mScope.clear();
        return addScope(scope);
    }

    /**
     * Add additional explicit scope for this login URI.
     *
     * @param scope a list of OAuth scopes to request with this authorize URI.
     * @return the login URI builder.
     */
    @NonNull
    @AnyThread
    public LoginUriBuilder addScope(@NonNull final String... scope) {
        for (final String s : scope) {
            if (!PATTERN_SCOPE.matcher(s).matches()) {
                throw new IllegalArgumentException(s + " is not a valid scope value");
            }
            mScope.add(s);
        }

        return this;
    }

    /**
     * Set an explicit state for this login URI.
     *
     * @param state an opaque state string that can be used to associate generation of login URI and callback after
     *              authenticating.
     * @return the login URI builder.
     */
    @NonNull
    @AnyThread
    public LoginUriBuilder state(@Nullable final String state) {
        mState = state;
        return this;
    }

    /**
     * Build this OAuth login URI.
     *
     * @return The login URI
     */
    @NonNull
    @AnyThread
    public abstract Uri build();
}
