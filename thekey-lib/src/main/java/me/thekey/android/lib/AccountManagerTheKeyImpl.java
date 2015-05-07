package me.thekey.android.lib;

import static me.thekey.android.lib.Constant.OAUTH_PARAM_ACCESS_TOKEN;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_ATTR_EMAIL;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_ATTR_FIRST_NAME;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_ATTR_LAST_NAME;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_REFRESH_TOKEN;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_THEKEY_GUID;
import static me.thekey.android.lib.Constant.OAUTH_PARAM_THEKEY_USERNAME;
import static me.thekey.android.lib.accounts.Constants.DATA_GUID;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import me.thekey.android.TheKeyInvalidSessionException;
import me.thekey.android.lib.accounts.AccountUtils;

final class AccountManagerTheKeyImpl extends TheKeyImpl {
    private static final String DATA_ATTR_LOAD_TIME = "attr_load_time";
    private static final String DATA_ATTR_EMAIL = "attr_email";
    private static final String DATA_ATTR_FIRST_NAME = "attr_first_name";
    private static final String DATA_ATTR_LAST_NAME = "attr_last_name";

    private static final String AUTH_TOKEN_ACCESS_TOKEN = "access_token";
    private static final String AUTH_TOKEN_REFRESH_TOKEN = "refresh_token";

    @NonNull
    private final AccountManager mAccountManager;

    @NonNull
    private final String mAccountType;

    AccountManagerTheKeyImpl(@NonNull final Context context, @NonNull final Configuration config) {
        super(context, config);
        assert mConfig.mAccountType != null :
                "This object should only be created when there is an account type in the config";
        mAccountManager = AccountManager.get(context);
        mAccountType = mConfig.mAccountType;
    }

    @NonNull
    @Override
    public Collection<String> getSessions() {
        final Collection<String> sessions = new HashSet<>();
        for (final Account account : mAccountManager.getAccountsByType(mAccountType)) {
            final String guid = getGuid(account);
            if (guid != null) {
                sessions.add(guid);
            }
        }
        return sessions;
    }

    @Nullable
    private String getGuid(@Nullable final Account account) {
        return AccountUtils.getGuid(mAccountManager, account);
    }

    @Override
    public boolean isValidSession(@Nullable final String guid) {
        return guid != null && findAccount(guid) != null;
    }

    @Nullable
    private Account findAccount(@NonNull final String guid) {
        final Account account = AccountUtils.getAccount(mAccountManager, mAccountType, guid);

        // reset the default session if this was it
        if (account == null) {
            resetDefaultSession(guid);
        }

        return account;
    }

    @Override
    void clearAuthState(@NonNull final String guid, final boolean sendBroadcast) {
        final Account account = findAccount(guid);
        if (account != null) {
            removeAccount(account, true);
        }
    }

    @NonNull
    @Override
    public Attributes getAttributes(@Nullable final String guid) {
        return new AttributesImpl(this, guid != null ? findAccount(guid) : null);
    }

    @Override
    void storeAttributes(@NonNull final String guid, @NonNull final JSONObject json) {
        final Account account = findAccount(guid);
        if (account != null) {
            mAccountManager.setUserData(account, DATA_ATTR_LOAD_TIME, Long.toString(System.currentTimeMillis()));
            mAccountManager.setUserData(account, DATA_ATTR_EMAIL, json.optString(OAUTH_PARAM_ATTR_EMAIL, null));
            mAccountManager.setUserData(account, DATA_ATTR_FIRST_NAME,
                                        json.optString(OAUTH_PARAM_ATTR_FIRST_NAME, null));
            mAccountManager.setUserData(account, DATA_ATTR_LAST_NAME, json.optString(OAUTH_PARAM_ATTR_LAST_NAME, null));
        }
    }

    @Override
    void removeAttributes(@NonNull final String guid) {
        final Account account = findAccount(guid);
        if (account != null) {
            mAccountManager.setUserData(account, DATA_ATTR_LOAD_TIME, null);
            mAccountManager.setUserData(account, DATA_ATTR_EMAIL, null);
            mAccountManager.setUserData(account, DATA_ATTR_FIRST_NAME, null);
            mAccountManager.setUserData(account, DATA_ATTR_LAST_NAME, null);
        }
    }

