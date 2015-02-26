package me.thekey.android.lib;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static me.thekey.android.lib.Constant.ARG_CAS_SERVER;
import static me.thekey.android.lib.Constant.ARG_CLIENT_ID;
import static me.thekey.android.lib.Constant.CAS_SERVER;
import static me.thekey.android.lib.Constant.OAUTH_GRANT_TYPE_AUTHORIZATION_CODE;
import static me.thekey.android.lib.Constant.OAUTH_GRANT_TYPE_REFRESH_TOKEN;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_ACCESS_TOKEN;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_ATTR_EMAIL;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_ATTR_FIRST_NAME;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_ATTR_GUID;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_ATTR_LAST_NAME;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_CLIENT_ID;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_CODE;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_EXPIRES_IN;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_GRANT_TYPE;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_REDIRECT_URI;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_REFRESH_TOKEN;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_STATE;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_THEKEY_GUID;
import static me.thekey.android.lib.Constant.REDIRECT_URI;
import static me.thekey.android.lib.Constant.THEKEY_PARAM_SERVICE;
import static me.thekey.android.lib.Constant.THEKEY_PARAM_TICKET;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeaderValueParser;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import me.thekey.android.TheKey;
import me.thekey.android.TheKeyContext;
import me.thekey.android.TheKeySocketException;
import me.thekey.android.lib.util.BroadcastUtils;

/**
 * The Key interaction library, handles all interactions with The Key OAuth API
 * endpoints and correctly stores/utilizes OAuth access_tokens locally
 */
public final class TheKeyImpl implements TheKey {
    private static final String PREFFILE_THEKEY = "thekey";
    private static final String PREF_ACCESS_TOKEN = "access_token";
    private static final String PREF_EXPIRE_TIME = "expire_time";
    private static final String PREF_GUID = "guid";
    private static final String PREF_REFRESH_TOKEN = "refresh_token";
    private static final String PREF_ATTR_LOAD_TIME = "attr_load_time";
    private static final String PREF_ATTR_GUID = "attr_guid";
    private static final String PREF_ATTR_EMAIL = "attr_email";
    private static final String PREF_ATTR_FIRST_NAME = "attr_firstName";
    private static final String PREF_ATTR_LAST_NAME = "attr_lastName";

    private static final Map<InstanceKey, TheKeyImpl> INSTANCES = new HashMap<InstanceKey, TheKeyImpl>();

    private final Object lock_auth = new Object();
    private final Object lock_attrs = new Object();

    @NonNull
    private final Context context;
    @NonNull
    private final Uri casServer;
    private final Long clientId;

    private TheKeyImpl(@NonNull final Context context, final long clientId, @NonNull final Uri casServer) {
        this.context = context;
        this.clientId = clientId;
        this.casServer = casServer;
    }

    @NonNull
    public static TheKey getInstance(@NonNull Context context) {
        while(true) {
            // short-circuit if this context is a TheKeyContext
            if (context instanceof TheKeyContext) {
                return ((TheKeyContext) context).getTheKey();
            }

            // check the ApplicationContext (if we haven't already)
            final Context old = context;
            context = context.getApplicationContext();
            if(context == old) {
                throw new UnsupportedOperationException("The provided Context hierarchy doesn't implement TheKeyContext");
            }
        }
    }

    /**
     * @hide
     */
    @NonNull
    public static TheKeyImpl getInstance(@NonNull final Context context, @NonNull final Bundle args) {
        final long id = args.getLong(ARG_CLIENT_ID, INVALID_CLIENT_ID);
        final String server = args.getString(ARG_CAS_SERVER);
        if (server != null) {
            return getInstance(context, server, id);
        } else {
            return getInstance(context, CAS_SERVER, id);
        }
    }

    @NonNull
    public static TheKeyImpl getInstance(@NonNull final Context context, final long clientId) {
        return getInstance(context, CAS_SERVER, clientId);
    }

