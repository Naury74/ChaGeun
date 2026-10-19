package com.naury.chageun.feature.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.naury.chageun.core.model.ProfileNameCheck
import com.naury.chageun.core.model.ProfileNameRule
import com.naury.chageun.core.ui.ChageunDialog
import com.naury.chageun.core.ui.DialogAction
import com.naury.chageun.core.ui.DialogActionStyle
import com.naury.kinetica.ui.KTextField

/**
 * 부를 이름 고치기 창. 비우고 저장하거나 기본 이름 버튼을 누르면 정한 이름을 지워 Google 이름이나 이메일 앞부분으로 돌아간다.
 * 저장에 실패하면 입력한 글자는 그대로 두고 이유를 칸 아래에 보여 준다.
 */
@Composable
internal fun ProfileNameDialog(
    state: NameEditorState,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onUseDefaultName: () -> Unit,
    onDismiss: () -> Unit,
) {
    val check = state.check
    val canSave = !state.isSaving && (check is ProfileNameCheck.Valid || check == ProfileNameCheck.Empty)
    val defaultLabel = stringResource(
        if (state.usesGoogleName) R.string.account_name_use_google else R.string.account_name_use_email,
    )
    val saveLabel = when {
        state.isSaving -> stringResource(R.string.account_name_saving)
        check == ProfileNameCheck.Empty -> defaultLabel
        else -> stringResource(R.string.account_name_save)
    }
    ChageunDialog(
        title = stringResource(R.string.account_name_title),
        message = stringResource(R.string.account_name_body),
        icon = Icons.Filled.Badge,
        onDismiss = onDismiss,
        // 저장하는 동안에는 결과를 보여 줘야 하므로 닫지 않는다.
        dismissible = !state.isSaving,
        actions = listOfNotNull(
            DialogAction(saveLabel, enabled = canSave, onClick = onSave),
            // 이미 비워 두었으면 위 버튼이 같은 일을 하므로 따로 두지 않는다.
            DialogAction(
                defaultLabel,
                DialogActionStyle.Secondary,
                enabled = !state.isSaving,
                onClick = onUseDefaultName,
            )
                .takeIf { state.hasCustomName && check != ProfileNameCheck.Empty },
            DialogAction(stringResource(R.string.account_cancel), DialogActionStyle.Secondary, onClick = onDismiss),
        ),
    ) {
        val isInvalid = check == ProfileNameCheck.TooLong || check == ProfileNameCheck.HasLineBreak
        val support = supportingText(state)
        KTextField(
            value = state.draft,
            onValueChange = onDraftChange,
            label = stringResource(R.string.account_name_label),
            supportingText = support.takeUnless { isInvalid },
            error = support.takeIf { isInvalid },
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (canSave) onSave() }),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { AccountErrorAlert(authErrorMessage(it)) }
    }
}

@Composable
private fun supportingText(state: NameEditorState): String {
    val length = ProfileNameRule.length(state.draft)
    return when (state.check) {
        is ProfileNameCheck.Valid -> stringResource(R.string.account_name_count, length, ProfileNameRule.MAX_LENGTH)
        ProfileNameCheck.Empty -> stringResource(
            if (state.usesGoogleName) R.string.account_name_empty_google else R.string.account_name_empty_email,
        )
        ProfileNameCheck.TooLong -> stringResource(R.string.account_name_too_long, ProfileNameRule.MAX_LENGTH, length)
        ProfileNameCheck.HasLineBreak -> stringResource(R.string.account_name_line_break)
    }
}
