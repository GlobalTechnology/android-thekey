package me.thekey.android

interface TheKeyService {
    @JvmDefault
    fun init(thekey: TheKey) = thekey.registerService(this)
}
