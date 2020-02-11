package me.thekey.android.livedata

import androidx.lifecycle.ComputableLiveData
import androidx.lifecycle.distinctUntilChanged
import me.thekey.android.TheKey
import me.thekey.android.TheKeyService

internal class DefaultSessionGuidLiveData(
    private val thekey: TheKey
) : ComputableLiveData<String?>(), TheKeyService {
    init {
        init(thekey)
        LiveDataRegistry.register(this)
    }

    internal val distinctLiveData = liveData.distinctUntilChanged()
    override fun compute() = thekey.defaultSessionGuid
}
