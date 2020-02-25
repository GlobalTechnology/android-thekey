package me.thekey.android.livedata

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import me.thekey.android.Attributes
import me.thekey.android.TheKey
import me.thekey.android.getService

val TheKey.defaultSessionGuidLiveData get() = (getService() ?: DefaultSessionGuidLiveData(this)).distinctLiveData

@SuppressLint("RestrictedApi")
fun TheKey.getAttributesLiveData(): LiveData<Attributes> = AttributesLiveData(this, null).liveData

@SuppressLint("RestrictedApi")
fun TheKey.getAttributesLiveData(guid: String): LiveData<Attributes> = AttributesLiveData(this, guid).liveData
