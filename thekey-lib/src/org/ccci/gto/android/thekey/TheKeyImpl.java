package org.ccci.gto.android.thekey;

import static org.ccci.gto.android.thekey.Constant.CAS_SERVER;
import static org.ccci.gto.android.thekey.Constant.OAUTH_GRANT_TYPE_AUTHORIZATION_CODE;
import static org.ccci.gto.android.thekey.Constant.OAUTH_GRANT_TYPE_REFRESH_TOKEN;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_ACCESS_TOKEN;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_CLIENT_ID;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_CODE;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_GRANT_TYPE;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_REDIRECT_URI;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_REFRESH_TOKEN;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_STATE;
import static org.ccci.gto.android.thekey.Constant.OAUTH_PARAM_THEKEY_GUID;
import static org.ccci.gto.android.thekey.Constant.REDIRECT_URI;
import static org.ccci.gto.android.thekey.Constant.THEKEY_PARAM_SERVICE;
import static org.ccci.gto.android.thekey.Constant.THEKEY_PARAM_TICKET;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import me.thekey.android.TheKey;
import me.thekey.android.TheKeySocketException;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Build;
import android.util.Pair;

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

    private final Context context;
    private final Uri casServer;
    private final Long clientId;

    public TheKeyImpl(final Context context, final Long clientId) {
        this(context, clientId, CAS_SERVER);
    }

    public TheKeyImpl(final Context context, final Long clientId, final String casServer) {
        this(context, clientId, Uri.parse(casServer));
    }

    public TheKeyImpl(final Context context, final Long clientId, final Uri casServer) {
        this.context = context;
        this.clientId = clientId;
        this.casServer = casServer;
    }

    protected Uri getCasUri() {
        return this.casServer;
    }

    protected Uri getCasUri(final String... segments) {
        final Builder uri = this.casServer.buildUpon();
        for (final String segment : segments) {
            uri.appendPath(segment);
        }
        return uri.build();
    }

    protected Long getClientId() {
        return this.clientId;
    }

    public String getGuid() {
        return this.getPrefs().getString(PREF_GUID, null);
    }

    protected Uri getAuthorizeUri() {
        return this.getAuthorizeUri(null);
    }

    protected Uri getAuthorizeUri(final String state) {
        // build oauth authorize url
        final Builder uri = this.getCasUri("oauth", "authorize").buildUpon()
                .appendQueryParameter(OAUTH_PARAM_CLIENT_ID, this.getClientId().toString())
                .appendQueryParameter(OAUTH_PARAM_REDIRECT_URI, REDIRECT_URI.toString());
        if (state != null) {
            uri.appendQueryParameter(OAUTH_PARAM_STATE, state);
        }

        return uri.build();
    }

    /**
     * This method returns a ticket for the specified service. This method is a
     * blocking method, this should never be called directly on the UI thread.
     *
     * @param service
     * @return The ticket
     */
    public String getTicket(final String service) throws TheKeySocketException {
        final Pair<String, Attributes> ticketPair = this.getTicketAndAttributes(service);
        return ticketPair != null ? ticketPair.first : null;
    }

    /**
     * This method returns a ticket for the specified service and attributes the ticket was issued for. This is a
     * blocking method, this should never be called directly on the UI thread.
     *
     * @param service
     * @return The ticket & attributes
     */
    public Pair<String, Attributes> getTicketAndAttributes(final String service) throws TheKeySocketException {
        Pair<String, Attributes> credentials = null;
        while ((credentials = this.getValidAccessTokenAndAttributes(0)) != null) {
            // fetch a ticket
            final String ticket = this.getTicket(credentials.first, service);
            if (ticket != null) {
                return Pair.create(ticket, credentials.second);
            }

            // the access token didn't work, remove it and restart processing
            this.removeAccessToken();
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

            // parse the json response
            final JSONObject json = this.parseJsonResponse(conn.getInputStream());
            return json.optString(THEKEY_PARAM_TICKET, null);
        } catch (final MalformedURLException e) {
            throw new RuntimeException("malformed CAS URL", e);
        } catch (final IOException e) {
            throw new TheKeySocketException("connect error", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private Pair<String, Attributes> getAccessTokenAndAttributes() {
        // we use getAll to access the preferences to reduce the chance of a
        // race condition
        final Map<String, ?> attrs = new HashMap<String, Object>(this.getPrefs().getAll());
        final long currentTime = System.currentTimeMillis();
        final long expireTime;
        {
            final Long v = (Long) attrs.get(PREF_EXPIRE_TIME);
            expireTime = v != null ? v : currentTime;
        }
        final String accessToken = expireTime >= currentTime ? (String) attrs.get(PREF_ACCESS_TOKEN) : null;

        // return the access_token, attributes pair if we have an access_token
        return accessToken != null ? Pair.create(accessToken, (Attributes) new AttributesImpl(attrs)) : null;
    }

    private String getRefreshToken() {
        return this.getPrefs().getString(PREF_REFRESH_TOKEN, null);
    }

    private Pair<String, Attributes> getValidAccessTokenAndAttributes(final int depth) throws TheKeySocketException {
        // prevent infinite recursion
        if (depth > 2) {
            return null;
        }

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

        // no valid access_token was found
        return null;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void removeAccessToken() {
        final Editor prefs = this.getPrefs().edit();
        prefs.remove(PREF_ACCESS_TOKEN);
        prefs.remove(PREF_EXPIRE_TIME);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.apply();
        } else {
            prefs.commit();
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

    protected boolean processCodeGrant(final String code, final Uri redirectUri) throws TheKeySocketException {
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
        final Editor prefs = this.getPrefs().edit();

        // store access_token
        if (json.has(OAUTH_PARAM_ACCESS_TOKEN)) {
            try {
                prefs.putString(PREF_ACCESS_TOKEN, json.getString(OAUTH_PARAM_ACCESS_TOKEN));
                prefs.remove(PREF_EXPIRE_TIME);
                if (json.has("expires_in")) {
                    prefs.putLong(PREF_EXPIRE_TIME, System.currentTimeMillis() + json.getLong("expires_in") * 1000);
                }
                prefs.remove(PREF_GUID);
                if (json.has(OAUTH_PARAM_THEKEY_GUID)) {
                    prefs.putString(PREF_GUID, json.getString(OAUTH_PARAM_THEKEY_GUID));
                }
            } catch (final JSONException e) {
                prefs.remove(PREF_ACCESS_TOKEN);
                prefs.remove(PREF_EXPIRE_TIME);
                prefs.remove(PREF_GUID);
            }
        }

        // store refresh_token
        if (json.has(OAUTH_PARAM_REFRESH_TOKEN)) {
            try {
                prefs.putString(PREF_REFRESH_TOKEN, json.getString(OAUTH_PARAM_REFRESH_TOKEN));
            } catch (final JSONException e) {
                prefs.remove(PREF_REFRESH_TOKEN);
            }
        }

        // store updates
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            prefs.apply();
        } else {
            prefs.commit();
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

    private static final class AttributesImpl implements Attributes {
        private final Map<String, ?> attrs;

        private AttributesImpl(final Map<String, ?> prefsMap) {
            this.attrs = new HashMap<String, Object>(prefsMap);
        }

        public String getGuid() {
            return (String) this.attrs.get(PREF_GUID);
        }
    }
}
