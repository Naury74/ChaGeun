package com.naury.chageun.core.common.storage

import javax.inject.Qualifier

/** 첨부 이미지를 보관하는 앱 전용 디렉터리다. 기록(쓰기)과 백업(내보내기, 삭제)이 함께 쓴다. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AttachmentDirectory
