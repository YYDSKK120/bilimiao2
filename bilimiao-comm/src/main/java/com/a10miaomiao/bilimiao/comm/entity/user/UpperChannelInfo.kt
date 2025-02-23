package com.a10miaomiao.bilimiao.comm.entity.user

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UpperChannelInfo(
    var cid : Int,
    var mid : Int,
    var name : String,
    var intro : String,
    var mtime : Long,
    var count : Int,
    var cover : String,
//        var archives : List<UpperArchives>,
    var isAll: Boolean = false
): Parcelable