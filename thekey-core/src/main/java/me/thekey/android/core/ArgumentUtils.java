package me.thekey.android.core;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import org.jetbrains.annotations.Contract;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static me.thekey.android.core.Constants.ARG_REDIRECT_URI;
import static me.thekey.android.core.Constants.ARG_SELF_SERVICE;
import static me.thekey.android.core.Constants.ARG_SIGNUP;

@RestrictTo(LIBRARY_GROUP)
public final class ArgumentUtils {
    @Nullable
    @Contract("_, !null -> !null")
    public static Uri getRedirectUri(@Nullable final Bundle args, @Nullable final Uri defValue) {
        final Uri redirectUri = args != null ? args.getParcelable(ARG_REDIRECT_URI) : null;
        return redirectUri != null ? redirectUri : defValue;
    }

    public static boolean isSelfServiceEnabled(@Nullable final Bundle args) {
        return args != null && args.getBoolean(ARG_SELF_SERVICE, false);
    }

    public static boolean isSignup(@Nullable final Bundle args) {
        return args != null && args.getBoolean(ARG_SIGNUP, false);
    }
}
