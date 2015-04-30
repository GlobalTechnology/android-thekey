package me.thekey.android.lib;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static me.thekey.android.lib.Constant.ARG_CAS_SERVER;
import static me.thekey.android.lib.Constant.ARG_CLIENT_ID;
import static me.thekey.android.lib.Constant.CAS_SERVER;
import static me.thekey.android.lib.Constant.OAUTH_GRANT_TYPE_AUTHORIZATION_CODE;
import static me.thekey.android.lib.Constant.OAUTH_GRANT_TYPE_REFRESH_TOKEN;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_ACCESS_TOKEN;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_CLIENT_ID;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_CODE;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_GRANT_TYPE;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_REDIRECT_URI;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_REFRESH_TOKEN;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_STATE;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_THEKEY_GUID;
import static me.thekey.android.lib.Constant.REDIRECT_URI;
import static me.thekey.android.lib.Constant.THEKEY_PARAM_SERVICE;
import static me.thekey.android.lib.Constant.THEKEY_PARAM_TICKET;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeaderValueParser;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import me.thekey.android.TheKey;
import me.thekey.android.TheKeyContext;
import me.thekey.android.TheKeyInvalidSessionException;
import me.thekey.android.TheKeySocketException;
import me.thekey.android.lib.util.BroadcastUtils;

/**
 * The Key interaction library, handles all interactions with The Key OAuth API
 * endpoints and correctly stores/utilizes OAuth access_tokens locally
 */
public abstract class TheKeyImpl implements TheKey {
    private static final Object INSTANCE_LOCK = new Object();
    private static TheKeyImpl INSTANCE = null;

    final Object mLockAuth = new Object();

    @NonNull
    final Context mContext;
    @NonNull
    final Configuration mConfig;
    @NonNull
    private final Uri mServer;
    private final long mClientId;

    TheKeyImpl(@NonNull final Context context, @NonNull final Configuration config) {
        mContext = context;
        mConfig = config;
        mServer = mConfig.mServer;
        mClientId = mConfig.mClientId;
        if (mClientId == INVALID_CLIENT_ID) {
            throw new IllegalStateException("client_id is invalid or not provided");
        }
    }

    @NonNull
    public static TheKeyImpl getInstance(@NonNull final Context context, @NonNull final Configuration config) {
        synchronized (INSTANCE_LOCK) {
            if (INSTANCE == null) {
                if (TextUtils.isEmpty(config.mAccountType)) {
                    INSTANCE = new PreferenceTheKeyImpl(context.getApplicationContext(), config);
                } else {
                    INSTANCE = new AccountManagerTheKeyImpl(context.getApplicationContext(), config);
                }
            }
        }

        // do we support reconfiguring TheKey?
        if (!INSTANCE.mConfig.equals(config)) {
            throw new IllegalArgumentException("Configuration cannot be changed after TheKeyImpl is initialized");
        }

        return INSTANCE;
    }

    @NonNull
    public static TheKey getInstance(@NonNull Context context) {
        synchronized (INSTANCE_LOCK) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
        }

        // support legacy lookup of TheKey object
        // deprecated
        while (true) {
            // short-circuit if this context is a TheKeyContext
            if (context instanceof TheKeyContext) {
                return ((TheKeyContext) context).getTheKey();
            }

            // check the ApplicationContext (if we haven't already)
            final Context old = context;
            context = context.getApplicationContext();
            if (context == old) {
                break;
            }
        }

