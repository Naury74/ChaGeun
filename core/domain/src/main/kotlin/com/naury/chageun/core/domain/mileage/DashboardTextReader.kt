package com.naury.chageun.core.domain.mileage

/** 계기판 사진의 글자를 줄 단위로 읽는다. 사진과 글자는 기기 밖으로 보내지 않는다. */
fun interface DashboardTextReader {
    /**
     * 글자 인식 모델을 처음 쓰면 Google Play 서비스가 내려받는다. 그동안 [onDownloadingModel]로
     * 진행률(0~1, 크기를 모르면 null)을 알린다. 이미 받아 둔 경우에는 부르지 않는다.
     */
    suspend fun read(imageUri: String, onDownloadingModel: (Float?) -> Unit): TextReadResult
}

sealed interface TextReadResult {
    data class Read(val lines: List<String>) : TextReadResult

    /** 모델을 내려받지 못했다. 대개 네트워크 문제라 다시 시도하면 된다. */
    data object ModelUnavailable : TextReadResult

    /** 이 기기에서 인식기를 쓸 수 없다(네이티브 오류, 시간 초과). */
    data object Unavailable : TextReadResult
}
