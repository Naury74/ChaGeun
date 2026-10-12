package com.naury.chageun.data.history

import java.io.File
import java.time.LocalDate
import javax.inject.Inject

/**
 * 앨범에 넣은 동영상. [width]·[height]는 회전을 반영한, 화면에 보이는 방향 기준 크기다.
 * [takenOn]은 원본에 적힌 촬영일이고 저장한 파일에는 남기지 않는다.
 */
internal data class ImportedVideo(
    val file: File,
    val poster: File,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val takenOn: LocalDate? = null,
)

internal sealed interface VideoImport {
    data class Imported(val video: ImportedVideo) : VideoImport

    /** 앨범에 넣기에는 너무 길거나 크다. 파일은 만들지 않는다. */
    data object TooLarge : VideoImport

    /** 읽을 수 없거나 옮길 수 없는 형식이다. 반쯤 쓴 파일은 남기지 않는다. */
    data object Failed : VideoImport
}

/** 실제 동영상을 다루지 않고 Repository를 테스트할 수 있도록 추상화했다. */
internal interface VideoImporter {
    /** [sourceUri]가 동영상이면 true. 사진이나 모르는 형식이면 false여서 사진으로 가져간다. */
    fun isVideo(sourceUri: String): Boolean

    /**
     * [sourceUri]의 영상·소리 트랙만 [target]으로 옮기고, 앞부분 한 장면을 [poster]에 JPEG로 쓴다.
     *
     * @param maxDurationMs 이보다 길면 [VideoImport.TooLarge].
     * @param maxBytes 원본이나 옮긴 파일이 이보다 크면 [VideoImport.TooLarge].
     */
    fun import(sourceUri: String, target: File, poster: File, maxDurationMs: Long, maxBytes: Long): VideoImport
}

/** 앨범이 쓰는 사진·동영상 가져오기. 저장소 생성자가 길어지지 않게 묶었다. */
internal class AlbumImporters @Inject constructor(val image: ImageImporter, val video: VideoImporter)