        throw new IllegalStateException("TheKeyImpl has not been initialized yet!");
    }

    /**
     * @hide
     */
    @NonNull
    @Deprecated
    public static TheKeyImpl getInstance(@NonNull final Context context, @NonNull final Bundle args) {
        return getInstance(context, Configuration.base().clientId(args.getLong(ARG_CLIENT_ID, INVALID_CLIENT_ID))
                .server(args.getString(ARG_CAS_SERVER)));
    }

    @NonNull
    @Deprecated
    public static TheKeyImpl getInstance(@NonNull final Context context, final long clientId) {
        return getInstance(context, Configuration.base().clientId(clientId));
    }

    @NonNull
    @Deprecated
    public static TheKeyImpl getInstance(@NonNull final Context context, @Nullable final String server,
                                         final long clientId) {
        return getInstance(context, Configuration.base().server(server).clientId(clientId));
    }

    @NonNull
    @Deprecated
    public static TheKeyImpl getInstance(@NonNull final Context context, @NonNull final Uri server,
                                         final long clientId) {
        return getInstance(context, Configuration.base().server(server).clientId(clientId));
    }

    @Nullable
    @Override
    @Deprecated
    public final String getGuid() {
        return getDefaultSessionGuid();
    }

    @Override
    public void setDefaultSession(@NonNull final String guid) throws TheKeyInvalidSessionException {
        if (!guid.equals(getDefaultSessionGuid())) {
            throw new TheKeyInvalidSessionException();
        }
    }

    @Override
    public boolean isValidSession(@Nullable final String guid) {
        return guid != null && guid.equals(getDefaultSessionGuid());
    }

    @NonNull
    @Override
    public Attributes getAttributes() {
        return getAttributes(getDefaultSessionGuid());
    }

    @Override
    public boolean loadAttributes() throws TheKeySocketException {
        return loadAttributes(getDefaultSessionGuid());
    }

    @Nullable
    @Override
    public String getTicket(@NonNull final String service) throws TheKeySocketException {
        final String guid = getDefaultSessionGuid();
        return guid != null ? getTicket(guid, service) : null;
    }

    @Nullable
    @Override
    @Deprecated
    public TicketAttributesPair getTicketAndAttributes(@NonNull final String service) throws TheKeySocketException {
        // short-circuit if there isn't a default session
        final String guid = getDefaultSessionGuid();
        if (guid == null) {
            return null;
        }

        final String ticket = getTicket(guid, service);
        if (ticket != null) {
            return new TicketAttributesPair(ticket, getAttributes(guid));
        }

        return null;
    }

    @Override
    public void logout() {
        final String guid = getDefaultSessionGuid();
        if (guid != null) {
            logout(guid);
        }
    }

    Uri getCasUri(final String... segments) {
        final Builder uri = mServer.buildUpon();
        for (final String segment : segments) {
            uri.appendPath(segment);
        }
        return uri.build();
    }

    /**
     * @hide
     */
    public Uri getAuthorizeUri() {
        return this.getAuthorizeUri(null);
    }

    private Uri getAuthorizeUri(final String state) {
        // build oauth authorize url
        final Builder uri = this.getCasUri("oauth", "authorize").buildUpon()
                .appendQueryParameter(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId))
                .appendQueryParameter(OAUTH_PARAM_REDIRECT_URI, REDIRECT_URI.toString());
        if (state != null) {
            uri.appendQueryParameter(OAUTH_PARAM_STATE, state);
        }

        return uri.build();
    }

    @Override
    public boolean loadAttributes(@Nullable final String guid) throws TheKeySocketException {
        if (guid == null) {
            return false;
        }

        String accessToken;
        while ((accessToken = getValidAccessToken(guid, 0)) != null) {
            // request the attributes from CAS
            HttpsURLConnection conn = null;
            try {
                // generate & send request
                final Uri attrsUri = this.getCasUri("api", "oauth", "attributes").buildUpon()
                        .appendQueryParameter(OAUTH_PARAM_ACCESS_TOKEN, accessToken).build();
                conn = (HttpsURLConnection) new URL(attrsUri.toString()).openConnection();

                if (conn.getResponseCode() == HTTP_OK) {
                    // parse the json response
                    final JSONObject json = this.parseJsonResponse(conn.getInputStream());

                    storeAttributes(guid, json);

                    // broadcast that we just loaded the attributes
                    BroadcastUtils.broadcastAttributesLoaded(mContext, guid);

                    // return that attributes were loaded
                    return true;
                } else if (conn.getResponseCode() == HTTP_UNAUTHORIZED) {
                    // parse the Authenticate header
                    String scheme = null;
                    NameValuePair[] params = new NameValuePair[0];
                    String auth = conn.getHeaderField("WWW-Authenticate");
                    if (auth != null) {
                        auth = auth.trim();
                        int i = auth.indexOf(" ");
                        if (i == -1) {
                            scheme = auth;
                        } else {
                            scheme = auth.substring(0, i);
                            auth = auth.substring(i).trim();
                            params = BasicHeaderValueParser.parseParameters(auth, BasicHeaderValueParser.DEFAULT);
                        }

                        scheme = scheme.toUpperCase(Locale.US);
                    }

                    // OAuth Bearer auth
                    if ("BEARER".equals(scheme)) {
                        // extract the error encountered
                        String error = null;
                        for (final NameValuePair param : params) {
                            if (param != null && "error".equals(param.getName())) {
                                error = param.getValue();
                            }
                        }

                        if ("insufficient_scope".equals(error)) {
                            removeAttributes(guid);
                            return false;
                        } else if ("invalid_token".equals(error)) {
                            removeAccessToken(guid, accessToken);
                            continue;
                        }
                    }
                }
            } catch (final MalformedURLException e) {
                throw new RuntimeException("malformed CAS URL", e);
            } catch (final IOException e) {
                throw new TheKeySocketException("connect error", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            // the access token didn't work, remove it and restart processing
            removeAccessToken(guid, accessToken);
        }

        return false;
    }

    abstract void storeAttributes(@NonNull String guid, @NonNull JSONObject json);

    abstract void removeAttributes(@NonNull String guid);

    @Nullable
    @Override
    public String getTicket(@NonNull final String guid, @NonNull final String service) throws TheKeySocketException {
        String accessToken;
        while ((accessToken = getValidAccessToken(guid, 0)) != null) {
            // fetch a ticket
            final String ticket = getTicketWithAccessToken(accessToken, service);
            if (ticket != null) {
                return ticket;
            }

            // the access token didn't work, remove it and restart processing
            removeAccessToken(guid, accessToken);
        }

        // the user needs to authenticate before a ticket can be retrieved
        return null;
    }

    @Nullable
    private String getTicketWithAccessToken(@NonNull final String accessToken, @NonNull final String service)
            throws TheKeySocketException {
        HttpsURLConnection conn = null;
        try {
            // generate & send request
            final Uri ticketUri = getCasUri("api", "oauth", "ticket").buildUpon()
                    .appendQueryParameter(OAUTH_PARAM_ACCESS_TOKEN, accessToken)
                    .appendQueryParameter(THEKEY_PARAM_SERVICE, service).build();
            conn = (HttpsURLConnection) new URL(ticketUri.toString()).openConnection();

            // parse the json response if we have a valid response
            if (conn.getResponseCode() == 200) {
                final JSONObject json = parseJsonResponse(conn.getInputStream());
                return json.optString(THEKEY_PARAM_TICKET, null);
            }
        } catch (final MalformedURLException e) {
            throw new RuntimeException("malformed CAS URL", e);
        } catch (final IOException e) {
            throw new TheKeySocketException("connect error", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    @Nullable
    abstract String getAccessToken(@NonNull String guid);

    @Nullable
    abstract String getRefreshToken(@NonNull String guid);

    @Nullable
    private String getValidAccessToken(@NonNull final String guid, final int depth)
            throws TheKeySocketException {
        // prevent infinite recursion
        if (depth > 2) {
            return null;
        }

        synchronized (mLockAuth) {
            // check for an existing accessToken
            final String accessToken = getAccessToken(guid);
            if (accessToken != null) {
                return accessToken;
            }

            // try fetching a new access_token using a refresh_token
            final String refreshToken = getRefreshToken(guid);
            if (refreshToken != null) {
                if (processRefreshTokenGrant(guid, refreshToken)) {
                    return getValidAccessToken(guid, depth + 1);
                }

                // the refresh_token isn't valid anymore
                removeRefreshToken(guid, refreshToken);
            }

            // no valid access_token was found, clear auth state
            clearAuthState(guid);
        }

        return null;
    }

    @Override
    public void logout(@NonNull final String guid) {
        // clearAuthState() may block on synchronization, so process the call on a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                clearAuthState(guid);
            }
        }).start();
    }

    abstract void removeAccessToken(@NonNull String guid, @NonNull String token);

    abstract void removeRefreshToken(@NonNull String guid, @NonNull String token);

    abstract void clearAuthState(@NonNull String guid);

    boolean processCodeGrant(final String code, final Uri redirectUri) throws TheKeySocketException {
        final Uri tokenUri = this.getCasUri("api", "oauth", "token");
        HttpsURLConnection conn = null;
        try {
            // generate & send request
            conn = (HttpsURLConnection) new URL(tokenUri.toString()).openConnection();
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            final byte[] data = (encodeParam(OAUTH_PARAM_GRANT_TYPE, OAUTH_GRANT_TYPE_AUTHORIZATION_CODE) + "&" +
                    encodeParam(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId)) + "&" +
                    encodeParam(OAUTH_PARAM_REDIRECT_URI, redirectUri.toString()) + "&" +
                    encodeParam(OAUTH_PARAM_CODE, code)).getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(data.length);
            conn.getOutputStream().write(data);

            if (conn.getResponseCode() == HTTP_OK) {
                // parse the json response
                final JSONObject json = this.parseJsonResponse(conn.getInputStream());
                final String guid = json.optString(OAUTH_PARAM_THEKEY_GUID, null);
                if (guid != null) {
                    storeGrants(guid, json);
                    return true;
                }
            }
        } catch (final MalformedURLException e) {
            throw new RuntimeException("invalid CAS URL", e);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding??? this shouldn't happen", e);
        } catch (final IOException e) {
            throw new TheKeySocketException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return false;
    }

    private boolean processRefreshTokenGrant(@NonNull final String guid, @NonNull final String refreshToken)
            throws TheKeySocketException {
        final Uri tokenUri = this.getCasUri("api", "oauth", "token");
        HttpsURLConnection conn = null;
        try {
            // generate & send request
            conn = (HttpsURLConnection) new URL(tokenUri.toString()).openConnection();
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            final byte[] data = (encodeParam(OAUTH_PARAM_GRANT_TYPE, OAUTH_GRANT_TYPE_REFRESH_TOKEN) + "&" +
                    encodeParam(OAUTH_PARAM_CLIENT_ID, Long.toString(mClientId)) + "&" +
                    encodeParam(OAUTH_PARAM_REFRESH_TOKEN, refreshToken)).getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(data.length);
            conn.getOutputStream().write(data);

            // store the grants in the JSON response
            if (conn.getResponseCode() == HTTP_OK) {
                storeGrants(guid, parseJsonResponse(conn.getInputStream()));
            } else {
                return false;
            }

            // return success
            return true;
        } catch (final MalformedURLException e) {
            throw new RuntimeException("invalid CAS URL", e);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding??? this shouldn't happen", e);
        } catch (final IOException e) {
            throw new TheKeySocketException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    abstract boolean storeGrants(@NonNull String guid, @NonNull JSONObject json);

    @SuppressWarnings("deprecation")
    private String encodeParam(final String name, final String value) {
        try {
            return URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            // we should never get here, but if for some odd reason we do, use
            // the default encoder which should be UTF-8
            return URLEncoder.encode(name) + "=" + URLEncoder.encode(value);
        }
    }

    @NonNull
    private JSONObject parseJsonResponse(final InputStream in) {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            final StringBuilder json = new StringBuilder();
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                json.append(buffer);
            }
            return new JSONObject(json.toString());
        } catch (final Exception e) {
            // return an empty object on error
            return new JSONObject();
        }
    }

    public static final class Configuration {
        @NonNull
        final Uri mServer;
        final long mClientId;
        @Nullable
        final String mAccountType;

        private Configuration(@Nullable final Uri server, final long id, @Nullable final String accountType) {
            mServer = server != null ? server : CAS_SERVER;
            mClientId = id;
            mAccountType = accountType;
        }

        public static Configuration base() {
            return new Configuration(null, INVALID_CLIENT_ID, null);
        }

        public Configuration server(@Nullable final String server) {
            return new Configuration(server != null ? Uri.parse(server + (server.endsWith("/") ? "" : "/")) : null,
                                     mClientId, mAccountType);
        }

        public Configuration server(@Nullable final Uri server) {
            return new Configuration(server, mClientId, mAccountType);
        }

        public Configuration clientId(final long id) {
            return new Configuration(mServer, id, mAccountType);
        }

        public Configuration accountType(@Nullable final String type) {
            return new Configuration(mServer, mClientId, type);
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Configuration)) {
                return false;
            }
            final Configuration that = (Configuration) o;
            return mClientId == that.mClientId && mServer.equals(that.mServer) &&
                    TextUtils.equals(mAccountType, that.mAccountType);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[] {mServer, mClientId, mAccountType});
        }
    }

    private static final class InstanceKey {
        @NonNull
        private final Uri mServer;
        private final long mId;
        @Nullable
        private final String mAccountType;

        private InstanceKey(@NonNull final Uri server, final long clientId, @Nullable final String accountType) {
            mServer = server;
            mId = clientId;
            mAccountType = accountType;
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof InstanceKey)) {
                return false;
            }
            final InstanceKey that = (InstanceKey) o;
            return this.mId == that.mId && this.mServer.equals(that.mServer) &&
                    TextUtils.equals(this.mAccountType, that.mAccountType);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[] {mServer, mId, mAccountType});
        }
    }
}
