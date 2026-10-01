package com.naury.chageun.core.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes an app-private image file off the main thread; null while loading or if the file is gone. */
@Composable
fun rememberFileImage(path: String): State<ImageBitmap?> = produceState<ImageBitmap?>(initialValue = null, path) {
    value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
}
