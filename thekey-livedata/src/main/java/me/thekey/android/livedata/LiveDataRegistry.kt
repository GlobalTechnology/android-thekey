package me.thekey.android.livedata

import androidx.annotation.RestrictTo
import me.thekey.android.events.EventsManager
import java.util.WeakHashMap

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LiveDataRegistry : EventsManager {
    private val registry: MutableMap<AttributesLiveData, Unit> = WeakHashMap()

    internal fun register(liveData: AttributesLiveData) = registry.put(liveData, Unit)

    override fun attributesUpdatedEvent(guid: String) {
        registry.keys.forEach { it.invalidateFor(guid) }
    }

    override fun changeDefaultSessionEvent(guid: String) {
        registry.keys.forEach { it.invalidateDefaultGuid() }
    }
}