    @NonNull
    public static TheKeyImpl getInstance(@NonNull final Context context, @Nullable final String server,
                                         final long clientId) {
        final Uri serverUri = server != null ? Uri.parse(server + (server.endsWith("/") ? "" : "/")) : CAS_SERVER;
        return getInstance(context, serverUri, clientId);
    }

    @NonNull
    public static TheKeyImpl getInstance(@NonNull final Context context, @NonNull final Uri server,
                                         final long clientId) {
        final InstanceKey key = new InstanceKey(server, clientId);
        TheKeyImpl thekey;
        synchronized (INSTANCES) {
            thekey = INSTANCES.get(key);
            if (thekey == null) {
                thekey = new TheKeyImpl(context.getApplicationContext(), clientId, server);
                INSTANCES.put(key, thekey);
            }
        }
        return thekey;
    }

    Uri getCasUri(final String... segments) {
        final Builder uri = this.casServer.buildUpon();
        for (final String segment : segments) {
            uri.appendPath(segment);
        }
        return uri.build();
    }

    private Long getClientId() {
        return this.clientId;
    }

    @Nullable
    public String getGuid() {
        return this.getPrefs().getString(PREF_GUID, null);
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
                .appendQueryParameter(OAUTH_PARAM_CLIENT_ID, this.getClientId().toString())
                .appendQueryParameter(OAUTH_PARAM_REDIRECT_URI, REDIRECT_URI.toString());
        if (state != null) {
            uri.appendQueryParameter(OAUTH_PARAM_STATE, state);
        }

        return uri.build();
    }

