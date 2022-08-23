package com.manager.mymanager.models

import android.os.Parcel
import android.os.Parcelable

data class Task(
    var title:String,
    var createdBy:String="",
    var cards:ArrayList<Card> = ArrayList()
):Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createTypedArrayList(Card.CREATOR)!!
    )
    constructor():this(
        title="",
        createdBy="",
        cards = ArrayList()
    )


    override fun writeToParcel(dest: Parcel, flags: Int)=with(dest) {
        writeString(title)
        writeString(createdBy)
        writeTypedList(cards)
    }



    override fun describeContents()=0

    companion object CREATOR : Parcelable.Creator<Task> {
        override fun createFromParcel(parcel: Parcel): Task {
            return Task(parcel)
        }

        override fun newArray(size: Int): Array<Task?> {
            return arrayOfNulls(size)
        }
    }

}
