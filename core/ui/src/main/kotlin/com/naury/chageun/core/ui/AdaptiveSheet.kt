package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** 입력 화면이다. Compact 창에서는 Bottom Sheet로, Sheet가 너무 넓어지는 창에서는 폭을 제한한 Dialog로 띄운다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheet(isExpanded: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    if (isExpanded) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.widthIn(max = 560.dp)) { content() }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) { content() }
    }
}
