package me.thekey.android.localbroadcast;

import android.content.IntentFilter;
import android.net.Uri;
import android.os.PatternMatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static me.thekey.android.localbroadcast.Constants.ACTION_ATTRIBUTES_LOADED;
import static me.thekey.android.localbroadcast.Constants.ACTION_CHANGE_DEFAULT_SESSION;
import static me.thekey.android.localbroadcast.Constants.ACTION_LOGIN;
import static me.thekey.android.localbroadcast.Constants.ACTION_LOGOUT;

public final class BroadcastUtils {
    private static final Uri URI_THEKEY = Uri.parse("thekey://");

    @NonNull
    private static Uri theKeyUri() {
        return URI_THEKEY;
    }

    @NonNull
    @RestrictTo(LIBRARY)
    static Uri theKeyUri(@NonNull final String guid) {
        return URI_THEKEY.buildUpon().appendPath(guid).build();
    }

    private static void addDataUri(@NonNull final IntentFilter filter, @NonNull final Uri uri, final int type) {
        final String scheme = uri.getScheme();
        if (scheme != null) {
            filter.addDataScheme(scheme);
        }
        final String host = uri.getHost();
        if (host != null) {
            filter.addDataAuthority(host, null);
        }
        final String path = uri.getPath();
        if (path != null) {
            filter.addDataPath(path, type);
        }
    }

    @NonNull
    private static IntentFilter genericFilter(@NonNull final String action, @Nullable final String guid) {
        final IntentFilter filter = new IntentFilter(action);
        if (guid == null) {
            addDataUri(filter, theKeyUri(), PatternMatcher.PATTERN_PREFIX);
        } else {
            addDataUri(filter, theKeyUri(guid), PatternMatcher.PATTERN_LITERAL);
        }
        return filter;
    }

    @NonNull
    public static IntentFilter loginFilter(@Nullable final String guid) {
        return genericFilter(ACTION_LOGIN, guid);
    }

    @NonNull
    public static IntentFilter logoutFilter(@Nullable final String guid) {
        return genericFilter(ACTION_LOGOUT, guid);
    }

    @NonNull
    public static IntentFilter changeDefaultSessionFilter() {
        return genericFilter(ACTION_CHANGE_DEFAULT_SESSION, null);
    }

    @NonNull
    public static IntentFilter attributesLoadedFilter(@Nullable final String guid) {
        return genericFilter(ACTION_ATTRIBUTES_LOADED, guid);
    }
}
