package com.manager.mymanager.models

import android.os.Parcel
import android.os.Parcelable

data class User (
    var id:String="",
    var name:String="",
    var email:String="",
    var image:String="",
    var mobile:Long=0,
    var fcmToken:String="",
    var selected:Boolean=false
    ):Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readString()!!
    ) {
    }

    override fun writeToParcel(dest: Parcel, flags: Int)=with(dest) {
        writeString(id)
        writeString(name)
        writeString(email)
        writeString(image)
        writeLong(mobile)
        writeString(fcmToken)
    }

    override fun describeContents()=0

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}