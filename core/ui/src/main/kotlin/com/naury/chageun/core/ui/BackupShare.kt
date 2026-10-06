package com.naury.chageun.core.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.naury.chageun.core.ui.photo.PhotoFiles
import java.io.File
import java.time.LocalDate

/**
 * 다른 기기로 보낼 백업 ZIP. 앱 캐시에 만들고 공유 창(Quick Share·메일·메신저·드라이브 앱)으로 넘긴다.
 * 서버를 거치지 않으므로 비용이 없다. 새 파일을 만들 때 이전 공유 파일은 지운다.
 */
object BackupShare {
    private const val DIRECTORY = "backup_share"
    private const val ZIP_MIME_TYPE = "application/zip"

    fun newFile(context: Context, today: LocalDate = LocalDate.now()): File {
        val directory = File(context.cacheDir, DIRECTORY)
        directory.deleteRecursively()
        directory.mkdirs()
        return File(directory, "chageun-backup-$today.zip")
    }

    fun contentUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, PhotoFiles.authority(context), file)

    /**
     * 받는 앱이 파일을 읽을 수 있게 권한을 함께 넘긴다. 공유 창 자체도 파일 이름·미리보기를 읽으므로
     * ClipData에 넣어 바깥 Intent까지 읽기 권한이 전달되게 한다.
     */
    fun chooser(uri: Uri, title: String): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = ZIP_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, title).apply {
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
