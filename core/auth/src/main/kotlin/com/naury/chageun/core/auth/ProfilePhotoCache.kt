package com.naury.chageun.core.auth

import java.io.File
import java.security.MessageDigest
import java.util.Locale

/**
 * 프로필 사진 파일 한 장을 맡는다. 파일 이름에 사진 주소의 해시를 넣는다.
 * 화면의 이미지 캐시가 경로로 찾으므로, 사진이 바뀌면 경로도 바뀌어야 지난 사진이 다시 나오지 않는다.
 * 주소 자체는 기기에 남기지 않는다.
 */
internal class ProfilePhotoCache(private val directory: File) {

    /** [url]의 사진을 이미 받아 뒀으면 그 파일. */
    fun cached(url: String): File? = fileFor(url).takeIf { it.isFile && it.length() > 0 }

    /** [bytes]를 [url]의 사진으로 저장하고 다른 사진은 지운다. 쓰다 멈춰도 반쪽 파일이 사진으로 쓰이지 않게 이름을 나중에 바꾼다. */
    fun store(url: String, bytes: ByteArray): File {
        directory.mkdirs()
        val target = fileFor(url)
        val partial = File(directory, target.name + PARTIAL_SUFFIX)
        partial.writeBytes(bytes)
        if (!partial.renameTo(target)) {
            partial.copyTo(target, overwrite = true)
            partial.delete()
        }
        directory.listFiles()?.filter { it != target }?.forEach(File::delete)
        return target
    }

    /** 로그아웃·계정 삭제·사진 없음일 때 폴더째 지운다. */
    fun clear() {
        directory.deleteRecursively()
    }

    private fun fileFor(url: String) = File(directory, "avatar-${url.hashPrefix()}.jpg")

    private fun String.hashPrefix(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .take(HASH_BYTES)
        .joinToString("") { "%02x".format(Locale.ROOT, it) }

    private companion object {
        const val PARTIAL_SUFFIX = ".part"
        const val HASH_BYTES = 8
    }
}
