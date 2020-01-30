package me.thekey.android.livedata

import androidx.lifecycle.LiveData
import me.thekey.android.Attributes
import me.thekey.android.TheKey

fun TheKey.getAttributesLiveData(): LiveData<Attributes> = AttributesLiveData(this, null).liveData
fun TheKey.getAttributesLiveData(guid: String): LiveData<Attributes> = AttributesLiveData(this, guid).liveData
