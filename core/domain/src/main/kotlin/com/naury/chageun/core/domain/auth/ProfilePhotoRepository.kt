package com.naury.chageun.core.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * 로그인한 계정의 프로필 사진을 기기 안에 한 장만 받아 둔다. 홈 머리말과 계정 화면의 동그라미에 쓴다.
 * 사진 주소가 바뀌면 새로 받고, 로그아웃하면 지운다.
 */
interface ProfilePhotoRepository {
    /** 받아 둔 사진 파일 경로. 로그인하지 않았거나, 사진이 없거나, 받지 못했으면 null이다. */
    val photoPath: Flow<String?>
}
