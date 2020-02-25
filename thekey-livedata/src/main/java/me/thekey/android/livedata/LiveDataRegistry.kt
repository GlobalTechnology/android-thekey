package me.thekey.android.livedata

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import me.thekey.android.events.EventsManager
import java.util.WeakHashMap

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LiveDataRegistry : EventsManager {
    private val registry: MutableMap<AttributesLiveData, Unit> = WeakHashMap()
    private val defaultGuidRegistry: MutableMap<DefaultSessionGuidLiveData, Unit> = WeakHashMap()

    internal fun register(liveData: AttributesLiveData) = registry.put(liveData, Unit)
    internal fun register(liveData: DefaultSessionGuidLiveData) = defaultGuidRegistry.put(liveData, Unit)

    @SuppressLint("RestrictedApi")
    override fun changeDefaultSessionEvent(guid: String) {
        registry.keys.forEach { it.invalidateForDefaultGuid() }
        defaultGuidRegistry.keys.forEach { it.invalidate() }
    }

    override fun attributesUpdatedEvent(guid: String) {
        registry.keys.forEach { it.invalidateFor(guid) }
    }

    @SuppressLint("RestrictedApi")
    override fun logoutEvent(guid: String, changingUser: Boolean) {
        registry.keys.forEach {
            it.invalidateFor(guid)
            it.invalidateForDefaultGuid()
        }
        defaultGuidRegistry.keys.forEach { it.invalidate() }
    }
}
