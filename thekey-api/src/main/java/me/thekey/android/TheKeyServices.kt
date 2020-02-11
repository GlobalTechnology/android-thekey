package me.thekey.android

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface TheKeyServices {
    fun registerService(service: TheKeyService, key: String = service.javaClass.name)
    fun <T : TheKeyService> getService(key: String): T?
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T : TheKeyService> TheKeyServices.getService(): T? = getService(T::class.java.name)
