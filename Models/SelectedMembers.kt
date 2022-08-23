package com.manager.mymanager.models

import android.os.Parcel
import android.os.Parcelable

data class SelectedMembers(
    var id: String = "",
    var image: String = ""
) : Parcelable {
    constructor(source: Parcel) : this(
        source.readString()!!,
        source.readString()!!
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(id)
        writeString(image)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SelectedMembers> =
            object : Parcelable.Creator<SelectedMembers> {
                override fun createFromParcel(source: Parcel): SelectedMembers =
                    SelectedMembers(source)

                override fun newArray(size: Int): Array<SelectedMembers?> = arrayOfNulls(size)
            }
    }
}