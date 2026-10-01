package com.naury.chageun.core.common.storage

import javax.inject.Qualifier

/** App-private directory holding attachment images, shared by history (writes) and backup (export, delete). */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AttachmentDirectory
