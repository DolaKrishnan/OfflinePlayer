package com.krishnan.offlineplayer

import android.net.Uri

data class Video(
    val id: Long,
    val title: String,
    val duration : Long,
    val durationStr : String,
    val thumbNail : Uri?
)
