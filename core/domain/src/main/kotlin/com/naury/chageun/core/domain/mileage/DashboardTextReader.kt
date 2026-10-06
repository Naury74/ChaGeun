package com.naury.chageun.core.domain.mileage

/** 계기판 사진의 글자를 줄 단위로 읽는다. 사진과 글자는 기기 밖으로 보내지 않는다. */
fun interface DashboardTextReader {
    /** @return 읽은 줄. 인식기를 쓸 수 없거나 실패하면 null. */
    suspend fun read(imageUri: String): List<String>?
}
