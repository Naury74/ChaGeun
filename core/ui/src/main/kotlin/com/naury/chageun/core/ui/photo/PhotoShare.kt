package com.naury.chageun.core.ui.photo

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * 다른 앱으로 보낼 사진·동영상 사본. 받는 앱이 언제 파일을 읽을지 알 수 없어 보낸 직후 지우지 않고,
 * 다음에 편집기를 열거나 공유할 때 하루 지난 사본을 정리한다. 앨범 원본 폴더는 다른 앱에 열지 않는다.
 */
internal object PhotoShare {
    private const val DIRECTORY = "photo_share"
    private const val STALE_MILLIS = 24 * 60 * 60 * 1000L

    /** [sourceUri]를 캐시 사본으로 옮기고 다른 앱이 읽을 수 있는 content URI를 돌려준다. 읽지 못하면 null. */
    fun copy(context: Context, sourceUri: Uri, extension: String): Uri? {
        val target = File(directory(context), "${UUID.randomUUID()}.$extension")
        val copied = runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
        }.getOrNull()
        if (copied == null || target.length() == 0L) {
            target.delete()
            return null
        }
        return FileProvider.getUriForFile(context, PhotoFiles.authority(context), target)
    }

    fun cleanStale(context: Context, now: Long = System.currentTimeMillis()) {
        directory(context).listFiles()?.filter { now - it.lastModified() > STALE_MILLIS }?.forEach(File::delete)
    }

    /**
     * 공유 창. 받는 앱과 공유 창 미리보기가 모두 파일을 읽을 수 있게 바깥 Intent에도 ClipData와 읽기 권한을 붙인다.
     */
    fun chooser(uri: Uri, mimeType: String, title: String): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, title).apply {
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun directory(context: Context) = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
}
