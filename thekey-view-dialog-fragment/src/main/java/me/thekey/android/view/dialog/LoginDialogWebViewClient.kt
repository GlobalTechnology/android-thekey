package me.thekey.android.view.dialog

import android.net.Uri
import com.karumi.weak.weak
import me.thekey.android.view.LoginWebViewClient
import me.thekey.android.view.fragment.findListener

internal class LoginDialogWebViewClient(fragment: LoginDialogFragment) :
    LoginWebViewClient(fragment.requireContext(), fragment.arguments) {
    private val fragment: LoginDialogFragment? by weak(fragment)

    override fun onAuthorizeSuccess(uri: Uri, code: String, state: String?) {
        LoginDialogCodeGrantAsyncTask(fragment, mTheKey, uri).execute()
    }

    override fun onAuthorizeError(uri: Uri, errorCode: String) {
        fragment?.apply {
            findListener<LoginDialogFragment.Listener>()?.onLoginFailure(this)

            // close the dialog if it is still active (added to the activity)
            if (isAdded) dismissAllowingStateLoss()
        }
    }
}
