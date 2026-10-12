package com.naury.chageun.data.weather

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/**
 * 내려받은 지역 목록을 앱 안(files/weather/regions)에 둔다. 공개 자료라 개인정보가 아니어서
 * 설정 › 데이터 › 모두 삭제에서도 지우지 않는다.
 *
 * 목록 파일을 먼저 쓰고 안내 파일(regions.json)을 마지막에 바꾼다. 둘 다 임시 파일에 쓴 뒤 이름을 바꾸므로,
 * 중간에 앱이 꺼져도 예전 목록이나 새 목록 중 하나만 보인다. 읽을 때도 sha256을 다시 맞춰 본다.
 */
internal class KoreaRegionListStore(private val dir: File) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, DIRECTORY))

    /** 설치한 목록. [bytes]는 CSV 파일 그대로다. */
    class Installed(val manifest: KoreaRegionManifest, val bytes: ByteArray)

    /** 설치한 목록이 없거나, 안내 파일을 읽을 수 없거나, 목록 파일이 sha256과 맞지 않으면 null이다. */
    fun installed(): Installed? {
        val manifestFile = File(dir, MANIFEST).takeIf(File::isFile) ?: return null
        val manifest = try {
            KoreaRegionManifest.parse(manifestFile.readText())
        } catch (_: IllegalArgumentException) {
            null
        }
        val bytes = manifest?.let { File(dir, it.file) }?.takeIf(File::isFile)?.readBytes() ?: return null
        return Installed(manifest, bytes).takeIf { KoreaRegionListCheck.sha256(bytes) == manifest.sha256 }
    }

    /** 실패하면 [IOException]을 던지고, 예전 목록은 그대로 남는다. */
    fun install(manifest: KoreaRegionManifest, bytes: ByteArray) {
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("지역 목록 폴더를 만들지 못함")
        writeAtomically(File(dir, manifest.file), bytes)
        writeAtomically(File(dir, MANIFEST), manifest.toJson().toByteArray())
        dir.listFiles()
            ?.filter { it.name != manifest.file && (it.name.endsWith(CSV) || it.name.endsWith(TEMP)) }
            ?.forEach(File::delete)
    }

    /** 설치한 목록을 지운다. 앱에 넣은 목록으로 돌아간다. 마지막 확인 시각은 남긴다. */
    fun clear() {
        dir.listFiles()?.filter { it.name != LAST_CHECK }?.forEach(File::delete)
    }

    fun lastCheckedAt(): Instant? =
        File(dir, LAST_CHECK).takeIf(File::isFile)?.readText()?.trim()?.toLongOrNull()?.let(Instant::ofEpochMilli)

    fun markChecked(at: Instant) {
        if (!dir.isDirectory && !dir.mkdirs()) return
        writeAtomically(File(dir, LAST_CHECK), at.toEpochMilli().toString().toByteArray())
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val temp = File(dir, target.name + TEMP)
        FileOutputStream(temp).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IOException("지역 목록 파일을 바꾸지 못함")
        }
    }

    private companion object {
        const val DIRECTORY = "weather/regions"
        const val MANIFEST = "regions.json"
        const val LAST_CHECK = "last_check"
        const val CSV = ".csv"
        const val TEMP = ".tmp"
    }
}