    @Override
    public boolean loadAttributes() throws TheKeySocketException {
        Pair<String, Attributes> credentials;
        while ((credentials = this.getValidAccessTokenAndAttributes(0)) != null) {
            // request the attributes from CAS
            HttpsURLConnection conn = null;
            try {
                // generate & send request
                final Uri attrsUri = this.getCasUri("api", "oauth", "attributes").buildUpon()
                        .appendQueryParameter(OAUTH_PARAM_ACCESS_TOKEN, credentials.first).build();
                conn = (HttpsURLConnection) new URL(attrsUri.toString()).openConnection();

                if (conn.getResponseCode() == HTTP_OK) {
                    // parse the json response
                    final JSONObject json = this.parseJsonResponse(conn.getInputStream());
                    storeAttributes(json);

                    // broadcast that we just loaded the attributes
                    BroadcastUtils.broadcastAttributesLoaded(context, json.optString(OAUTH_PARAM_ATTR_GUID, null));

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
                            this.removeAttributes();
                            return false;
                        } else if ("invalid_token".equals(error)) {
                            this.removeAccessToken(credentials.first);
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
            this.removeAccessToken(credentials.first);
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void storeAttributes(final JSONObject json) {
        final Editor prefs = this.getPrefs().edit();

        prefs.putLong(PREF_ATTR_LOAD_TIME, System.currentTimeMillis());
        prefs.putString(PREF_ATTR_GUID, json.optString(OAUTH_PARAM_ATTR_GUID, null));
        prefs.putString(PREF_ATTR_EMAIL, json.optString(OAUTH_PARAM_ATTR_EMAIL, null));
        prefs.putString(PREF_ATTR_FIRST_NAME, json.optString(OAUTH_PARAM_ATTR_FIRST_NAME, null));
        prefs.putString(PREF_ATTR_LAST_NAME, json.optString(OAUTH_PARAM_ATTR_LAST_NAME, null));

        // we synchronize this to prevent race conditions with getAttributes
        synchronized (this.lock_attrs) {
            // store updates
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                prefs.apply();
            } else {
                prefs.commit();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void removeAttributes() {
        final Editor prefs = this.getPrefs().edit();
        prefs.remove(PREF_ATTR_GUID);
        prefs.remove(PREF_ATTR_EMAIL);
        prefs.remove(PREF_ATTR_FIRST_NAME);
        prefs.remove(PREF_ATTR_LAST_NAME);
        prefs.remove(PREF_ATTR_LOAD_TIME);

        // we synchronize this to prevent race conditions with getAttributes
        synchronized (this.lock_attrs) {
            // store updates
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                prefs.apply();
            } else {
                prefs.commit();
            }
        }
    }

    @NonNull
    @Override
    public Attributes getAttributes() {
        synchronized (this.lock_attrs) {
            // return the attributes for the current OAuth session
            return new AttributesImpl(this.getPrefs().getAll());
        }
    }

    /**
     * This method returns a ticket for the specified service. This method is a
     * blocking method, this should never be called directly on the UI thread.
     *
     * @param service
     * @return The ticket
     */
    @Nullable
    @Override
    public String getTicket(@NonNull final String service) throws TheKeySocketException {
        final TicketAttributesPair ticketPair = this.getTicketAndAttributes(service);
        return ticketPair != null ? ticketPair.ticket : null;
    }

    /**
     * This method returns a ticket for the specified service and attributes the ticket was issued for. This is a
     * blocking method, this should never be called directly on the UI thread.
     *
     * @param service
     * @return The ticket &amp; attributes
     */
    @Nullable
    @Override
    public TicketAttributesPair getTicketAndAttributes(@NonNull final String service) throws TheKeySocketException {
        Pair<String, Attributes> credentials;
        while ((credentials = this.getValidAccessTokenAndAttributes(0)) != null) {
            // fetch a ticket
            final String ticket = this.getTicket(credentials.first, service);
            if (ticket != null) {
                return new TicketAttributesPair(ticket, credentials.second);
            }

            // the access token didn't work, remove it and restart processing
            this.removeAccessToken(credentials.first);
        }

        // the user needs to authenticate before a ticket can be retrieved
        return null;
    }

    private String getTicket(final String accessToken, final String service) throws TheKeySocketException {
        HttpsURLConnection conn = null;
        try {
            // generate & send request
            final Uri ticketUri = this.getCasUri("api", "oauth", "ticket").buildUpon()
                    .appendQueryParameter(OAUTH_PARAM_ACCESS_TOKEN, accessToken)
                    .appendQueryParameter(THEKEY_PARAM_SERVICE, service).build();
            conn = (HttpsURLConnection) new URL(ticketUri.toString()).openConnection();

            // parse the json response if we have a valid response
            if (conn.getResponseCode() == 200) {
                final JSONObject json = this.parseJsonResponse(conn.getInputStream());
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

    private Pair<String, Attributes> getAccessTokenAndAttributes() {
        final long currentTime = System.currentTimeMillis();

        synchronized (this.lock_attrs) {
            final Map<String, ?> attrs = this.getPrefs().getAll();
            final long expireTime;
            {
                final Long v = (Long) attrs.get(PREF_EXPIRE_TIME);
                expireTime = v != null ? v : currentTime;
            }
            final String accessToken = expireTime >= currentTime ? (String) attrs.get(PREF_ACCESS_TOKEN) : null;

            // return a pair only if we have an access_token
            return accessToken != null ? Pair.create(accessToken, (Attributes) new AttributesImpl(attrs)) : null;
        }
    }

    private String getRefreshToken() {
        return this.getPrefs().getString(PREF_REFRESH_TOKEN, null);
    }

    private Pair<String, Attributes> getValidAccessTokenAndAttributes(final int depth) throws TheKeySocketException {
        // prevent infinite recursion
        if (depth > 2) {
            return null;
        }

        synchronized (this.lock_auth) {
            // check for an existing accessToken
            final Pair<String, Attributes> credentials = this.getAccessTokenAndAttributes();
            if (credentials != null && credentials.first != null) {
                return credentials;
            }

            // try fetching a new access_token using a refresh_token
            final String refreshToken = this.getRefreshToken();
            if (refreshToken != null) {
                if (this.processRefreshTokenGrant(refreshToken)) {
                    return this.getValidAccessTokenAndAttributes(depth + 1);
                }

                // the refresh_token isn't valid anymore
                this.removeRefreshToken();
            }

            // no valid access_token was found, clear auth state
            this.clearAuthState();
        }

        return null;
    }

    @Override
    public void logout() {
        // clearAuthState() may block on synchronization, so process the call on a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                clearAuthState();
            }
        }).start();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void removeAccessToken(final String token) {
        synchronized (this.lock_auth) {
            if (token != null && token.equals(this.getPrefs().getString(PREF_ACCESS_TOKEN, null))) {
                final Editor prefs = this.getPrefs().edit();
                prefs.remove(PREF_ACCESS_TOKEN);
                prefs.remove(PREF_EXPIRE_TIME);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    prefs.apply();
                } else {
                    prefs.commit();
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void removeRefreshToken() {
        final Editor prefs = this.getPrefs().edit();
        prefs.remove(PREF_REFRESH_TOKEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.apply();
        } else {
            prefs.commit();
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void clearAuthState() {
        final Editor prefs = this.getPrefs().edit();
        prefs.remove(PREF_ACCESS_TOKEN);
        prefs.remove(PREF_REFRESH_TOKEN);
        prefs.remove(PREF_EXPIRE_TIME);
        prefs.remove(PREF_GUID);

        synchronized (this.lock_auth) {
            final String guid = this.getPrefs().getString(PREF_GUID, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                prefs.apply();
            } else {
                prefs.commit();
            }

            // broadcast a logout action if we had a guid
            if (guid != null) {
                BroadcastUtils.broadcastLogout(context, guid, false);
            }
        }
    }

    boolean processCodeGrant(final String code, final Uri redirectUri) throws TheKeySocketException {
        final Uri tokenUri = this.getCasUri("api", "oauth", "token");
        HttpsURLConnection conn = null;
        try {
            // generate & send request
            conn = (HttpsURLConnection) new URL(tokenUri.toString()).openConnection();
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            final byte[] data = (encodeParam(OAUTH_PARAM_GRANT_TYPE, OAUTH_GRANT_TYPE_AUTHORIZATION_CODE) + "&"
                    + encodeParam(OAUTH_PARAM_CLIENT_ID, this.clientId.toString()) + "&"
                    + encodeParam(OAUTH_PARAM_REDIRECT_URI, redirectUri.toString()) + "&" + encodeParam(
                    OAUTH_PARAM_CODE, code)).getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(data.length);
            conn.getOutputStream().write(data);

            // parse the json response
            final JSONObject json = this.parseJsonResponse(conn.getInputStream());
            this.storeGrants(json);

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

    private boolean processRefreshTokenGrant(final String refreshToken) throws TheKeySocketException {
        final Uri tokenUri = this.getCasUri("api", "oauth", "token");
        HttpsURLConnection conn = null;
        try {
            // generate & send request
            conn = (HttpsURLConnection) new URL(tokenUri.toString()).openConnection();
            conn.setDoOutput(true);
            conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            final byte[] data = (encodeParam(OAUTH_PARAM_GRANT_TYPE, OAUTH_GRANT_TYPE_REFRESH_TOKEN) + "&"
                    + encodeParam(OAUTH_PARAM_CLIENT_ID, this.clientId.toString()) + "&" + encodeParam(
                    OAUTH_PARAM_REFRESH_TOKEN, refreshToken)).getBytes("UTF-8");
            conn.setFixedLengthStreamingMode(data.length);
            conn.getOutputStream().write(data);

            // parse the json response
            final JSONObject json = this.parseJsonResponse(conn.getInputStream());
            this.storeGrants(json);

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

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void storeGrants(final JSONObject json) {
        try {
            final Editor prefs = this.getPrefs().edit();

            // store access_token
            if (json.has(OAUTH_PARAM_ACCESS_TOKEN)) {
                prefs.putString(PREF_ACCESS_TOKEN, json.getString(OAUTH_PARAM_ACCESS_TOKEN));
                prefs.remove(PREF_EXPIRE_TIME);
                if (json.has(OAUTH_PARAM_EXPIRES_IN)) {
                    prefs.putLong(PREF_EXPIRE_TIME, System.currentTimeMillis() + json.getLong(OAUTH_PARAM_EXPIRES_IN)
                            * 1000);
                }
                prefs.remove(PREF_GUID);
                if (json.has(OAUTH_PARAM_THEKEY_GUID)) {
                    prefs.putString(PREF_GUID, json.getString(OAUTH_PARAM_THEKEY_GUID));
                }
            }

            // store refresh_token
            if (json.has(OAUTH_PARAM_REFRESH_TOKEN)) {
                prefs.putString(PREF_REFRESH_TOKEN, json.getString(OAUTH_PARAM_REFRESH_TOKEN));
            }

            // we synchronize update to prevent race conditions
            synchronized (this.lock_auth) {
                final String oldGuid = this.getPrefs().getString(PREF_GUID, null);
                final String newGuid = json.optString(OAUTH_PARAM_THEKEY_GUID, null);

                // store updates
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    prefs.apply();
                } else {
                    prefs.commit();
                }

                // trigger logout/login broadcasts based on guid changes
                if (oldGuid != null && !oldGuid.equals(newGuid)) {
                    BroadcastUtils.broadcastLogout(context, oldGuid, newGuid != null);
                }
                if (newGuid != null && !newGuid.equals(oldGuid)) {
                    BroadcastUtils.broadcastLogin(context, newGuid);
                }
            }
        } catch (final JSONException e) {
            this.clearAuthState();
        }
    }

    private SharedPreferences getPrefs() {
        return this.context.getSharedPreferences(PREFFILE_THEKEY, Context.MODE_PRIVATE);
    }

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

    private static final class InstanceKey {
        private final Uri mServer;
        private final long mId;

        private InstanceKey(final Uri server, final long clientId) {
            mServer = server;
            mId = clientId;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof InstanceKey)) {
                return false;
            }
            final InstanceKey key = (InstanceKey) o;
            return this.mId == key.mId
                    && ((this.mServer == key.mServer) || (this.mServer != null && this.mServer.equals(key.mServer)));
        }

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = hash * 31 + Long.valueOf(mId).hashCode();
            hash = hash * 31 + (mServer != null ? mServer.hashCode() : 0);
            return hash;
        }
    }

    private static final class AttributesImpl implements Attributes {
        private final Map<String, ?> attrs;
        private final boolean valid;

        private AttributesImpl(final Map<String, ?> prefsMap) {
            this.attrs = new HashMap<String, Object>(prefsMap);
            this.attrs.remove(PREF_ACCESS_TOKEN);
            this.attrs.remove(PREF_REFRESH_TOKEN);
            this.attrs.remove(PREF_EXPIRE_TIME);

            // determine if the attributes are valid
            final String guid = (String) this.attrs.get(PREF_GUID);
            this.valid = this.attrs.containsKey(PREF_ATTR_LOAD_TIME) && guid != null
                    && guid.equals(this.attrs.get(PREF_ATTR_GUID));
        }

        @Nullable
        @Override
        public String getGuid() {
            return (String) this.attrs.get(PREF_GUID);
        }

        @Override
        public boolean areValid() {
            return this.valid;
        }

        @NonNull
        @Override
        public Date getLoadedTime() {
            final Long time = this.valid ? (Long) this.attrs.get(PREF_ATTR_LOAD_TIME) : null;
            return new Date(time != null ? time : 0);
        }

        @Nullable
        @Override
        public String getEmail() {
            return this.valid ? (String) this.attrs.get(PREF_ATTR_EMAIL) : null;
        }

        @Nullable
        @Override
        public String getFirstName() {
            return this.valid ? (String) this.attrs.get(PREF_ATTR_FIRST_NAME) : null;
        }

        @Nullable
        @Override
        public String getLastName() {
            return this.valid ? (String) this.attrs.get(PREF_ATTR_LAST_NAME) : null;
        }
    }
}
