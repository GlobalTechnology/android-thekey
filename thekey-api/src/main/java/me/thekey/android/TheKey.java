package me.thekey.android;

import android.content.Context;
import android.net.Uri;

import java.lang.reflect.InvocationTargetException;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import timber.log.Timber;

public interface TheKey extends TheKeyServices, TheKeySessions, TheKeyAttributeApi, TheKeyTicketApi, TheKeyTokenApi {
    long INVALID_CLIENT_ID = -1;

    // OAuth params
    String PARAM_CODE = "code";
    String PARAM_SCOPE = "scope";
    String PARAM_STATE = "state";

    // RFC-7636 PKCE
    String PARAM_CODE_CHALLENGE_METHOD = "code_challenge_method";
    String PARAM_CODE_CHALLENGE = "code_challenge";
    String CODE_CHALLENGE_METHOD_S256 = "S256";

    /**
     * @return the configured default redirect_uri
     */
    @NonNull
    @AnyThread
    Uri getDefaultRedirectUri();

    /**
     * @return a builder that will generate a login URI.
     */
    @NonNull
    @AnyThread
    LoginUriBuilder loginUriBuilder();

    @NonNull
    @AnyThread
    static TheKey getInstance(@NonNull final Context context) {
        TheKey instance = null;
        try {
            instance = (TheKey) Class
                    .forName("me.thekey.android.core.TheKeyImpl")
                    .getMethod("getInstance", Context.class)
                    .invoke(null, context);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (final Exception e) {
            Timber.tag("TheKey")
                    .d(e, "Unable to get instance of TheKeyImpl");
        }

        return instance != null ? instance : NoOpTheKey.INSTANCE;
    }
}
