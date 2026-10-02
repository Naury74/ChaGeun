package com.naury.chageun.core.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 앱 전용 이미지 파일을 메인 스레드 밖에서 디코딩한다. 불러오는 중이거나 파일이 없으면 null이다. */
@Composable
fun rememberFileImage(path: String): State<ImageBitmap?> = produceState<ImageBitmap?>(initialValue = null, path) {
    value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
}
