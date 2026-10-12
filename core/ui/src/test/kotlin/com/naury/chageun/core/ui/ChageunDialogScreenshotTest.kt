package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.Gutters
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 공용 대화상자와 입력 칸의 모양. 폰은 아래 시트, 넓은 창은 가운데 카드로 뜬다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChageunDialogScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun SignOutDialog() {
        ChageunDialog(
            title = "로그아웃할까요?",
            message = "기록은 이 휴대폰에 그대로 남아요. 다시 로그인할 때까지 백업은 멈춰요.",
            icon = Icons.AutoMirrored.Filled.Logout,
            onDismiss = {},
            actions = listOf(
                DialogAction("로그아웃") {},
                DialogAction("취소", DialogActionStyle.Secondary) {},
            ),
        )
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun confirm_phone() {
        composeRule.setContent { AppFrame { SignOutDialog() } }
        composeRule.captureScreen("dialog_confirm_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    fun confirm_phoneDark() {
        composeRule.setContent { AppFrame { SignOutDialog() } }
        composeRule.captureScreen("dialog_confirm_phone_dark")
    }

    @Test
    @Config(qualifiers = "ko-w840dp-h900dp-hdpi")
    fun confirm_tablet() {
        composeRule.setContent { AppFrame(Gutters.Expanded) { SignOutDialog() } }
        composeRule.captureScreen("dialog_confirm_tablet")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun destructive_phone() {
        composeRule.setContent {
            AppFrame {
                ChageunDialog(
                    title = "모든 데이터를 지울까요?",
                    message = "차량, 정비 기록, 사진이 이 휴대폰에서 모두 지워져요. 되돌릴 수 없어요.",
                    icon = Icons.Filled.DeleteForever,
                    tone = ChageunTheme.colors.critical,
                    onDismiss = {},
                    actions = listOf(
                        DialogAction("모두 지우기", DialogActionStyle.Destructive) {},
                        DialogAction("취소", DialogActionStyle.Secondary) {},
                    ),
                )
            }
        }
        composeRule.captureScreen("dialog_destructive_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun withTextField_phone() {
        composeRule.setContent {
            AppFrame {
                ChageunDialog(
                    title = "비밀번호 재설정",
                    message = "가입한 이메일로 재설정 링크를 보내 드려요.",
                    onDismiss = {},
                    actions = listOf(
                        DialogAction("보내기") {},
                        DialogAction("취소", DialogActionStyle.Secondary) {},
                    ),
                ) {
                    ChageunTextField(
                        value = "driver@example",
                        onValueChange = {},
                        label = "이메일",
                        isError = true,
                        supportingText = "이메일 주소를 확인해 주세요",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        composeRule.captureScreen("dialog_text_field_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun options_phone() {
        composeRule.setContent {
            AppFrame {
                ChageunOptionDialog(
                    title = "무엇을 기록할까요?",
                    options = listOf(
                        DialogOption("정비·교체", {}, Icons.Filled.Build, ChageunTheme.colors.good),
                        DialogOption("주유·충전", {}, Icons.Filled.LocalGasStation, ChageunTheme.colors.upcoming),
                        DialogOption("사진 찍기", {}, Icons.Filled.PhotoCamera),
                        DialogOption("앨범에서 고르기", {}, Icons.Filled.PhotoLibrary),
                    ),
                    cancelLabel = "취소",
                    onDismiss = {},
                )
            }
        }
        composeRule.captureScreen("dialog_options_phone")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun datePicker_phone() {
        composeRule.setContent {
            AppFrame {
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = PICKED.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                )
                ChageunDatePickerDialog(state = state, onConfirm = {}, onDismiss = {})
            }
        }
        composeRule.captureScreen("dialog_date_picker_phone")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    fun datePicker_phoneDark() {
        composeRule.setContent {
            AppFrame {
                val state = rememberDatePickerState(
                    initialSelectedDateMillis = PICKED.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                )
                ChageunDatePickerDialog(state = state, onConfirm = {}, onDismiss = {})
            }
        }
        composeRule.captureScreen("dialog_date_picker_phone_dark")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun textFields_phone() {
        composeRule.setContent { AppFrame { TextFieldStates() } }
        composeRule.captureScreen("text_field_states_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    fun textFields_phoneDark() {
        composeRule.setContent { AppFrame { TextFieldStates() } }
        composeRule.captureScreen("text_field_states_phone_dark")
    }

    @Composable
    private fun TextFieldStates() {
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChageunTextField(value = "", onValueChange = {
            }, label = "정비소", singleLine = true, modifier = Modifier.fillMaxWidth())
            ChageunTextField(
                value = "45000",
                onValueChange = {},
                label = "주행거리",
                suffix = "km",
                singleLine = true,
                visualTransformation = GroupedNumberTransformation,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            ChageunTextField(
                value = "12가345",
                onValueChange = {},
                label = "새 번호판",
                isError = true,
                supportingText = "번호판 형식을 확인해 주세요",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ChageunTextField(
                value = "",
                onValueChange = {},
                placeholder = "항목, 정비소, 메모 검색",
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ChageunTextField(
                value = "엔진오일과 필터를 같이 바꿨어요.",
                onValueChange = {},
                label = "메모",
                supportingText = "정비소에서 받은 영수증 내용을 적어 두면 좋아요",
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    private companion object {
        val PICKED: LocalDate = LocalDate.of(2024, 3, 14)
    }
}
