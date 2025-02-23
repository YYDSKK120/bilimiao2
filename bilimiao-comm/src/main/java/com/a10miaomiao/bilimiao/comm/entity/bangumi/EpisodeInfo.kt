package com.a10miaomiao.bilimiao.comm.entity.bangumi

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class EpisodeInfo(
    val aid: Int,
    val badge: String,
    val badge_type: Int,
    val cid: Int,
    val cover: String,
    val dimension: DimensionXInfo,
    val from: String,
    val id: String,
    val ep_id: String,
    val index: String,
    val index_title: String,
    val long_title: String,
    val mid: Int,
    val page: Int,
    val pub_real_time: String,
    val section_id: String,
    val section_type: Int,
    val share_url: String,
    val status: Int,
    val title: String,
    val vid: String
) : Parcelable
