package me.thekey.android.core;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.Base64;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;
import static android.util.Base64.NO_PADDING;
import static android.util.Base64.NO_WRAP;
import static android.util.Base64.URL_SAFE;

@RestrictTo(LIBRARY)
final class PkceUtils {
    private static final int BASE64_FLAGS = URL_SAFE | NO_WRAP | NO_PADDING;
    private static final Random RAND = new SecureRandom();

    @NonNull
    static String generateUrlSafeBase64String(final int bytesOfEntropy) {
        final byte[] bytes = new byte[bytesOfEntropy];
        RAND.nextBytes(bytes);
        return Base64.encodeToString(bytes, BASE64_FLAGS);
    }

    @NonNull
    static String generateVerifier() {
        return generateUrlSafeBase64String(32);
    }

    @NonNull
    static String encodeS256Challenge(@NonNull final String verifier) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] hash = md.digest(verifier.getBytes(Charset.forName("US-ASCII")));
            return Base64.encodeToString(hash, BASE64_FLAGS);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 isn't supported on this device", e);
        }
    }
}
