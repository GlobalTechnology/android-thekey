package me.thekey.android.lib.accounts;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;

import static me.thekey.android.lib.accounts.Constants.DATA_GUID;

public class AccountUtils {
    @Nullable
    public static String getGuid(@NonNull final Context context, @Nullable final Account account) {
        return getGuid(AccountManager.get(context), account);
    }

    @Nullable
    public static String getGuid(@NonNull final AccountManager manager, @Nullable final Account account) {
        return account != null ? manager.getUserData(account, DATA_GUID) : null;
    }

    @Nullable
    public static Account getAccount(@NonNull final Context context, @NonNull final String accountType,
                                     @Nullable final String guid) {
        return getAccount(AccountManager.get(context), accountType, guid);
    }

    @Nullable
    @SuppressWarnings("MissingPermission")
    @RequiresPermission(value = Manifest.permission.GET_ACCOUNTS, conditional = true)
    public static Account getAccount(@NonNull final AccountManager manager, @NonNull final String accountType,
                                     @Nullable final String guid) {
        // short-circuit if we don't have an account
        if (guid == null) {
            return null;
        }

        // search for the matching account
        final Account[] accounts = manager.getAccountsByType(accountType);
        for (final Account account : accounts) {
            if (guid.equals(getGuid(manager, account))) {
                return account;
            }
        }

        return null;
    }
}
