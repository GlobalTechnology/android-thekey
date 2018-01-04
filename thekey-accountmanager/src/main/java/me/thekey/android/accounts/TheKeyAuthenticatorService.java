package me.thekey.android.accounts;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TheKeyAuthenticatorService extends Service {
    private TheKeyAccountAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuthenticator = new TheKeyAccountAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
