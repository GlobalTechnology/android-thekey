package me.thekey.android.core;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import org.jetbrains.annotations.Contract;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static me.thekey.android.core.Constants.ARG_REDIRECT_URI;
import static me.thekey.android.core.Constants.ARG_SELF_SERVICE;

@RestrictTo(LIBRARY_GROUP)
public final class ArgumentUtils {
//    public static Uri getRedirectUri(@Nullable final Bundle args) {
//        return getRedirectUri(args, null);
//    }
//
    @Nullable
    @Contract("_, !null -> !null")
    public static Uri getRedirectUri(@Nullable final Bundle args, @Nullable final Uri defValue) {
        final Uri redirectUri = args != null ? args.getParcelable(ARG_REDIRECT_URI) : null;
        return redirectUri != null ? redirectUri : defValue;
    }

    public static boolean isSelfServiceEnabled(@Nullable final Bundle args) {
        return args != null && args.getBoolean(ARG_SELF_SERVICE, false);
    }
}