    @Override
    boolean storeGrants(@NonNull final String guid, @NonNull final JSONObject json) {
        // short-circuit if this grant is for a different user
        if (!TextUtils.equals(guid, json.optString(OAUTH_PARAM_THEKEY_GUID, null))) {
            return false;
        }

        // determine username
        String username = json.optString(OAUTH_PARAM_THEKEY_USERNAME, null);
        if (TextUtils.isEmpty(username)) {
            username = guid;
        }

        // create account if it doesn't already exist
        Account account = findAccount(guid);
        boolean broadcastLogin = false;
        if (account == null) {
            // create a new account
            account = new Account(username, mAccountType);
            final Bundle data = new Bundle(1);
            data.putString(DATA_GUID, guid);
            removeAccount(account, true);
            mAccountManager.addAccountExplicitly(account, null, data);
            broadcastLogin = true;
        } else if (!username.equals(account.name)) {
            // remove any potentially conflicting account before proceeding
            removeAccount(new Account(username, account.type), true);

            // username is different, rename account
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    // rename account
                    account = mAccountManager.renameAccount(account, username, null, null).getResult();
                } catch (final Exception ignored) {
                    // suppress the error, this shouldn't be fatal
                }
            } else {
                // native rename is not supported, let's rely on migration framework to rename account
                final String defaultGuid = getDefaultSessionGuid();
                final MigratingAccount migratingAccount = getMigratingAccount(guid);
                migratingAccount.attributes = new UsernameWrappedAttributes(username, migratingAccount.attributes);
                removeAccount(account, false);
                createMigratingAccount(migratingAccount);
                if (defaultGuid != null) {
                    try {
                        setDefaultSession(defaultGuid);
                    } catch (final TheKeyInvalidSessionException ignored) {
                    }
                }

                // update account object
                account = findAccount(guid);
                if (account == null) {
                    // this is strange, let's bail and let the user sort it out next time they try something
                    return false;
                }
            }
        }

        // store access_token
        if (json.has(OAUTH_PARAM_ACCESS_TOKEN)) {
            mAccountManager.setAuthToken(account, AUTH_TOKEN_ACCESS_TOKEN,
                                         json.optString(OAUTH_PARAM_ACCESS_TOKEN, null));
            //TODO: store expiration time for access_token?
//                if (json.has(OAUTH_PARAM_EXPIRES_IN)) {
//                    final long expireTime =
//                            System.currentTimeMillis() + json.getLong(OAUTH_PARAM_EXPIRES_IN) * 1000;
//                }
        }

        // store refresh_token
        if (json.has(OAUTH_PARAM_REFRESH_TOKEN)) {
            mAccountManager.setAuthToken(account, AUTH_TOKEN_REFRESH_TOKEN,
                                         json.optString(OAUTH_PARAM_REFRESH_TOKEN, null));
        }

        if (broadcastLogin) {
            BroadcastUtils.broadcastLogin(mContext, guid);
        }

        return true;
    }

    @Nullable
    @Override
    String getAccessToken(@NonNull final String guid) {
        final Account account = findAccount(guid);
        if (account != null) {
            try {
                return mAccountManager.blockingGetAuthToken(account, AUTH_TOKEN_ACCESS_TOKEN, false);
            } catch (final Exception ignored) {
            }
        }
        return null;
    }

    @Override
    void removeAccessToken(@NonNull final String guid, @NonNull final String token) {
        final Account account = findAccount(guid);
        if (account != null) {
            mAccountManager.invalidateAuthToken(mAccountType, token);
        }
    }

    @Nullable
    @Override
    String getRefreshToken(@NonNull final String guid) {
        final Account account = findAccount(guid);
        if (account != null) {
            try {
                return mAccountManager.blockingGetAuthToken(account, AUTH_TOKEN_REFRESH_TOKEN, false);
            } catch (final Exception ignored) {
            }
        }
        return null;
    }

    @Override
    void removeRefreshToken(@NonNull final String guid, @NonNull final String token) {
        final Account account = findAccount(guid);
        if (account != null) {
            mAccountManager.invalidateAuthToken(mAccountType, token);
        }
    }

    @Override
    boolean createMigratingAccount(@NonNull final MigratingAccount account) {
        // remove any existing accounts that conflict with the migrating account
        final Account existing = findAccount(account.guid);
        if (existing != null) {
            removeAccount(existing, false);
        }

        // create account
        final String username = account.attributes.getUsername();
        final Account newAccount = new Account(TextUtils.isEmpty(username) ? account.guid : username, mAccountType);
        final Bundle data = new Bundle();
        data.putString(DATA_GUID, account.guid);
        removeAccount(newAccount, false);
        mAccountManager.addAccountExplicitly(newAccount, null, data);

        // set auth tokens for this account
        mAccountManager.setAuthToken(newAccount, AUTH_TOKEN_ACCESS_TOKEN, account.accessToken);
        mAccountManager.setAuthToken(newAccount, AUTH_TOKEN_REFRESH_TOKEN, account.refreshToken);

        // set all attributes
        mAccountManager.setUserData(newAccount, DATA_ATTR_LOAD_TIME,
                                    Long.toString(account.attributes.getLoadedTime().getTime()));
        mAccountManager.setUserData(newAccount, DATA_ATTR_EMAIL, account.attributes.getEmail());
        mAccountManager.setUserData(newAccount, DATA_ATTR_FIRST_NAME, account.attributes.getFirstName());
        mAccountManager.setUserData(newAccount, DATA_ATTR_LAST_NAME, account.attributes.getLastName());

        // return success
        return true;
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private void removeAccount(@NonNull final Account account, final boolean broadcastLogout) {
        final String guid = getGuid(account);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                mAccountManager.removeAccount(account, null, null).getResult();
            } catch (final Exception ignored) {
            }
        } else {
            mAccountManager.removeAccountExplicitly(account);
        }

        // reset the default session (if this was it)
        resetDefaultSession(guid);

        if (broadcastLogout && guid != null) {
            BroadcastUtils.broadcastLogout(mContext, guid, false);
        }
    }

    private static final class AttributesImpl implements Attributes {
        @Nullable
        private final String mUsername;
        @Nullable
        private final String mGuid;
        private final boolean mValid;
        @NonNull
        private final Date mLoadedTime;
        @Nullable
        private final String mEmail;
        @Nullable
        private final String mFirstName;
        @Nullable
        private final String mLastName;

        AttributesImpl(@NonNull final AccountManagerTheKeyImpl theKey, @Nullable final Account account) {
            mUsername = account != null ? account.name : null;
            mGuid = theKey.getGuid(account);
            mValid = account != null;
            if (mValid) {
                final AccountManager manager = theKey.mAccountManager;
                long loadedTime;
                try {
                    loadedTime = Long.parseLong(manager.getUserData(account, DATA_ATTR_LOAD_TIME));
                } catch (final Exception e) {
                    loadedTime = 0;
                }
                mLoadedTime = new Date(loadedTime);
                mEmail = manager.getUserData(account, DATA_ATTR_EMAIL);
                mFirstName = manager.getUserData(account, DATA_ATTR_FIRST_NAME);
                mLastName = manager.getUserData(account, DATA_ATTR_LAST_NAME);
            } else {
                mLoadedTime = new Date(0);
                mEmail = null;
                mFirstName = null;
                mLastName = null;
            }
        }

        @Nullable
        @Override
        public String getUsername() {
            return mUsername;
        }

        @Nullable
        @Override
        public String getGuid() {
            return mGuid;
        }

        @Override
        public boolean areValid() {
            return mValid;
        }

        @NonNull
        @Override
        public Date getLoadedTime() {
            return mLoadedTime;
        }

        @Nullable
        @Override
        public String getEmail() {
            return mEmail;
        }

        @Nullable
        @Override
        public String getFirstName() {
            return mFirstName;
        }

        @Nullable
        @Override
        public String getLastName() {
            return mLastName;
        }
    }

    private static final class UsernameWrappedAttributes implements Attributes {
        @NonNull
        private final String mUsername;
        @NonNull
        private final Attributes mAttributes;

        UsernameWrappedAttributes(@NonNull final String username, @NonNull final Attributes attributes) {
            mUsername = username;
            mAttributes = attributes;
        }

        @Nullable
        @Override
        public String getUsername() {
            return mUsername;
        }

        @Nullable
        @Override
        public String getGuid() {
            return mAttributes.getGuid();
        }

        @NonNull
        @Override
        public Date getLoadedTime() {
            return mAttributes.getLoadedTime();
        }

        @Override
        public boolean areValid() {
            return mAttributes.areValid();
        }

        @Nullable
        @Override
        public String getEmail() {
            return mAttributes.getEmail();
        }

        @Nullable
        @Override
        public String getFirstName() {
            return mAttributes.getFirstName();
        }

        @Nullable
        @Override
        public String getLastName() {
            return mAttributes.getLastName();
        }
    }
}
