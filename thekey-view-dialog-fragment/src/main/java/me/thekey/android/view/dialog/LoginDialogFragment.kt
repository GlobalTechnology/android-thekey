package me.thekey.android.view.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commit
import me.thekey.android.view.Builder
import me.thekey.android.view.LoginWebViewClient
import me.thekey.android.view.fragment.FragmentBuilder
import me.thekey.android.view.util.DisplayUtil
import timber.log.Timber

private const val TAG = "LoginDialogFragment"

open class LoginDialogFragment : DialogFragment() {
    companion object {
        @JvmStatic
        fun builder() = builder(LoginDialogFragment::class.java)

        @JvmStatic
        fun <T : LoginDialogFragment> builder(clazz: Class<T>): Builder<T> = FragmentBuilder(clazz)
    }

    interface Listener {
        fun onLoginSuccess(dialog: LoginDialogFragment, guid: String)

        @JvmDefault
        fun onLoginCanceled(dialog: LoginDialogFragment) = Unit

        @JvmDefault
        fun onLoginFailure(dialog: LoginDialogFragment) = Unit
    }

    // region Lifecycle
    override fun onAttach(context: Context) {
        super.onAttach(context)
        updateLoginWebViewClient()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireActivity())
            .apply {
                // build dialog
                val frame = LayoutInflater.from(context).inflate(R.layout.thekey_login, null) as FrameLayout
                setView(frame.attachLoginView(frame))
            }
            // handle back button presses to navigate back in the WebView if possible
            // TODO: switch this to the new back listener
            .setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK && !event.isCanceled) {
                    return@setOnKeyListener DisplayUtil.navigateBackIfPossible(loginView)
                }
                false
            }
            .create()
    }

    override fun onDestroyView() {
        // HACK: Work around bug
        //       http://code.google.com/p/android/issues/detail?id=17423
        //       https://issuetracker.google.com/issues/36929400
        if (retainInstance) dialog?.setDismissMessage(null)
        super.onDestroyView()
    }
    // endregion Lifecycle

    // region Login WebView
    private var currentFrame: FrameLayout? = null
    private var loginView: WebView? = null
    @UiThread
    private fun FrameLayout.attachLoginView(frame: FrameLayout): FrameLayout {
        detachLoginView()

        // create the LoginWebViewClient if we don't have one yet
        val webViewClient = loginWebViewClient ?: LoginDialogWebViewClient(this@LoginDialogFragment).also {
            loginWebViewClient = it
            updateLoginWebViewClient()
        }

        // create a Login WebView if one doesn't exist already
        loginView = loginView ?: DisplayUtil.createLoginWebView(context, webViewClient, arguments)

        // attach the login view to the current frame
        addView(loginView)
        currentFrame = this

        return this
    }

    @UiThread
    private fun detachLoginView() {
        // remove the login view from any existing frame
        try {
            currentFrame?.removeView(loginView)
        } catch (e: IllegalArgumentException) {
            // XXX: KEYAND-12 IllegalArgumentException: Receiver not registered: android.webkit.WebViewClassic
            Timber.tag(TAG)
                .e(e, "error removing Login WebView, let's just reset the login view")
            loginView = null
            loginWebViewClient = null
        }

        currentFrame = null
    }

    // region LoginWebViewClient
    private var loginWebViewClient: LoginWebViewClient? = null
    private fun updateLoginWebViewClient() = loginWebViewClient?.setActivity(activity)
    // endregion LoginWebViewClient
    // endregion Login WebView

    override fun dismissAllowingStateLoss() {
        try {
            super.dismissAllowingStateLoss()
        } catch (suppressed: IllegalStateException) {
            // HACK: work around state loss exception if the dialog was added to the back stack
            Timber.tag(TAG).e(suppressed, "Error dismissing the LoginDialogFragment, probably because of a Back Stack.")
            fragmentManager?.commit(allowStateLoss = true) { remove(this@LoginDialogFragment) }
        }
    }
}
