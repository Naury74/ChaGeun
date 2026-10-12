package com.naury.chageun.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 계정 동그라미. 받아 둔 프로필 사진이 있으면 사진을, 없거나 읽지 못하면 이름 첫 글자를,
 * 이름도 없으면 사람 아이콘을 보인다. 읽어 줄 설명은 부르는 쪽이 붙인다.
 */
@Composable
fun ProfileAvatar(name: String?, photoPath: String?, size: Dp, modifier: Modifier = Modifier) {
    val photo = photoPath?.let { rememberFileImage(it).value }
    val circle = modifier.size(size)
    when {
        photo != null -> Image(
            bitmap = photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = circle
                .clip(CircleShape)
                // 밝은 사진이 밝은 배경에, 어두운 사진이 어두운 배경에 묻히지 않게 얇은 테두리를 두른다.
                .border(AVATAR_BORDER, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .testTag(PROFILE_AVATAR_PHOTO_TAG),
        )
        name != null -> Initial(name, size, circle.testTag(PROFILE_AVATAR_INITIAL_TAG))
        else -> Icon(
            Icons.Filled.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = circle,
        )
    }
}

@Composable
private fun Initial(name: String, size: Dp, modifier: Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // 동그라미 크기가 고정이라 첫 글자도 글꼴 크기 설정을 따르지 않게 한다. 읽을 이름은 설명으로 따로 준다.
        val glyphSize = with(LocalDensity.current) { (size * INITIAL_RATIO).toSp() }
        Text(
            name.take(1).uppercase(),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = glyphSize, lineHeight = glyphSize),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.wrapContentSize(unbounded = true),
        )
    }
}

/** 테스트에서 사진과 첫 글자 중 무엇이 보이는지 찾는 태그. */
const val PROFILE_AVATAR_PHOTO_TAG = "profile_avatar_photo"
const val PROFILE_AVATAR_INITIAL_TAG = "profile_avatar_initial"

private val AVATAR_BORDER = 1.dp
private const val INITIAL_RATIO = 14f / 32f
