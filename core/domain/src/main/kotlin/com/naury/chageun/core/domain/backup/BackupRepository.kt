package com.naury.chageun.core.domain.backup

data class LocalDataSummary(val vehicles: Int, val records: Int, val photos: Int)

sealed interface ImportPreview {
    /** [current]는 이 기기에서 교체될 데이터다. */
    data class Ready(val incoming: LocalDataSummary, val current: LocalDataSummary) : ImportPreview

    data class UnsupportedVersion(val version: Int) : ImportPreview

    data object Invalid : ImportPreview
}

interface BackupRepository {
    suspend fun summary(): LocalDataSummary

    /**
     * `data.json`과 첨부 이미지를 담은 ZIP을 사용자가 고른 문서인 [destinationUri]에 쓴다.
     * @return 대상에 쓰지 못하면 false.
     */
    suspend fun export(destinationUri: String): Boolean

    suspend fun previewImport(sourceUri: String): ImportPreview

    /**
     * 로컬 데이터 전체를 [sourceUri]의 아카이브로 교체한다. DB 행은 하나의 Transaction으로 바꾸고
     * 첨부 파일은 커밋된 뒤에만 바꾸므로, 실패해도 기존 데이터는 그대로 남는다.
     * @return 아무것도 바뀌지 않았으면 false.
     */
    suspend fun import(sourceUri: String): Boolean

    /** 모든 차량, 기록, 알림 상태, 첨부 파일을 지운다. 사용자 설정은 유지한다. */
    suspend fun deleteAll()
}
