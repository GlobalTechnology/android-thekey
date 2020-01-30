package me.thekey.android.livedata

import android.os.AsyncTask
import androidx.annotation.AnyThread
import androidx.lifecycle.ComputableLiveData
import me.thekey.android.Attributes
import me.thekey.android.TheKey
import me.thekey.android.exception.TheKeySocketException
import me.thekey.android.getService
import timber.log.Timber

internal class AttributesLiveData(
    private val thekey: TheKey,
    private val guid: String? = null
) : ComputableLiveData<Attributes>() {
    init {
        thekey.getService<LiveDataRegistry>()?.register(this)
    }

    private val currentGuid get() = guid ?: thekey.defaultSessionGuid
    private val loadAttributes = Runnable {
        try {
            thekey.loadAttributes(currentGuid)
        } catch (e: TheKeySocketException) {
            Timber.tag("TheKey")
                .d(e, "error loading fresh attributes for AttributesLiveData")
        }
    }

    override fun compute() = thekey.getCachedAttributes(currentGuid)
        .also { if (!it.areValid() || it.areStale()) AsyncTask.THREAD_POOL_EXECUTOR.execute(loadAttributes) }

    @AnyThread
    internal fun invalidateFor(guid: String) {
        if (guid == currentGuid) invalidate()
    }

    @AnyThread
    internal fun invalidateDefaultGuid() {
        if (guid == null) invalidate()
    }
}
