package me.thekey.android.view.fragment

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.fragment.app.Fragment

@RestrictTo(LIBRARY_GROUP)
inline fun <reified T> Fragment.findAncestorFragment(): T? {
    var candidate = parentFragment
    while (candidate != null) {
        if (candidate is T) return candidate
        candidate = parentFragment
    }
    return null
}

@RestrictTo(LIBRARY_GROUP)
inline fun <reified T> Fragment.findListener(): T? = targetFragment as? T ?: findAncestorFragment() ?: activity as? T
