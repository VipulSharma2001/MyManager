package com.manager.mymanager.models

import android.os.Parcel
import android.os.Parcelable

data class Board(
    var name:String="",
    var image:String="",
    var createdBy:String="",
    var assignedTo:ArrayList<String> = ArrayList(),
    var documentId:String="",
    var taskList:ArrayList<Task> = ArrayList()

):Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createStringArrayList()!!,
        parcel.readString()!!,
        parcel.createTypedArrayList(Task.CREATOR)!!
    )
    override fun writeToParcel(dest: Parcel, flags: Int)=with(dest) {
        writeString(name)
        writeString(image)
        writeString(createdBy)
        writeStringList(assignedTo)
        writeString(documentId)
        writeTypedList(taskList)
    }
    override fun describeContents()=0

    companion object CREATOR : Parcelable.Creator<Board> {
        override fun createFromParcel(parcel: Parcel): Board {
            return Board(parcel)
        }

        override fun newArray(size: Int): Array<Board?> {
            return arrayOfNulls(size)
        }
    }
}

