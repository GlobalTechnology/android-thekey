package me.thekey.android.core;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static me.thekey.android.TheKey.PARAM_CODE;
import static me.thekey.android.TheKey.PARAM_STATE;

@RestrictTo(LIBRARY)
class DataUriUtils {
    @NonNull
    static Uri removeCodeAndState(@NonNull final Uri uri) {
        String query = uri.getEncodedQuery();
        if (query != null) {
            query = query.replaceAll("(^|&)" + PARAM_CODE + "=[^&]*&?", "&");
            query = query.replaceAll("(^|&)" + PARAM_STATE + "=[^&]*&?", "&");

            // strip leading/trailing &
            while (query.startsWith("&")) {
                query = query.substring(1);
            }
            while (query.endsWith("&")) {
                query = query.substring(0, query.length() - 1);
            }

            // replace query
            return uri.buildUpon().encodedQuery(query).build();
        }

        return uri;
    }
}
