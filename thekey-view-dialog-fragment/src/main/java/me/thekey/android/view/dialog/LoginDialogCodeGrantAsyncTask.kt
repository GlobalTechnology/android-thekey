package me.thekey.android.view.dialog

import android.net.Uri
import com.karumi.weak.weak
import me.thekey.android.TheKey
import me.thekey.android.core.CodeGrantAsyncTask
import me.thekey.android.view.fragment.findListener

internal class LoginDialogCodeGrantAsyncTask(dialog: LoginDialogFragment?, thekey: TheKey, dataUri: Uri) :
    CodeGrantAsyncTask(thekey, dataUri) {
    private val dialog: LoginDialogFragment? by weak(dialog)

    override fun onPostExecute(guid: String?) {
        super.onPostExecute(guid)
        dialog?.run {
            findListener<LoginDialogFragment.Listener>()?.let {
                when {
                    guid != null -> it.onLoginSuccess(this, guid)
                    else -> it.onLoginFailure(this)
                }
            }

            // close the dialog if it is still active
            if (isAdded) dismissAllowingStateLoss()
        }
    }
}
