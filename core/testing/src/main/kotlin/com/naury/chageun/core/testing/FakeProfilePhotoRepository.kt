package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.auth.ProfilePhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** 받아 둔 프로필 사진 경로를 테스트가 바로 정한다. */
class FakeProfilePhotoRepository(path: String? = null) : ProfilePhotoRepository {
    override val photoPath = MutableStateFlow(path)
}
