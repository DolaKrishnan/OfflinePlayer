package com.krishnan.offlineplayer

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class VideoViewModel : ViewModel() {

    private val _videoList = MutableStateFlow<List<Video>>(emptyList())
    val videoList: StateFlow<List<Video>> = _videoList

    fun fetchLocalVideos(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val videos = loadLocalVideos(context)
            _videoList.value = videos
        }
    }

    private suspend fun loadLocalVideos(context: Context): List<Video> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<Video>()

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                var duration = cursor.getLong(durationColumn)
                val durationTime = formatDuration(duration)

                var thumbnailUri: Uri? = ContentUris.withAppendedId(
                    uri,
                    id
                )
                videoList.add(Video(id, name, duration, durationTime, thumbnailUri))
            }
        }
        return@withContext videoList
    }

    fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds) // hh:mm:ss
        } else {
            String.format("%02d:%02d", minutes, seconds) // mm:ss
        }
    }
}